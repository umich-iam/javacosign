package edu.umich.auth.cosign.pool;

import javax.net.ssl.*;
import java.net.Socket;
import java.util.*;
import java.io.*;

/**
 * @author htchan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class CosignConnection {

	private static final boolean DEBUG2OUT = false;
	private static final boolean DEBUG_DETAIL = DEBUG2OUT && false;
	private static Map countMap = new HashMap();
	private static final Random rnd = new Random();

	public static final int COOKIE_VALID = 2;	
	public static final int USER_LOGGED_OUT = 4;
	public static final int SERVER_NOT_READY = 5;


	private CosignConnectionPool thePool;
	private String cosignConId;
	private String poolId;
	private long usageCount = 0;
	private String hostname;
	private int port;
	private BufferedReader in;
	private PrintWriter out;
	private SSLSocket ss;
	private BufferedReader sin;
	private PrintWriter sout;

	/**
	 * Constructor for CosignConnection.
	 */
	public CosignConnection(String hostname, int port) throws IOException {
		super();	
		this.hostname = hostname;
		this.port = port;
		this.poolId = hostname + ":" + port;
		this.cosignConId = this.poolId + ":" + incrementCount();
		init();
	}
	
	private int incrementCount() {
		CosignConnectionCounter counter = (CosignConnectionCounter) countMap.get(this.poolId);
		if (null == counter) {
			counter = new CosignConnectionCounter();
			countMap.put(this.poolId, counter);
		}
		return counter.increment();
	}
	
	private int decrementCount() {
		CosignConnectionCounter counter = (CosignConnectionCounter) countMap.get(this.poolId);
		if (null == counter) {
			throw new RuntimeException("Shouldn't get here. Must have created connections before! um...");
		}
		return counter.decrement();
	}
	
	private void init() throws IOException {
		try{
			// Non-SSL stuff
			Socket s = new Socket(hostname, port);
				
			this.in = new BufferedReader(new InputStreamReader(s.getInputStream()));
			this.out = new PrintWriter(s.getOutputStream());
				
			if (DEBUG2OUT) {
				System.out.println("Initializing " + cosignConId + " ...");
				System.out.println("Reading banner!");
				System.out.println("Result: " + in.readLine());
			}
			else {
				in.readLine();
			}
					
			out.println("STARTTLS");
			out.flush();
				
			if (DEBUG2OUT) {
				System.out.println("Result STARTTLS: " + in.readLine());
			}
			else {
				in.readLine();
			}
			
			// SSL stuff
			SSLSocket ss = CosignSSLSocketFactory.INSTANCE.createSSLSocket(s, hostname, port, true);

			if (DEBUG_DETAIL) {
				String[] cipherSuites = ss.getEnabledCipherSuites();
				for (int i = 0; i < cipherSuites.length; i++) {
					System.out.println("Enabled Ciper Suite: " + cipherSuites[i]);
				}
			}
			ss.startHandshake();
			sin = new BufferedReader(new InputStreamReader(ss.getInputStream()));
			sout = new PrintWriter(ss.getOutputStream());
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
			throw new IOException("Failed in CosignConnection init()!");
		}
	}
	
	
	/**
	 * Returns the cosignConName.
	 * @return String
	 */
	public String getCosignConId() {
		return cosignConId;
	}
	
	public long getUsageCount() {
		return usageCount;
	}

	public String checkCookie(String serviceName, String cookie) {
		usageCount++;
		try {
			if (DEBUG2OUT) System.out.println("CHECK " + serviceName + "=" + cookie);
			this.sout.println("CHECK " + serviceName + "=" + cookie);
			this.sout.flush();
			String result = sin.readLine();
			if (DEBUG2OUT) System.out.println("Result CHECK[" + this.cosignConId + "]: " + result);
			return result;
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
			return null;
		}
	}
	
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
//			try {
//				long sleepTime = Math.round((rnd.nextFloat()*20));
//				if (DEBUG2OUT) System.out.println("Sleep " + sleepTime + " ms!");
//				Thread.sleep(sleepTime);
//			}
//			catch (InterruptedException ie) {
//				ie.printStackTrace();
//			}
			return true;
		}
		catch (IOException ioe) {
			return false;
		}
	}
	
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
//			try {
//				long sleepTime = Math.round((rnd.nextFloat()*20));
//				if (DEBUG2OUT) System.out.println("Sleep " + sleepTime + " ms!");
//				Thread.sleep(sleepTime);
//			}
//			catch (InterruptedException ie) {
//				ie.printStackTrace();
//			}
			return true;
		}
		catch (IOException ioe) {
			return false;
		}
	}
		
	public void setThePool(CosignConnectionPool thePool) {
		this.thePool = thePool;
	}
	
	public void returnToPool() {
		try {
			thePool.returnObject(this);
		}
		catch (Exception e) {
			if (DEBUG2OUT) System.out.println("Fail to return " + this.cosignConId + " to the pool!");
		}
	}
	
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
