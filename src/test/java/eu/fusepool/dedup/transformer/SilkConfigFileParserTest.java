package eu.fusepool.dedup.transformer;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SilkConfigFileParserTest {

    @Before
    public void setUp() throws Exception {}

    @Test
    public void testGetTargetDataSourceType() throws IOException {
        InputStream in = (SilkConfigFileParser.class).getResourceAsStream("silk-config-dbpedia.xml");
        File configFile = FileUtil.inputStreamToFile(in,"silk-config-dbpedia-",".xml");
        SilkConfigFileParser parser = new SilkConfigFileParser(configFile.getAbsolutePath());
        String targetType = parser.getTargetDataSourcetype();
        Assert.assertTrue("sparqlEndpoint".equals(targetType));
    }

}
