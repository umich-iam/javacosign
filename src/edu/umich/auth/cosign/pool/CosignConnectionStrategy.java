package edu.umich.auth.cosign.pool;

import java.util.TreeMap;

/**
 * @author htchan *  * To change this generated comment edit the template variable "typecomment": * Window>Preferences>Java>Templates. * To enable and disable the creation of type comments go to * Window>Preferences>Java>Code Generation.
 * 
 * @uml.stereotype name="tagged" isDefined="true" 
 * @uml.stereotype name="interface" 
 */

public interface CosignConnectionStrategy {
	public CosignConnection getConnection(TreeMap poolMap);
	public void reset();
}
