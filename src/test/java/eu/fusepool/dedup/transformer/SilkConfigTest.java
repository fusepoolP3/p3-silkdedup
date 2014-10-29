package eu.fusepool.dedup.transformer;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.ServerSocket;
import java.util.Iterator;

import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.ontologies.OWL;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;

import eu.fusepool.p3.transformer.server.TransformerServer;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.WireMockServer;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

import org.junit.Rule;

public class SilkConfigTest {
	File ttlFile = null;
	private String transformerBaseUri;
	private byte[] silkconf;
	private byte[] ttlData;
	private static int mockPort = 0;
	
	//private final int WIREMOCK_PORT = 8089;
	@BeforeClass
	public static void setMockPort() {
		mockPort = findFreePort();
	}
	
	@Before
    public void setUp() throws Exception {
		ttlFile = FileUtil.inputStreamToFile(getClass().getResourceAsStream("testfoaf.ttl"), "test-", ".ttl");
        InputStream inttl = new FileInputStream(ttlFile);
        ttlData = IOUtils.toByteArray(inttl);
        inttl.close();
        
		silkconf = IOUtils.toByteArray(getClass().getResourceAsStream("silk-config-file.xml"));
		
		final int transformerServerPort = findFreePort();
        transformerBaseUri = "http://localhost:" + transformerServerPort + "/";
        RestAssured.baseURI = transformerBaseUri;
        TransformerServer server = new TransformerServer(transformerServerPort);
        server.start(new DuplicatesTransformer());
    
	}
	
	@After
	public void tearDown() throws Exception {
		ttlFile.delete();
	}
	
	@Rule
    public WireMockRule wireMockRule = new WireMockRule(mockPort);
	
	@Test
    public void testRemoteConfig() {
	    // Set up a service in the mock server to respond to a get request that must be sent by the transformer 
        // to fetch the silk config file 
		stubFor(get(urlEqualTo("/fusepoolp3/silk-config-file.xml"))
	    	    .willReturn(aResponse()
	    			.withStatus(HttpStatus.SC_OK)
	    			.withHeader("Content-Type", "text/xml")
	    			.withBody(silkconf)));
		
		// The response object acts as a transformer's client. It sends a post request
		// to the transformer with the url of the silk config file, the data to be interlinked
		// and gets the result from the transformer.
    	Response response = 
	    	RestAssured.given().header("Accept", "text/turtle")
	    	.contentType("text/turtle")
	    	.content(ttlData)
	    	.expect().statusCode(HttpStatus.SC_OK).when()
	    	.post("/?config=http://localhost:" + mockPort +"/fusepoolp3/silk-config-file.xml");
    	
    	Graph graph = Parser.getInstance().parse(response.getBody().asInputStream(), "text/turtle");
        Iterator<Triple> typeTriples = graph.filter(null, OWL.sameAs, null);
        Assert.assertTrue("No equivalent entities found", typeTriples.hasNext());
    	
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
