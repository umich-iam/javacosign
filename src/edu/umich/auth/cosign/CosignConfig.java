package edu.umich.auth.cosign;

import java.io.IOException;
import java.util.Properties;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author htchan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class CosignConfig {
	
	private static final String COSIGN_SERVER = "COSIGN_SERVER_";
	private static final String COSIGN_PORT = "COSIGN_PORT_";
	private static final String COSIGN_POOL_CONFIG = "COSIGN_POOL_CONFIG_";

	public static final CosignConfig INSTANCE =
		new CosignConfig();

	private Properties props = new Properties();

	/**
	 * Constructor for Config.
	 */
	private CosignConfig() {
		super();
		try {
			System.out.println("Reading cosignConfig.properties!");
			props.load(CosignConfig.class.getResourceAsStream("cosignConfig.properties"));
		}
		catch (IOException ioe) {
			System.out.println("Fail to load cosignConfig.properties!");
			ioe.printStackTrace();
		}
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
	
	public String getProperty(String key) {
		return props.getProperty(key);
	}

	public CosignServer[] getCosignServers() {
		ArrayList cosignServers = new ArrayList();
		int i = 1;
		do {
			System.out.println(COSIGN_SERVER + i);
			String hostname = props.getProperty(COSIGN_SERVER + i);
			String port = props.getProperty(COSIGN_PORT + i);
			String config = props.getProperty(COSIGN_POOL_CONFIG + i);
			if (null == hostname || null == port) {
				throw new RuntimeException(
					"Error in cosign server configuration"
					+ " in cosignConfig.properties!");
			}
			cosignServers.add(new CosignServer(hostname, port, config));
			i++;
		}
		while (null != props.getProperty(COSIGN_SERVER + i));
		return (CosignServer[]) cosignServers.toArray(
			new CosignServer[cosignServers.size()]);
	}
				
}
