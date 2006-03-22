package edu.umich.auth.cosign.tests;

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
public class TestSuite1 extends TestCase {

    public TestSuite1(String s) {
        super(s);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTestSuite(edu.umich.auth.cosign.tests.TestCosignConfig.class);
        return suite;
    }
}
