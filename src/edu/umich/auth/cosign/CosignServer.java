package edu.umich.auth.cosign;

import org.apache.commons.pool.impl.*;
;

/**
 * @author htchan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class CosignServer {

	private String address;
	private int port;

  /**
   * 
   * @uml.property name="config"
   * @uml.associationEnd multiplicity="(1 1)"
   */
  private GenericObjectPool.Config config = new GenericObjectPool.Config();


	/**
	 * Constructor for CosignServer.
	 */
	public CosignServer(String address,
		String port, String configString) {
		super();
		this.address = address;
		this.port = Integer.parseInt(port);
		this.config = parseConfigString(configString);
		//System.out.println("Constructing cosign server = " + address + ":" + port + ":" + config);
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
   * 
   * @uml.property name="config"
   */
  public GenericObjectPool.Config getConfig() {
    return config;
  }

  /**
   * Returns the hostname.
   * @return String
   * 
   * @uml.property name="hostname"
   */
  public String getAddress() {
    return address;
  }

  /**
   * Returns the port.
   * @return int
   * 
   * @uml.property name="port"
   */
  public int getPort() {
    return port;
  }

  public String getCosignServerId() {
  	return address + ":" + port;
  }

	
  public String toString() {
	return getCosignServerId();
  }

}
