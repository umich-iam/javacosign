package edu.umich.auth.cosign.pool;

import java.util.TreeMap;

/**
 * @author htchan
 * 
 * @uml.stereotype name="tagged" isDefined="true" 
 * @uml.stereotype name="interface" 
 */

public interface CosignConnectionStrategy {
	public CosignConnection getConnection(TreeMap poolMap);
	public void reset();
}