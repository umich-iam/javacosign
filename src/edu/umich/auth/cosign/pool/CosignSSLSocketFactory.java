package edu.umich.auth.cosign.pool;

import javax.net.ssl.*;
import com.sun.net.ssl.*;
import java.net.Socket;
import java.security.*;
import java.io.*;

import edu.umich.auth.cosign.CosignConfig;

/**
 * This singleton class creates SSL Sockets for Cosign connections. It
 * reads the keystore and truststore path info from the
 * <code>CosignConfig</code> class to create the SSlSocketFactory
 * for all the future secure Cosign connections.
 * 
 * @author htchan
 * @see edu.umich.auth.cosign.CosignConfig
 */
public class CosignSSLSocketFactory {

	public static final CosignSSLSocketFactory INSTANCE = new CosignSSLSocketFactory();

	/**
	 * The SSL socket factory to create secure Cosign connection
	 * @uml.property name="sslSocketFactory"
	 * @uml.associationEnd multiplicity="(1 1)"
	 */
	private SSLSocketFactory sslSocketFactory;

	/**
	 * Constructor for CosignSSLSocketFactory.
	 */
	private CosignSSLSocketFactory() {
		super();
		init();
	}

	/**
	 * This methods do a bunch of SSL initialization.  It adds the SSL 
	 * provider (For Java 1.3 backward compatibility). It creates the
	 * SSLContext with the keystore/truststore specified by
	 * the CosignConfig class.
	 */
	private void init() {
		try {
			// Adds this provider for Java 1.3 backward compatibility
			Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
			
			// Creates SSL Context
			SSLContext ctx = SSLContext.getInstance("TLS");
			
			// Creates KeyManager Factory
			KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
			
			// Creates TrustManager Factory
			TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
			
			// Creates a keystore instance
			KeyStore ks = KeyStore.getInstance("JKS");
			
			// Gets the location the keystore from CosignConfig
			String keyStorePath = CosignConfig.INSTANCE.getProperty("KEYSTORE_PATH");
			
			// Gets the password of the keystore from ConsignConfig
			String keyStorePwd = CosignConfig.INSTANCE.getProperty("KEYSTORE_PASSWORD");
			
			// Loads the keystore into memory
			ks.load(new FileInputStream(keyStorePath), keyStorePwd.toCharArray());
			
			// Initializes KeyManager Factory
			kmf.init(ks, keyStorePwd.toCharArray());
			
			// Initializes TrustManager Factory
			tmf.init(ks);
			
			// Initializes the SSL Context with KeyManager and TrustManager Factory
			ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
			
			// Get a socket factory from the context
			this.sslSocketFactory = (SSLSocketFactory) ctx.getSocketFactory();
			
		} catch (IOException ioe) {
			System.out.println("Failed to locate keystore file!");
			ioe.printStackTrace();
		} catch (Exception e) {
			System.out.println("Failed to create CosignSSLSccketFactory!");
			e.printStackTrace();
		}
	}

	/**
	 * This method reInitializes the SSL Socket Factory if 
	 * the CosignConfig file has been modified.
	 */
	public synchronized void reInitialize() {
		System.out.println("Re-initialize the CosignSSLSocketFactory!");
		init();
	}

	/**
	 * This method create SSL socket for Cosign connection.
	 * @param s				Non-SSL socket to be converted
	 * @param hostname		HostName of the Cosign Server
	 * @param port			Port number of the Cosign Server
	 * @param autoClose		close the underlying socket when this socket is closed 
	 * @return				SSL Socket for Cosign connection
	 * @throws IOException
	 */
	public SSLSocket createSSLSocket(Socket s, String hostname, int port,
			boolean autoClose) throws IOException {
		return (SSLSocket) sslSocketFactory.createSocket(s, hostname, port,
				autoClose);
	}

}