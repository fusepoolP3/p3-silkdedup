/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.fusepool.dedup.transformer;

import de.fuberlin.wiwiss.silk.config.LinkingConfig;
import eu.fusepool.p3.transformer.HttpRequestEntity;
import eu.fusepool.p3.transformer.RdfGeneratingTransformer;
import de.fuberlin.wiwiss.silk.Silk;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.LockableMGraph;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.impl.SimpleGraph;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.clerezza.rdf.core.serializedform.ParsingProvider;
import org.apache.clerezza.rdf.core.serializedform.SerializingProvider;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.jena.parser.JenaParserProvider;
import org.apache.clerezza.rdf.ontologies.OWL;
import org.apache.clerezza.rdf.utils.smushing.SameAsSmusher;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DuplicatesTransformer extends RdfGeneratingTransformer {
	final String SILK_CONFIG_FILE = "src/main/resources/silk-config-file.xml";
	final String INPUT_RDF_FILE = "src/main/resources/inputdata.ttl";
	final String SILK_RESULT_FILE = "src/main/resources/accepted_links.nt";
	final String BASE_URI = "http://example.org/";
	
	private static final Logger log = LoggerFactory.getLogger(DuplicatesTransformer.class);

    @Override
    public Set<MimeType> getSupportedInputFormats() {
        try {
            MimeType mimeType = new MimeType("text/plain;charset=UTF-8");
            return Collections.singleton(mimeType);
        } catch (MimeTypeParseException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    @Override
	protected TripleCollection generateRdf(HttpRequestEntity entity) throws IOException {
    	final InputStream inputRdfData = entity.getData();
    	TripleCollection duplicates = findDuplicates(inputRdfData);
		return duplicates;
	}

    protected TripleCollection findDuplicates(InputStream inputRdf) throws IOException {
        File configFile = new File(SILK_CONFIG_FILE);
        
        File rdfFile = new File(INPUT_RDF_FILE);
        FileOutputStream outRdf = new FileOutputStream(rdfFile);
        IOUtils.copy(inputRdf, outRdf);
        inputRdf.close();
        outRdf.close();
          
        
        // interlink entities
        Silk.executeFile(configFile, null, 1, true);
        log.info("Interlinking task completed.");
        
        
        // returns the result to the client
        return parseResult(SILK_RESULT_FILE);
    }
    /**
     * Smushes the input RDF graph using the of equivalent links. Returns the same graph replacing all the equivalent 
     * URIs with a preferred one adding all the statements to it.
     * @param inputRdfData
     * @param duplicates
     * @return
     */
    protected TripleCollection smushData(InputStream inputRdfData, TripleCollection duplicates){
    	MGraph inputGraph = new SimpleMGraph();
    	ParsingProvider parser = new JenaParserProvider();
    	parser.parse(inputGraph, inputRdfData, SupportedFormat.TURTLE, null);
    	SameAsSmusher smusher = new SameAsSmusher() {
    		@Override 
    		protected UriRef getPreferedIri(Set<UriRef> uriRefs) {
    			UriRef preferedIri = null;
    			Set<UriRef> canonUris = new HashSet<UriRef>();
    			for(UriRef uriRef: uriRefs) {
    				if(uriRef.getUnicodeString().startsWith(BASE_URI))
    					canonUris.add(uriRef);
    			}
    			if(canonUris.size() > 0)
    				preferedIri = canonUris.iterator().next();
    			if(canonUris.size() == 0)
    				preferedIri = uriRefs.iterator().next();
    			return preferedIri;
    		}
    	};
    	
    	//smusher.smush(inputGraph, duplicates, true); //remove the use of a LockableMGraph
    	return inputGraph; 
    }
    /**
     * Reads the silk output (n-triples) and returns the owl:sameas statements as a result
     * @param fileName
     * @return
     * @throws IOException
     */
    public TripleCollection parseResult(String fileName) throws IOException {
    	final TripleCollection links = new SimpleMGraph();
    	BufferedReader in = new BufferedReader(new FileReader(fileName));
    	String statement;
    	while((statement = in.readLine())!= null){
    		Triple link = new TripleImpl(getSubject(statement),OWL.sameAs,getObject(statement));
    		links.add(link);
    	}
    	in.close();
    	return links;
    }
    
    public UriRef getSubject(String statement){
    	int endOfSubjectIndex = statement.indexOf('>');
    	String subjectName = statement.substring(1, endOfSubjectIndex);
    	UriRef subjectRef = new UriRef(subjectName);
    	return subjectRef;
    }
    
    public UriRef getObject(String statement) {
    	int startOfObjectIndex = statement.lastIndexOf('<');
    	String objectName = statement.substring(startOfObjectIndex + 1, statement.length() - 1);
    	UriRef objectRef = new UriRef(objectName);
    	return objectRef;
    }
	
	@Override
	public boolean isLongRunning() {
		// TODO Auto-generated method stub
		return false;
	}

    
}
