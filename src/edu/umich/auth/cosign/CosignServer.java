package edu.umich.auth.cosign;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.pool.impl.*;

/**
 * This class represents a Cosign server and its connection pool config.
 * 
 * @author dillaman
 */
public class CosignServer {

	private final String host;
  private final String[] hostAddrs;
  
	private final int port;

	/**
	 * @uml.property name="config"
	 * @uml.associationEnd multiplicity="(1 1)"
	 */
	private GenericObjectPool.Config config = new GenericObjectPool.Config();

  private Log log = LogFactory.getLog( CosignServer.class );

  /**
	 * Constructor for CosignServer.
	 */
	public CosignServer() throws UnknownHostException {
    host = (String)CosignConfig.INSTANCE.getPropertyValue(CosignConfig.COSIGN_SERVER_HOST);
    port = ((Integer)CosignConfig.INSTANCE.getPropertyValue(CosignConfig.COSIGN_SERVER_PORT)).intValue();
    hostAddrs = initHostAddresses();
    config = initObjectPoolConfig();
	}

  /**
   * This method get all the available Cosign server IP addrs through
   * a DNS lookup.
   * @return An array of <code>String</code> IP addresses.
   */
  public String[] getHostAddresses() {
    return hostAddrs;
  }
  
  /**
   * Returns the host name.
   * @return  String  The DNS hostname of the cosign server.
   */
  public String getHost () {
    return host;
  }
  
  /**
   * Returns the port.
   * 
   * @return int
   * 
   * @uml.property name="port"
   */
  public int getPort() {
    return port;
  }

	/**
	 * Returns the config.
	 * 
	 * @return CosignConnectionPool.Config
	 * 
	 * @uml.property name="config"
	 */
	public GenericObjectPool.Config getConfig() {
		return config;
	}

	/**
	 * Returns the Cosign server ID.
	 * 
	 * @return The Cosign server ID.
	 */
	public String getCosignServerId() {
		return host + ":" + port;
	}

  /**
   * Returns the unique id of this server
   */
	public String toString() {
		return getCosignServerId();
	}
  
  /**
   * 
   * @return
   * @throws UnknownHostException
   */
  private String[] initHostAddresses () throws UnknownHostException {
    // Performs a DNS lookup.
    InetAddress[] addresses = InetAddress.getAllByName(host);
    String[] hostAddrs = new String[addresses.length];
    for (int i = 0; i < addresses.length; i++) {
      hostAddrs[i] = addresses[i].getHostAddress();
    }
    return hostAddrs;
  }

  /**
   * This method configures the connection pool of this Cosign server.
   * @param configString  Should be <code>null</code> for release 1.0
   * @return  GenericObjectPool.Config
   */
  private GenericObjectPool.Config initObjectPoolConfig() {
    GenericObjectPool.Config config = new GenericObjectPool.Config();
    config.maxActive = ((Integer)CosignConfig.INSTANCE.getPropertyValue(CosignConfig.CONNECTION_POOL_SIZE)).intValue();
    config.maxIdle = -1;
    config.maxWait = -1l;
    config.testOnBorrow = true;
    config.testOnReturn = false;
    config.testWhileIdle = true;
    config.minEvictableIdleTimeMillis = 1800000l; // 30 minutes
    config.numTestsPerEvictionRun = 3;
    config.timeBetweenEvictionRunsMillis = 600000l; // 10 minutes
    config.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_GROW;
    return config;
  }

}