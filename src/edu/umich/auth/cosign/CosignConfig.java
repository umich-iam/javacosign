package edu.umich.auth.cosign;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.Arrays;

/**
 * @author htchan
 * 
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates. To enable and disable the creation of
 * type comments go to Window>Preferences>Java>Code Generation.
 */
public class CosignConfig
{
  private static final String COSIGN_DOMAIN = "COSIGN_DOMAIN";
  private static final String COSIGN_PORT = "COSIGN_PORT";
  private static final String COSIGN_POOL_CONFIG = "COSIGN_POOL_CONFIG";

  public static final CosignConfig INSTANCE = new CosignConfig();

  private Properties props = new Properties();
  private CosignServer[] servers;

  /**
   * Constructor for Config.
   */
  private CosignConfig()
  {
    super();
    try
    {
      System.out.println( "Reading cosignConfig.properties!" );
      props.load( CosignConfig.class.getResourceAsStream( "cosignConfig.properties" ) );
    }
    catch ( IOException ioe )
    {
      System.out.println( "Fail to load cosignConfig.properties!" );
      ioe.printStackTrace();
    }
  }

  public String toString()
  {
    Object[] keys = props.keySet().toArray();
    Arrays.sort( keys );
    StringBuffer sb = new StringBuffer();
    for ( int i = 0; i < keys.length; i++ )
    {
      sb.append( keys[i] + " = " + props.get( keys[i] ) + "\n" );
    }
    return sb.toString();
  }

  public String getProperty( String key )
  {
    return props.getProperty( key );
  }

  public CosignServer[] getCosignServers()
  {
 
    String domainName = props.getProperty( COSIGN_DOMAIN );
    String port = props.getProperty( COSIGN_PORT );
    String config = props.getProperty( COSIGN_POOL_CONFIG );
    
    if ( null == domainName || null == port )
    {
      throw new RuntimeException( "Error in cosign server configuration in cosignConfig.properties!" );
    }
    
    // DNS lookup.
    InetAddress[] addresses;
    
    try
    {
      addresses = InetAddress.getAllByName(domainName);
      servers = new CosignServer[ addresses.length ];
      
      for (int i = 0; i < addresses.length; i++) {
        servers[ i ] = new CosignServer( addresses[i].getHostAddress(), port, config );
      }
    }
    catch ( UnknownHostException e )
    {
      if (null == servers)
        throw new RuntimeException( "Cannot resolve cosign server domain name " + domainName );
    }
    
    return servers;
  }
}