package com.lexalytics.kstreamluwak;

import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class TestMonitorTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public TestMonitorTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( TestMonitorTest.class );
    }

    /**
     * Rigourous Test :-)
     * @throws IOException 
     */
    public void testBasic() throws IOException
    {
    	FilteringMonitor monitor = new FilteringMonitor();
    	assertFalse( monitor.test(null, "d d1 foobar") );
    	assertFalse( monitor.test(null, "q q1 foobar") );
    	assertTrue( monitor.test(null, "d d1 foobar") );
    }
}
