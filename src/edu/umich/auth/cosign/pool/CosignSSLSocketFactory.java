package edu.umich.auth.cosign.pool;

import javax.net.ssl.*;
import com.sun.net.ssl.*;
import java.net.Socket;
import java.security.*;
import java.io.*;

import edu.umich.auth.cosign.CosignConfig;

/**
 * @author htchan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class CosignSSLSocketFactory {

	public static final CosignSSLSocketFactory INSTANCE 
		= new CosignSSLSocketFactory();
		
	private SSLSocketFactory sslSocketFactory;

	/**
	 * Constructor for CosignSSLSocketFactory.
	 */
	private CosignSSLSocketFactory() {
		super();
		init();
	}

	private void init() {
		try {
			Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
			SSLContext ctx = SSLContext.getInstance("TLS");
			KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
			TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
			KeyStore ks = KeyStore.getInstance("JKS");
			String keyStorePath = CosignConfig.INSTANCE.getProperty("KEYSTORE_PATH");
			String keyStorePwd = CosignConfig.INSTANCE.getProperty("KEYSTORE_PASSWORD");
			ks.load(new FileInputStream(keyStorePath), keyStorePwd.toCharArray());
			kmf.init(ks, keyStorePwd.toCharArray());
			tmf.init(ks);
			ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
			this.sslSocketFactory = (SSLSocketFactory) ctx.getSocketFactory();
		}
		catch (IOException ioe) {
			System.out.println("Failed to locate keystore file!");
			ioe.printStackTrace();
		}
		catch (Exception e) {
			e.printStackTrace();
			System.out.println("Failed to create CosignSSLSccketFactory!");
			System.out.println("Algorithm and/or certificate problems!");
		}
	}

	public void reInitialize() {
		init();
	}
	
	public SSLSocket createSSLSocket(Socket s, 
									 String hostname,
									 int port, 
									 boolean autoClose) throws IOException {
		System.out.println("SSLSocketFactory = " + sslSocketFactory);
		return (SSLSocket) sslSocketFactory.createSocket(s,hostname, port, autoClose);
	}

}
