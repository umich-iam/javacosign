package edu.umich.auth.cosign.pool;

import java.util.TreeMap;

import edu.umich.auth.cosign.CosignConfig;
import edu.umich.auth.cosign.CosignServer;

import org.apache.commons.pool.impl.*;

/**
 * @author htchan
 * 
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates. To enable and disable the creation of
 * type comments go to Window>Preferences>Java>Code Generation.
 * 
 * @uml.stereotype name="tagged" isDefined="true"
 */

public class ConnectionManager
{
  private static final boolean DEBUG2OUT = true;

  public static final ConnectionManager INSTANCE = new ConnectionManager();

  /**
   * 
   * @uml.property name="strategyMap" @uml.associationEnd multiplicity="(0 1)"
   * qualifier="cookie:java.lang.String
   * ccs:edu.umich.auth.cosign.pool.CosignConnectionStrategy"
   */
  TreeMap strategyMap = new TreeMap();

  /**
   * 
   * @uml.property name="poolMap"
   */
  TreeMap poolMap = new TreeMap();

  /**
   * Constructor for ConnectionManager.
   */
  private ConnectionManager()
  {
    super();
    CosignServer[] cosignServers = CosignConfig.INSTANCE.getCosignServers();
    for ( int i = 0; i < cosignServers.length; i++ )
    {
      addCosignConnectionPool( cosignServers[i] );
    }
  }

  public CosignConnection getConnection( String cookie )
  {
    CosignConnectionStrategy ccs;

    if ( strategyMap.containsKey( cookie ) )
    {
      ccs = (CosignConnectionStrategy) strategyMap.get( cookie.toString() );
    }
    else
    {
      ccs = new DefaultCosignConnectionStrategy( poolMap );
      strategyMap.put( cookie, ccs );
    }
    
    return ccs.getConnection( poolMap );
  }

  public void cleanUpStrategy( String cookie )
  {
    strategyMap.remove( cookie );
  }

  public void complete( String cookie )
  {
    if ( null == cookie )
    {
      return;
    }
    if ( strategyMap.containsKey( cookie ) )
    {
      ( (CosignConnectionStrategy) strategyMap.get( cookie ) ).reset();
    }
    else
    {
      throw new IllegalStateException( "Strategy not found in the StrategyMap for cookie = '" + cookie + "'" );
    }
  }

  public void addCosignConnectionPool( CosignServer cosignServer )
  {
    String address = cosignServer.getAddress();
    int port = cosignServer.getPort();
    GenericObjectPool.Config config = cosignServer.getConfig();
    
    CosignConnectionFactory ccf = new CosignConnectionFactory( address, port );
    GenericObjectPoolFactory gopf;
    if ( null != config )
    {
      if ( DEBUG2OUT )
        System.out.println( "Using custom config for " + cosignServer + " ..." );
      gopf = new GenericObjectPoolFactory( ccf, config );
    }
    else
    {
      if ( DEBUG2OUT )
        System.out.println( "Using default config for " + cosignServer + " ..." );

      gopf = new GenericObjectPoolFactory( ccf );
    }
    String poolId = address + ":" + port;
    poolMap.put( poolId, new CosignConnectionPool( (GenericObjectPool) gopf.createPool(), cosignServer ) );
    
  }

  public void removeConnectionPool( String poolId )
  {
    //(GenericObjectPool) poolMap.get(poolId);
  }

  public void activateConnectionPool( String poolId )
  {
  }

  protected void finalize() throws Throwable
  {
    try
    {
      strategyMap.clear();
      poolMap.clear();
      strategyMap = null;
      poolMap = null;
    }
    finally
    {
      super.finalize();
    }
  }
}