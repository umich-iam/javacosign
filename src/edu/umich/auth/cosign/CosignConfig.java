package edu.umich.auth.cosign;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.Arrays;

import edu.umich.auth.cosign.pool.CosignSSLSocketFactory;

/**
 * This class is the in-memory configurations of the Cosign filter.
 * It reads in the Cosign configuration file and monitor any change
 * in it.
 * 
 * @author htchan
 */
public class CosignConfig implements Runnable {
	
	//  Singleton for this class
	public static final CosignConfig INSTANCE = new CosignConfig();
	
	// Configuration properties constants
	public static final String COSIGN_DOMAIN = "COSIGN_DOMAIN";
	public static final String COSIGN_PORT = "COSIGN_PORT";
	public static final String COSIGN_POOL_CONFIG = "COSIGN_POOL_CONFIG";
	public static final String COSIGN_POOL_LOCKED_SLEEP_TIME = "COSIGN_POOL_LOCKED_SLEEP_TIME";
	public static final String COSIGN_POOL_MONITORING_INTERVAL = "COSIGN_POOL_MONITORING_INTERVAL";
	public static final String CONFIG_FILE_MONITORING_INTERVAL = "CONFIG_FILE_MONITORING_INTERVAL";
	public static final String CONFIG_FILE_PATH = "CONFIG_FILE_PATH";
	
    // Configuration file name
	private static final String configFileName = "cosignConfig.properties";
	private File configFile;
	
	private boolean firstTimeReadProperties = true;
	
	// Temporarily store the properties read from the config file
	private Properties props = new Properties();
	
	// Available consign servers
	private CosignServer[] servers;
	
	// Last update time of the config file
	private long lastUpdate;
	
	// How often we monitor the config file change
	private long configFileMonitoringInterval;
	
	/**
	 * Constructor for Config. Read the config file
	 */
	private CosignConfig() {
		super();
		readProperties();
		firstTimeReadProperties = false;
	}
	
	/**
	  * This method starts a new thread to monitor the configuration
	  * file change.
	  */
	public void run() {
		while (true) {
			try {
				Thread.sleep(configFileMonitoringInterval);
				if (configFile.exists() && configFile.lastModified() > lastUpdate) {
					System.out.println(configFileName + " got updated!");
					lastUpdate = configFile.lastModified();
					readProperties();
					CosignSSLSocketFactory.INSTANCE.reInitialize();
				}
			} catch (InterruptedException ie) {
				System.out.println("Problem in run() in CosignConfig!");
			}
		}
	}

	/**
	 * This method get all the available Cosign servers through
	 * a DNS lookup.
	 * @return	An array of <code>CosignServer</code>.
	 */
	public CosignServer[] getCosignServers() {
		String domainName = props.getProperty(COSIGN_DOMAIN);
		String port = props.getProperty(COSIGN_PORT);
		String config = props.getProperty(COSIGN_POOL_CONFIG);
		if (null == domainName || null == port) {
			throw new RuntimeException("Error in cosign server configuration in cosignConfig.properties!");
		}
		// Performs a DNS lookup.
		InetAddress[] addresses;
		try {
			addresses = InetAddress.getAllByName(domainName);
			servers = new CosignServer[addresses.length];
			for (int i = 0; i < addresses.length; i++) {
				servers[i] = new CosignServer(addresses[i].getHostAddress(), port, config);
			}
		} catch (UnknownHostException e) {
			if (null == servers)
				throw new RuntimeException("Cannot resolve cosign server domain name " + domainName);
		}
		return servers;
	}
	
	/**
	 * This method reads all the properties in the Cosign
	 * configuration file.
	 */
	private void readProperties() {
		try {
			if (firstTimeReadProperties) {
				System.out.println("First time reading properties!");
				firstTimeReadProperties = false;
				props.load(CosignConfig.class.getResourceAsStream(configFileName));
			}
			else {
				System.out.println("Reading properties!");
				props.load(new FileInputStream(configFile));
			}
			configFileMonitoringInterval = getLongProperty(CONFIG_FILE_MONITORING_INTERVAL);
			configFile = new File(getProperty(CONFIG_FILE_PATH));
			lastUpdate = configFile.lastModified();
		} catch (IOException ioe) {
			throw new RuntimeException("Fail to load " + configFileName + "!");
		}
		catch (Exception e) {
			throw new RuntimeException("Fail to create file handle to " + getProperty(CONFIG_FILE_PATH));
		}
	}
	
	/**
	 * This method returns a property in <code>String</code>.
	 * @param key	The key of the property
	 * @return		The <code>String</code> value of the property.
	 */
	public String getProperty(String key) {
		return props.getProperty(key);
	}

	/**
	 * This method returns a property in <code>long</code>.
	 * @param key	The key of the property
	 * @return		The <code>long</code> value of the property.
	 */
	public long getLongProperty(String key) {
		return Long.parseLong(props.getProperty(key));
	}
	
	/**
	 * This method returns a string with all key/value pairs
	 * of the Cosign configurations.
	 */
	public String toString() {
		Object[] keys = props.keySet().toArray();
		Arrays.sort(keys);
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < keys.length; i++) {
			sb.append(keys[i] + " = " + props.get(keys[i]) + "\n");
		}
		return sb.toString();
	}
}