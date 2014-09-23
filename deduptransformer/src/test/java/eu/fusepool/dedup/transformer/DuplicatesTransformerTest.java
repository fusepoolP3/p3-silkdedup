package eu.fusepool.dedup.transformer;



import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.util.Iterator;

import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.hamcrest.core.StringContains;
import org.junit.Before;
import org.junit.Test;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;

import eu.fusepool.dedup.transformer.DuplicatesTransformer;
import eu.fusepool.p3.transformer.server.TransformerServer;

public class DuplicatesTransformerTest {
	
	final String SILK_CONFIG_FILE = "src/test/resources/silk-config-file.xml";
	final String SOURCE_RDF_FILE = "src/main/resources/inputdata.ttl";
	final String TARGET_RDF_FILE = "src/main/resources/inputdata.ttl";
	private String baseUri;
	private String rdfData;
	
	@Before
    public void setUp() throws Exception {
		
		File rdfFile = new File(SOURCE_RDF_FILE);
		InputStream in = new FileInputStream(rdfFile);
        rdfData = IOUtils.toString(in);
        in.close();
        
        final int port = findFreePort();
        baseUri = "http://localhost:"+port+"/";
        RestAssured.baseURI = "http://localhost:"+port+"/";
        TransformerServer server = new TransformerServer(port);
        server.start(new DuplicatesTransformer(SILK_CONFIG_FILE));
    }
	
	
    @Test
    public void turtleOnGet() {
        //Nothing specific here
        Response response = RestAssured.given().header("Accept", "text/turtle")
                .expect().statusCode(HttpStatus.SC_OK).header("Content-Type", "text/turtle").when()
                .get();
    }
    
	@Test
	public void testSilk() throws IOException {
		
		//Response response = 
        RestAssured.given().header("Accept", "text/turtle")
                .contentType("text/turtle;charset=UTF-8")
                .content(rdfData)
                .expect().statusCode(HttpStatus.SC_OK).content(new StringContains("hello")).header("Content-Type", "text/turtle").when()
                .post(baseUri);
	}
	
	public static int findFreePort() {
        int port = 0;
        try (ServerSocket server = new ServerSocket(0);) {
            port = server.getLocalPort();
        } catch (Exception e) {
            throw new RuntimeException("unable to find a free port");
        }
        return port;
    }

}
