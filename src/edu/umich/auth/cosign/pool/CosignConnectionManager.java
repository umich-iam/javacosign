package edu.umich.auth.cosign.pool;

import java.util.TreeMap;

import edu.umich.auth.cosign.CosignConfig;


/**
 * @author htchan
 * 
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates. To enable and disable the creation of
 * type comments go to Window>Preferences>Java>Code Generation.
 * 
 * @uml.stereotype name="tagged" isDefined="true"
 */

public class CosignConnectionManager
{
  private static final boolean DEBUG2OUT = true;

  public static final CosignConnectionManager INSTANCE = new CosignConnectionManager();

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
  private CosignConnectionManager()
  {
    super();
    Thread t1 = new Thread(CosignConfig.INSTANCE);
    t1.start();
    Thread t2 = new Thread(CosignConnectionPoolManager.INSTANCE);
    t2.start();
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
      ccs = new CosignConnectionStrategy();
      strategyMap.put( cookie, ccs );
    }
    
    return ccs.getConnection();
  }
  
  public void returnConnection( CosignConnection cc ) {
  	cc.returnToPool();
  }
  
  public void cleanUpStrategy( String cookie ) {
  	strategyMap.remove( cookie );
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