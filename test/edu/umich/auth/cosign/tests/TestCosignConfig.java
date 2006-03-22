package edu.umich.auth.cosign.tests;

import java.io.*;
import java.util.*;

import edu.umich.auth.cosign.*;
import edu.umich.auth.cosign.util.*;
import junit.framework.*;

/**
 * <p>Title: </p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: </p>
 *
 * @author not attributable
 * @version 1.0
 */
public class TestCosignConfig extends TestCase {
    private CosignConfig cosignConfig = null;
    File file = new File("C:\\JavaWorkII\\JavaCosignII\\test\\edu\\umich\\auth\\cosign\\tests\\testdata.txt");
    private Vector cases = new Vector();
    public TestCosignConfig(String name) {
        super(name);
    }

    private class TestCase{
        String dir=null;
        String resource=null;
        String qs=null;
        String service=null;
        /**
         * TestContents
         */
        public TestCase() {
        }

        public void setDir(String newDir){
            this.dir = newDir;
        }
        public String getDir(){
            return this.dir;
        }
        public void setResource(String newRes){
            this.resource = newRes;
        }
        public String getResource(){
            return this.resource;
        }
        public void setQs(String newQs){
            this.qs = newQs;
        }
        public String getQs(){
            return this.qs;
        }
        public void setService(String newService){
            this.service = newService;
        }
        public String getService(){
            return this.service;
        }



    }
    protected void setUp() throws Exception {
        super.setUp();
        try {
        BufferedReader in = new BufferedReader(new FileReader(file));
        String str;
        while ((str = in.readLine()) != null) {
            process(str);
        }
        in.close();
    } catch (IOException e) {
        System.out.println("Exception: " + e.getMessage());
    }

        cosignConfig = CosignConfig.INSTANCE;
        cosignConfig.setConfigFilePath("C:\\JavaWorkII\\JavaCosignII\\CoTestWeb\\WEB-INF\\conf\\cosignConfig.xml");
    }

    private void process(String str){
        if(str.startsWith("/*")||str.startsWith("//*"))
            return;
        String next=null;
        StringTokenizer st = new StringTokenizer(str,",;");
        TestCase testCase = new TestCase();
        next = st.nextToken();
        if(!next.equalsIgnoreCase(" "))
            testCase.setDir(next);
        next = st.nextToken();
        if(!next.equalsIgnoreCase(" "))
            testCase.setResource(next);
        next = st.nextToken();
        if(!next.equalsIgnoreCase(" "))
            testCase.setQs(next);
        next = st.nextToken();
        if(!next.equalsIgnoreCase(" "))
            testCase.setService(next);
        cases.add(testCase);
     }

    protected void tearDown() throws Exception {
        cosignConfig = null;
        super.tearDown();
    }

    public void testHasServiceOveride() {
        String path = "";
        String resource = "";
        String qString = "";
        ServiceConfig expectedReturn = null;
        Enumeration numer = cases.elements();
        while(numer.hasMoreElements()){
            TestCase tcase = (TestCase) numer.nextElement();
            ServiceConfig actualReturn = cosignConfig.hasServiceOveride(tcase.getDir(), tcase.getResource(), tcase.getQs());
            if( actualReturn != null && actualReturn.getName().equalsIgnoreCase(tcase.getService())){
               System.out.println("Service Found, complete match: " + tcase.getService() + " -Is public: " + actualReturn.isPublicAccess());
            }
            else
                if( actualReturn != null )
                     System.out.println("Service Found, NOT complete match service name returned: " + actualReturn.getName() + " -Is public: " + actualReturn.isPublicAccess());
                    else
                        System.out.println("Test Case Evaluated to false, no service returned, case service: " + tcase.getService());
        }
            }

}
