package edu.umich.auth.cosign;

import org.apache.commons.pool.impl.*;;

/**
 * @author htchan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class CosignServer {

	private String hostname;
	private int port;
	private GenericObjectPool.Config config = new GenericObjectPool.Config();

	/**
	 * Constructor for CosignServer.
	 */
	public CosignServer(String hostname,
		String port, String configString) {
		super();
		this.hostname = hostname;
		this.port = Integer.parseInt(port);
		this.config = parseConfigString(configString);
		System.out.println("Registering cosign server = "
			+ hostname + ":" + port + ":" + configString);
	}

	private GenericObjectPool.Config parseConfigString(String configString) {
		if (null == configString) {
			this.config.maxActive = 20;
			this.config.maxIdle = -1;
			this.config.maxWait = -1l;
			this.config.testOnBorrow = true;
			this.config.testOnReturn = false;
			this.config.testWhileIdle = true;
			this.config.minEvictableIdleTimeMillis = 1800000l;  // 30 minutes
			this.config.numTestsPerEvictionRun = 3;
			this.config.timeBetweenEvictionRunsMillis = 600000l; // 10 minutes
			this.config.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_GROW;
		}
		else {
			// do something with the configString
		}
		return this.config;
	}

	/**
	 * Returns the config.
	 * @return CosignConnectionPool.Config
	 */
	public GenericObjectPool.Config getConfig() {
		return config;
	}

	/**
	 * Returns the hostname.
	 * @return String
	 */
	public String getHostname() {
		return hostname;
	}

	/**
	 * Returns the port.
	 * @return int
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Sets the config.
	 * @param config The config to set
	 */
	public void setConfig(GenericObjectPool.Config config) {
		this.config = config;
	}

	/**
	 * Sets the hostname.
	 * @param hostname The hostname to set
	 */
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	/**
	 * Sets the port.
	 * @param port The port to set
	 */
	public void setPort(int port) {
		this.port = port;
	}
	
	public String toString() {
		return hostname + ":" + port;
	}

}
