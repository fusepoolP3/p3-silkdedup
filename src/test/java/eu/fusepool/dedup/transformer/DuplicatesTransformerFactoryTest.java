package eu.fusepool.dedup.transformer;

import static org.junit.Assert.*;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DuplicatesTransformerFactoryTest {

	DuplicatesTransformerFactory factory = null;
	@Before
	public void setUp() throws Exception {
		factory = new DuplicatesTransformerFactory();
	}

	@Test
	public void testGetRequestParamValue() {	  
		String queryString = "async=true&config=http://example.org/silk-config.xml";
	    Assert.assertTrue("http://example.org/silk-config.xml".equals(factory.getRequestParamValue(queryString, factory.SILK_CONFIG_FILE_URI_PARAM)));
	    Assert.assertTrue("true".equals(factory.getRequestParamValue(queryString, factory.TRANSFORMER_ASYNC_PARAM)));
	}

}
