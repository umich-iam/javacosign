package edu.umich.auth.cosign;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.Arrays;

import edu.umich.auth.cosign.pool.CosignConnectionPoolManager;
/**
 * @author htchan
 * 
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates. To enable and disable the creation of
 * type comments go to Window>Preferences>Java>Code Generation.
 */
public class CosignConfig implements Runnable {
	
	public static final CosignConfig INSTANCE = new CosignConfig();
	public static final String COSIGN_DOMAIN = "COSIGN_DOMAIN";
	public static final String COSIGN_PORT = "COSIGN_PORT";
	public static final String COSIGN_POOL_CONFIG = "COSIGN_POOL_CONFIG";
	public static final String COSIGN_POOL_MONITORING_INTERVAL = "COSIGN_POOL_MONITORING_INTERVAL";
	public static final String CONFIG_FILE_MONITORING_INTERVAL = "CONFIG_FILE_MONITORING_INTERVAL";
	private static final String configFileName = "cosignConfig.properties";
	private File configFile = new File(configFileName);
	private Properties props = new Properties();
	private CosignServer[] servers;
	private long lastUpdate;
	
	private long configFileMonitoringInterval;
	/**
	 * Constructor for Config.
	 */
	private CosignConfig() {
		super();
		readProperties();
	}
	
	public void run() {
		while (true) {
			try {
				Thread.sleep(configFileMonitoringInterval);
				if (configFile.exists() && configFile.lastModified() > lastUpdate) {
					System.out.println("cosignConfig.properties got updated!");
					lastUpdate = configFile.lastModified();
					readProperties();
				}
			} catch (InterruptedException ie) {
				System.out.println("Problem in run() in CosignConfig!");
			}
		}
	}

	
	public CosignServer[] getCosignServers() {
		String domainName = props.getProperty(COSIGN_DOMAIN);
		String port = props.getProperty(COSIGN_PORT);
		String config = props.getProperty(COSIGN_POOL_CONFIG);
		if (null == domainName || null == port) {
			throw new RuntimeException(
					"Error in cosign server configuration in cosignConfig.properties!");
		}
		// DNS lookup.
		InetAddress[] addresses;
		try {
			addresses = InetAddress.getAllByName(domainName);
			servers = new CosignServer[addresses.length];
			for (int i = 0; i < addresses.length; i++) {
				servers[i] = new CosignServer(addresses[i].getHostAddress(),
						port, config);
			}
		} catch (UnknownHostException e) {
			if (null == servers)
				throw new RuntimeException(
						"Cannot resolve cosign server domain name "
								+ domainName);
		}
		return servers;
	}
	
	private void readProperties() {
		try {
			System.out.println("Reading " + configFileName);
			props.load(CosignConfig.class.getResourceAsStream(configFileName));
			configFileMonitoringInterval = getLongProperty(CONFIG_FILE_MONITORING_INTERVAL);
		} catch (IOException ioe) {
			throw new RuntimeException("Fail to load " + configFileName + "!");
		}		
	}
	
	public String getProperty(String key) {
		return props.getProperty(key);
	}
	
	public long getLongProperty(String key) {
		return Long.parseLong(props.getProperty(key));
	}
	
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