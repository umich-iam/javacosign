package edu.umich.auth.cosign.pool;

import javax.net.ssl.*;
import java.net.Socket;
import java.util.*;
import java.io.*;

/**
 * This class represents a Cosign Connection.  It creates a socket
 * connection to the cosign server, converts to an SSL connection
 * and then starts a TLS handshake.  It uses to check whether a 
 * cosign service cookie is valid or not.  Once the check is over,
 * it will return the connection to the connection pool.
 * 
 * @author htchan * 
 * @uml.stereotype name="tagged" isDefined="true" 
 */

public class CosignConnection {

	/**
	 * Debug flag for standard out
	 */
	private static final boolean DEBUG2OUT = true;
	
	/**
	 * Super detail debug
	 */
	private static final boolean DEBUG_DETAIL = DEBUG2OUT && false;
	
	/**
	 * This captures how many connections exist in each connection pool
	 */ 
	private static Map countMap = new HashMap();

	/**
	 * This is the connection pool this connection belongs to.
	 * @uml.property name="thePool"
	 * @uml.associationEnd multiplicity="(0 1)" inverse="thePool:edu.umich.auth.cosign.pool.CosignConnectionPool"
	 */
	private CosignConnectionPool thePool;

	/**
	 * This is the ID for this consign connection.
	 */
	private String cosignConId;
	
	/**
	 * This connection pool ID.
	 */
	private String poolId;
	
	/**
	 * The usage count of the connection through its lifetime.
	 */
	private long usageCount = 0;
	
	/**
	 * The hostname of the cosign server
	 */
	private String hostname;
	
	/**
	 * The port number of the cosign server, default to 6663
	 */
	private int port;
	
	/**
	 * Used to receive response from the cosign server in Non-SSL mode
	 */
	private BufferedReader in;
	
	/**
	 * Used to send query to the cosign server in Non-SSL mode
	 */
	private PrintWriter out;

	/**
	 * This is the converted SSL connection
	 * @uml.property name="ss"
	 * @uml.associationEnd multiplicity="(0 1)"
	 */
	private SSLSocket ss;

	/**
	 * Used to receive response from the cosign server in SSL mode
	 */
	private BufferedReader sin;
	
	/**
	 * Used to send query to the cosign server in SSL mode
	 */
	private PrintWriter sout;

	/**
	 * Constructor for CosignConnection.
	 * @param hostname 	Cosign Server Hostname 
	 * @param port		Cosign Server port
	 */
	public CosignConnection(String hostname, int port) throws IOException {
		super();	
		this.hostname = hostname;
		this.port = port;
		this.poolId = hostname + ":" + port;
		this.cosignConId = this.poolId + ":" + incrementCount();
		init();
	}
	
	/**
	 * This method increments the connections count of the connection pool
	 * this connection belongs to.
	 * @return the current number of connections in the connection pool
	 */
	private int incrementCount() {
		CosignConnectionCounter counter = (CosignConnectionCounter) countMap.get(this.poolId);
		if (null == counter) {
			counter = new CosignConnectionCounter();
			countMap.put(this.poolId, counter);
		}
		return counter.increment();
	}

	/**
	 * This method decrements the connections count of connection pool
	 * this connection belongs to.
	 * @return the current number of connections in the connection pool
	 */
	private int decrementCount() {
		CosignConnectionCounter counter = (CosignConnectionCounter) countMap.get(this.poolId);
		if (null == counter) {
			throw new RuntimeException("Shouldn't get here. Must have created connections before! um...");
		}
		return counter.decrement();
	}
	
	/**
	 * This method creates a socket to the cosign server, reads
	 * the banner, converts the non-SSL socket to an SSL one and
	 * starts an SSL handshake and then properly set the input and
	 * out streams.
	 * @throws IOException	If any socket/SSL exceptions occurs
	 */
	private void init() throws IOException {
		try{
			// Creates non-SSL socket
			Socket s = new Socket(hostname, port);
			
			// Gets the input/output reader/writer
			this.in = new BufferedReader(new InputStreamReader(s.getInputStream()));
			this.out = new PrintWriter(s.getOutputStream());
			
			// This reads welcome message from the cosign server
			if (DEBUG2OUT) {
				System.out.println("Initializing " + cosignConId + " ...");
				System.out.println("Reading banner!");
				System.out.println("Result: " + in.readLine());
			}
			else {
				in.readLine();
			}
			
			// Notifies the cosign to begin SSL communication
			out.println("STARTTLS");
			out.flush();
			
			// Get cosign server's response
			if (DEBUG2OUT) {
				System.out.println("Result STARTTLS: " + in.readLine());
			}
			else {
				in.readLine();
			}
			
			// Convert existing socket to an SSL one
			SSLSocket ss = CosignSSLSocketFactory.INSTANCE.createSSLSocket(s, hostname, port, true);

			// Shows all the cipher suites
			if (DEBUG_DETAIL) {
				String[] cipherSuites = ss.getEnabledCipherSuites();
				for (int i = 0; i < cipherSuites.length; i++) {
					System.out.println("Enabled Ciper Suite: " + cipherSuites[i]);
				}
			}
			
			// Start the SSL handshake
			ss.startHandshake();
			
			// Set the SSL input/output reader/writer
			sin = new BufferedReader(new InputStreamReader(ss.getInputStream()));
			sout = new PrintWriter(ss.getOutputStream());
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
			throw new IOException("Failed in CosignConnection init()!");
		}
	}
	
	
	/**
	 * This methods returns the cosign connection ID
	 * @return String The cosign connection ID
	 */
	public String getCosignConId() {
		return cosignConId;
	}
	
	/**
	 * This method returns the usage count of this connection
	 * @return The usage count of this connection
	 */
	public long getUsageCount() {
		return usageCount;
	}

	/**
	 * This method checks the cosign service cookie against the
	 * cosign server.  The cosign server will return a message
	 * in a specified format which will be handled in another class.
	 * @param serviceName 	The cosign service name e.g. cosign-wolverineaccess
	 * @param cookie		The base64 cosign service cookie
	 * @return				The response from the cosign server.  Returns null
	 * 						if there is an IOException.
	 */
	public String checkCookie(String serviceName, String cookie) {
		usageCount++;
		try {
			if (DEBUG2OUT) System.out.println("CHECK " + serviceName + "=" + cookie);
			
			// Send the cookie to the cosign server 
			this.sout.println("CHECK " + serviceName + "=" + cookie);
			this.sout.flush();
			
			// Gets the result from the cosign server
			String result = sin.readLine();
			
			if (DEBUG2OUT) System.out.println("Result CHECK[" + this.cosignConId + "]: " + result);
			
			return result;
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
			return null;
		}
	}
	
	/**
	 * This methods check whether the secure Cosign connection is 
	 * still alive by issuing a NOOP request to the Cosign server.
	 * @return	True if the socket/server is alive. False otherwise.
	 */
	public boolean isSecureValid() {
		usageCount++;
		try {
			this.sout.println("NOOP");
			this.sout.flush();
			String result = sin.readLine();
			if (DEBUG2OUT) System.out.println("Result NOOP[" + this.cosignConId + "]: " + result);
			if (null == result) {
				return false;
			}
			return true;
		}
		catch (IOException ioe) {
			return false;
		}
	}
	
	/**
	 * This methods check whether the Cosign connection is still 
	 * alive by issuing a NOOP request to the Cosign server.  This
	 * method is only used for development/testing.
	 * @return	True if the socket/server is alive. False otherwise.
	 */
	public boolean isValid() {
		usageCount++;
		try {
			this.out.println("NOOP");
			this.out.flush();
			String result = in.readLine();
			if (DEBUG2OUT) System.out.println("Result NOOP[" + this.cosignConId + "]: " + result);
			if (null == result) {
				return false;
			}
			return true;
		}
		catch (IOException ioe) {
			return false;
		}
	}

	/**
	 * This method sets the Cosign connection pool this Cosign
	 * connection belongs.
	 * @uml.property name="thePool"
	 */
	public void setThePool(CosignConnectionPool thePool) {
		this.thePool = thePool;
	}

	/**
	 * This method returns this connection back to the connection
	 * pool it belongs.
	 */
	public void returnToPool() {
		try {
			thePool.returnObject(this);
		}
		catch (Exception e) {
			if (DEBUG2OUT) System.out.println("Fail to return " + this.cosignConId + " to the pool!");
		}
	}
	
	/**
	 * This method hard closes the Cosign connections.
	 */
	public void close() {
		System.out.println("Hard closing cosign connection = " + cosignConId);
		decrementCount();
		try {
			if (null != this.sout) {
				this.sout.close();
			}
			if (null != this.in) {
				this.sin.close();
			}
			if (null != this.out) {
				this.out.close();
			}
			if (null != this.in) {
				this.in.close();
			}
			if (null != this.ss) {
				this.ss.close();
			}
		}
		catch (IOException ioe) {
			if (DEBUG2OUT) System.out.println("Failed to close CosignConnection!");
		}
		sout = null;
		sin = null;
		out = null;
		in = null;
		ss = null;
	}
}
