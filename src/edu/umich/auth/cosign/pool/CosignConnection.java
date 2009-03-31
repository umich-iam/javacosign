package edu.umich.auth.cosign.pool;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.net.ssl.*;
import javax.security.auth.*;
import javax.security.auth.kerberos.*;

import com.sun.security.auth.callback.*;
import com.sun.security.auth.module.*;
import edu.umich.auth.cosign.*;
import org.apache.commons.logging.*;
//import sun.security.krb5.*;
import edu.umich.auth.cosign.util.ProxyCookie;


/**
 * This class represents a Cosign Connection.  It creates a socket
 * connection to the cosign server, converts to an SSL connection
 * and then starts a TLS handshake.  It uses to check whether a
 * cosign service cookie is valid or not.  Once the check is over,
 * it will return the connection to the connection pool.
 *
 * @author dillaman
 * @author patkm
 * @uml.stereotype name="tagged" isDefined="true"
 */
public class CosignConnection {

    //Credentials tgt;
    // Codes received from the Cosign server.
    public static final int COSIGN_CODE_UNKNOWN = -1;
    public static final int COSIGN_USER_AUTHENTICATED = 2;
    public static final int COSIGN_USER_NOT_AUTHENTICATED = 4;
    public static final int COSIGN_SERVER_RETRY = 5;
    //private PrincipalName principal = null;
    //private KerberosPrincipal principal = null;
    private static final int COSIGN_CODE_START = COSIGN_USER_AUTHENTICATED;
    private static final int COSIGN_CODE_STOP = COSIGN_SERVER_RETRY;

    /**
     * The unique pool Id and ip addr
     */
    private final String cosignConId;


    /**
     * The host addr of the cosign server
     */
    private final String hostAddr;

    /**
     * The port number of the cosign server, default to 6663
     */
    private final int port;

    /**
     * Used to receive response from the cosign server in Non-SSL mode
     */
    private BufferedReader in;

    /**
     * Used to send query to the cosign server in Non-SSL mode
     */
    private PrintWriter out;

    /**
     * This is the converted SSL connection
     * @uml.property name="ss"
     * @uml.associationEnd multiplicity="(0 1)"
     */
    private SSLSocket ss;

    /**
     * Used to receive response from the cosign server in SSL mode
     */
    private BufferedReader sin;

    /**
     * Used to send query to the cosign server in SSL mode
     */
    private PrintWriter sout;


    /**
     * Used to keep protocol version
     */
    private float protVersion;

    /**
     * Used for logging info and error messages
     */
    private Log log = LogFactory.getLog(CosignConnection.class);

    /**
     * Constructor for CosignConnection.
     * @param hostname   Cosign Server Hostname
     * @param port    Cosign Server port
     */
    public CosignConnection(String cosignConListId, String hostAddr, int port) throws
            IOException {
        this.cosignConId = cosignConListId + ":" + hostAddr + ":" + port;
        this.hostAddr = hostAddr;
        this.port = port;

        init();
    }

    /**
     * This methods returns the cosign connection ID
     * @return String The cosign connection ID
     */
    public String getCosignConId() {
        return cosignConId;
    }

    /**
     * This method retuns the connection protocol version
     * @return float
     */
    public float getProtVersion() {
        return protVersion;
    }

    /**
     * This method returns the host IP address
     * @return String The host IP address
     */
    public String getHostAddress() {
        return hostAddr;
    }

    /**
     * This method converts a Cosign response string into an int code.
     * @param cosignResponse String The response received from checkCookie
     * @return int The code for the given cosignResponse.  Returns
     *    COSIGN_CODE_DEFAULT if cosignResponse is null
     */
    public static int convertResponseToCode(String cosignResponse) {
        if (cosignResponse == null) {
            return COSIGN_CODE_UNKNOWN;
        }
        try {
            int cosignCode = Integer.parseInt(cosignResponse.substring(0, 1));
            if ((cosignCode >= COSIGN_CODE_START) &&
                (cosignCode <= COSIGN_CODE_STOP)) {
                return cosignCode;
            }
        } catch (Exception e) {
        }
        return COSIGN_CODE_UNKNOWN;
    }

    /**
     * This method checks the cosign service cookie against the
     * cosign server.  The cosign server will return a message
     * in a specified format which will be handled in another class.
     * @param serviceName   The cosign service name e.g. cosign-wolverineaccess
     * @param cookie    The base64 cosign service cookie
     * @return        The response from the cosign server.  Returns null
     *             if there is an IOException.
     */
    public String checkCookie(String serviceName, String cookie) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("[" + cosignConId + "] CHECK " + serviceName + "=" +
                          cookie);
            }

            // Send the cookie to the cosign server
            this.sout.println("CHECK " + serviceName + "=" + cookie);

            //this.sout.println("CHECK " +  cookie);
            this.sout.flush();

            // Gets the result from the cosign server
            String cosignResponse = sin.readLine();

            if (log.isDebugEnabled()) {
                log.debug("[" + cosignConId + "] result CHECK: " +
                          cosignResponse);
            }
            return cosignResponse;

        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("[" + cosignConId +
                          "] failed while validating cookie with cosign server",
                          e);
            }
            return null;
        }
    }


    /**
     * This method retreives a kerberos tgt based on a cosign service cookie.
     * It will then place the kerb5 credetials and principle into the current
     * Subject to be used in GSSAPI calls.
     * @param serviceName   The cosign service name e.g. cosign-wolverineaccess
     * @param cookie    The base64 cosign service cookie
     * @param subject   The current threads jaas subject
     * @param sPrinciple The priciple just receieved from the server in a check cookie
     * @return        cosign error codes
     *
     */
    public String retrieveTGT(String serviceName, String cookie,
                              Subject subject, CosignPrincipal sPrinciple) {
        KerberosPrincipal kerbPrinciple = null;
        try {

            // Check if a tgt/kerb5priciple already exists.
            Iterator iterator = subject.getPrincipals().iterator();
            while (iterator.hasNext()) {
                Object object = iterator.next();
                if (object instanceof KerberosPrincipal) {
                    kerbPrinciple = (KerberosPrincipal) object;
                    break;
                }
            }
            if( kerbPrinciple != null ){
                return "240 Kerb creds set";
            }

            BufferedInputStream bsin = new BufferedInputStream(this.ss.
                    getInputStream());

            if (log.isDebugEnabled()) {
                log.debug("[" + cosignConId + "] RETR " + serviceName + "=" +
                          cookie + " tgt");
            }

            // Send the cookie to the cosign server
            String commmand = "RETR " + serviceName + "=" + cookie + " tgt";
            this.sout.println(commmand);

            this.sout.flush();
            byte[] buff = new byte[1024];
            ArrayList arr = new ArrayList();
            int numRead = bsin.read(buff);
            int line = 0;
            String st = new String(buff, 0, numRead - 2);
            if (st.equalsIgnoreCase("240 retrieving file")) {
                while ((numRead != -1) && (numRead != 0) && (numRead != 3)) {
                    byte[] a = new byte[numRead];
                    System.arraycopy(buff, 0, a, 0, numRead);
                    arr.add(line++, a);
                    buff = new byte[1024];
                    numRead = bsin.read(buff);
                }

            } else {
                //todo:
                //derive error code
                return st;
            }
            log.info("Retrieved kerberos bytes - next write out tgt");
            File file = File.createTempFile("temp", ".tmp",
                                            new File((String) CosignConfig.
                    INSTANCE.getPropertyValue(CosignConfig.
                                              KERBEROS_TICKET_CACHE_DIRECTORY)));
            log.info("Opening file: " + file.getAbsolutePath() + " - write out tgt");
            FileOutputStream fw = new FileOutputStream(file);
            fw.write((byte[]) arr.get(2));
            fw.flush();
            fw.close();
            log.info("closing file : " + file.getAbsolutePath() + " - write out tgt");
            //tells where kdc, and other kerb options can be found
            System.setProperty("java.security.krb5.conf",
                               (String) CosignConfig.
                               INSTANCE.getPropertyValue(CosignConfig.
                    KERBEROS_KERB5_CONF));

            String pName = sPrinciple.getName() + "@" + sPrinciple.getRealm();

            Map options = new HashMap();
            options.put("principal", pName);
            options.put("useTicketCache", "true");
            options.put("client", "true");
            options.put("debug",
                        ((Boolean) CosignConfig.INSTANCE.getPropertyValue(CosignConfig.KERBEROS_KERB5_DEBUG)).booleanValue() ?
                        "true" : "false");
            options.put("doNotPrompt", "true");
            options.put("ticketCache", file.getAbsolutePath());
            //System.setProperty("java.security.krb5.realm", "UMICH.EDU");
            //System.setProperty("java.security.krb5.kdc", "fear.ifs.umich.edu");
            System.setProperty("useSubjectCredsOnly", "true");
            System.setProperty("sun.security.krb5.debug",
                               ((Boolean)
                               CosignConfig.INSTANCE.getPropertyValue(CosignConfig.
                    KERBEROS_KERB5_DEBUG)).booleanValue() ? "true" : "false");
             log.info("Instantiating login module");

            Krb5LoginModule lc = new Krb5LoginModule();

            lc.initialize(subject, new TextCallbackHandler(),
                          (Map)new HashMap(), options);
            boolean ok = lc.login();


            if (ok) {
                log.info("kerberos login ok");
                lc.commit();
                iterator = subject.getPrincipals().iterator();
           while (iterator.hasNext()) {
               Object object = iterator.next();
               if (object instanceof KerberosPrincipal) {
                   kerbPrinciple = (KerberosPrincipal) object;
                   break;
               }
           }
           if( kerbPrinciple != null ){
               return "240 Kerb creds set";
           }

            } else {
                st = "449 Bad Ticket";
            }
            boolean success = file.delete();
            if (!success) {
                // Deletion failed
            }



            return st;

        } catch (Exception e) {
            log.info("Exception in kerberos intance: " + e.getMessage());
            if (log.isDebugEnabled()) {
                log.debug("[" + cosignConId +
                          "] failed while validating cookie with cosign server",
                          e);
            }
            return null;
        }
    }


    /**
     * This method retreives 0-n proxy cookies defined in the cosign server.
     * It will then place these "cookies" in the cosign principle within the current
     * Subject to be used in external code.
     * @param serviceName   The cosign service name e.g. cosign-wolverineaccess
     * @param cookie    The base64 cosign service cookie
     * @param subject   The current threads jaas subject
     * @param sPrinciple The priciple just receieved from the server in a check cookie
     * @return        cosign error codes
     *
     */
    public String retrieveProxyCookies(String serviceName, String cookie,
                              Subject subject, CosignPrincipal sPrinciple) {
        CosignPrincipal cosignPrinciple = null;
        try {

            // Check if proxy cookies already exists.
            Iterator iterator = subject.getPrincipals().iterator();
            while (iterator.hasNext()) {
                Object object = iterator.next();
                if (object instanceof CosignPrincipal) {
                    cosignPrinciple = (CosignPrincipal) object;
                    break;
                }
            }
            if( cosignPrinciple != null ){
                cosignPrinciple.clearProxyCookies();

            }else{
                //throw new Exception("Cosign principle is null, unable to proceed with Proxy cookies.");
                cosignPrinciple = sPrinciple;
            }


            BufferedInputStream bsin = new BufferedInputStream(this.ss.
                    getInputStream());

            if (log.isDebugEnabled()) {
                log.debug("[" + cosignConId + "] RETR " + serviceName + "=" + cookie + " cookies");
            }

            // Send the request to the cosign server
            String commmand = "RETR " + serviceName + "=" + cookie + " cookies";
            this.sout.println(commmand);

            this.sout.flush();
            byte[] buff = new byte[1024];
            ArrayList arr = new ArrayList();
            int numRead = bsin.read(buff);

            String st = new String(buff, 0, numRead - 2);
            if (st.startsWith("241") ){
                while (st.indexOf("Cookies registered")==-1) {
                    arr.add(st);
                    ProxyCookie pc = new ProxyCookie(st,serviceName);
                    cosignPrinciple.addProxyCookie(pc);
                    numRead = bsin.read(buff);
                    st = new String(buff, 0, numRead - 2);
                }

            } else {
                //todo:
                //derive error code
                return st;
            }

        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("[" + cosignConId +
                          "] failed while validating cookie with cosign server",
                          e);
            }

        }
        return null;
    }




    /**
     * This methods check whether the secure Cosign connection is
     * still alive by issuing a NOOP request to the Cosign server.
     * @return  True if the socket/server is alive. False otherwise.
     */
    public boolean isConnectionValid() {
        try {
            if (log.isDebugEnabled()) {
                log.debug("[" + cosignConId + "] NOOP");
            }

            this.sout.println("NOOP");
            this.sout.flush();
            String result = sin.readLine();

            if (log.isDebugEnabled()) {
                log.debug("[" + cosignConId + "] result NOOP: " + result);
            }
            if (null == result) {
                return false;
            }
            return true;
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("[" + cosignConId +
                          "] failed while checking connection to cosign server",
                          e);
            }
            return false;
        }
    }

    /**
     * This method hard closes the Cosign connections.
     */
    public void close() {
        if (log.isDebugEnabled()) {
            log.debug("[" + cosignConId + "]: hard closing cosign connection");
        }

        try {
            if (null != this.sout) {
                this.sout.close();
            }
            if (null != this.in) {
                this.sin.close();
            }
            if (null != this.out) {
                this.out.close();
            }
            if (null != this.in) {
                this.in.close();
            }
            if (null != this.ss) {
                this.ss.close();
            }
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("[" + cosignConId +
                          "]: failed to close CosignConnection!", e);
            }
        }
        sout = null;
        sin = null;
        out = null;
        in = null;
        ss = null;
        this.protVersion = (float) 0.0;
    }

    /**
     * This method creates a socket to the cosign server, reads
     * the banner, converts the non-SSL socket to an SSL one and
     * starts an SSL handshake and then properly set the input and
     * out streams.
     * @throws IOException  If any socket/SSL exceptions occurs
     */
    private void init() throws IOException {
        try {
            // Creates non-SSL socket
            Socket s = new Socket(hostAddr, port);
            s.setSoTimeout(10000); //milliseconds
            s.setSoLinger(true, 10); //seconds
            // Gets the input/output reader/writer
            this.in = new BufferedReader(new InputStreamReader(s.getInputStream()));
            this.out = new PrintWriter(s.getOutputStream());

            // This reads welcome message from the cosign server
            String response = in.readLine();
            StringTokenizer Tok = new StringTokenizer(response);
            String tokString = null;
            if (Tok.hasMoreElements()) {
                tokString = (String) Tok.nextElement(); //the numeric results code
                tokString = (String) Tok.nextElement(); //protocol vesion if any
                try {
                    Float protVer = new Float(tokString);
                    this.protVersion = protVer.floatValue();
                } catch (NumberFormatException ex) {
                    this.protVersion = (float) 1.0;
                }
            }
            if (!(this.protVersion < 2.0)) {
                CosignConfig.INSTANCE.setServerVersion("2");
            } else {
                CosignConfig.INSTANCE.setServerVersion("1");
            }

            if (log.isDebugEnabled()) {
                log.debug("[" + cosignConId + "]: initializing ...");
                log.debug("[" + cosignConId + "]: reading banner!");
                log.debug("[" + cosignConId + "]: result: " + response);
            }
//check for protocol version


            // Notifies the cosign to begin SSL communication
            if (this.protVersion >= (float) 2.0) {
                out.println("STARTTLS 2");
            } else {
                out.println("STARTTLS");
            }
            out.flush();

            // Get cosign server's response
            response = in.readLine();

            if (log.isDebugEnabled()) {
                log.debug("[" + cosignConId + "]: result STARTTLS: " + response);
            }

            // Convert existing socket to an SSL one
            SSLSocket ss = CosignSSLSocketFactory.INSTANCE.createSSLSocket(s,
                    hostAddr, port, true);

            // Shows all the cipher suites
            if (log.isDebugEnabled()) {
                String[] cipherSuites = ss.getEnabledCipherSuites();
                for (int i = 0; i < cipherSuites.length; i++) {
                    log.info("[" + cosignConId + "]: enabled Ciper Suite: " +
                             cipherSuites[i]);
                }
            }

            // Start the SSL handshake
            ss.startHandshake();
            this.ss = ss;
            // Set the SSL input/output reader/writer
            sin = new BufferedReader(new InputStreamReader(ss.getInputStream()));
            sout = new PrintWriter(ss.getOutputStream());
            if (this.protVersion >= (float) 2.0) {
                response = sin.readLine();
                if (log.isDebugEnabled()) {
                    log.debug("[" + cosignConId +
                              "]: result 2.0 or greater SSLCONNECT " +
                              response);
                }
            }
        } catch (IOException ioe) {
            log.debug("[" + cosignConId + "]: failed to init CosignConnection",
                      ioe);
            throw new IOException("Failed in CosignConnection init()!");
        }
    }

}

/*Copyright (c) 2002-2008 Regents of The University of Michigan.
All Rights Reserved.

    Permission to use, copy, modify, and distribute this software and
    its documentation for any purpose and without fee is hereby granted,
    provided that the above copyright notice appears in all copies and
    that both that copyright notice and this permission notice appear
    in supporting documentation, and that the name of The University
    of Michigan not be used in advertising or publicity pertaining to
    distribution of the software without specific, written prior
    permission. This software is supplied as is without expressed or
    implied warranties of any kind.

The University of Michigan
c/o UM Webmaster Team
Arbor Lakes
Ann Arbor, MI  48105
*/