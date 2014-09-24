/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.fusepool.dedup.transformer;

import eu.fusepool.p3.transformer.HttpRequestEntity;
import eu.fusepool.p3.transformer.RdfGeneratingTransformer;
import de.fuberlin.wiwiss.silk.Silk;
import de.fuberlin.wiwiss.silk.config.LinkingConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.ontologies.OWL;
import org.apache.clerezza.rdf.utils.smushing.SameAsSmusher;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DuplicatesTransformer extends RdfGeneratingTransformer {

    final String SILK_CONFIG_FILE = "src/main/resources/silk-config-file.xml";
    final String SILK_RESULT_FILE = "src/main/resources/accepted_links.nt";
    final String BASE_URI = "http://example.org/";


    private static final Logger log = LoggerFactory.getLogger(DuplicatesTransformer.class);

    public DuplicatesTransformer() {
    }

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
        File configFile = inputStreamToFile(getClass().getResourceAsStream("silk-config-file.xml"));

        
        File rdfFile = File.createTempFile("input", ".ttl");
        File outFile = File.createTempFile("output", ".nt");
        SilkConfigFileParser parser = new SilkConfigFileParser(SILK_CONFIG_FILE);
		parser.updateOutputFile(outFile.getAbsolutePath());
		parser.updateSourceFile(rdfFile.getAbsolutePath());
		parser.saveChanges();
        FileOutputStream outRdf = new FileOutputStream(rdfFile);
        IOUtils.copy(inputRdf, outRdf);
        inputRdf.close();
        outRdf.close();

        // interlink entities
        Silk.executeFile(configFile, null, 1, true);
        log.info("Interlinking task completed.");

        // returns the result to the client
        return parseResult(outFile);
    }

    /**
     * Smushes the input RDF graph using the of equivalent links. Returns the
     * same graph replacing all the equivalent URIs with a preferred one adding
     * all the statements to it.
     *
     * @param inputRdfData
     * @param duplicates
     * @return
     */
    protected TripleCollection smushData(InputStream inputRdfData, TripleCollection duplicates) {
        MGraph inputGraph = new SimpleMGraph();
        Parser parser = Parser.getInstance();
        parser.parse(inputGraph, inputRdfData, SupportedFormat.TURTLE, null);
        SameAsSmusher smusher = new SameAsSmusher() {
            @Override
            protected UriRef getPreferedIri(Set<UriRef> uriRefs) {
                UriRef preferedIri = null;
                Set<UriRef> canonUris = new HashSet<UriRef>();
                for (UriRef uriRef : uriRefs) {
                    if (uriRef.getUnicodeString().startsWith(BASE_URI)) {
                        canonUris.add(uriRef);
                    }
                }
                if (canonUris.size() > 0) {
                    preferedIri = canonUris.iterator().next();
                }
                if (canonUris.size() == 0) {
                    preferedIri = uriRefs.iterator().next();
                }
                return preferedIri;
            }
        };

        //smusher.smush(inputGraph, duplicates, true); //remove the use of a LockableMGraph
        return inputGraph;
    }

    /**
     * Reads the silk output (n-triples) and returns the owl:sameas statements
     * as a result
     *
     * @param file
     * @return
     * @throws IOException
     */
    public TripleCollection parseResult(File file) throws IOException {
        Parser parser = Parser.getInstance();
        return parser.parse(new FileInputStream(file), SupportedFormat.N_TRIPLE);
    }


    @Override
    public boolean isLongRunning() {
        // TODO Auto-generated method stub
        return false;
    }


    public File inputStreamToFile(InputStream in) throws IOException {
        OutputStream out = null;
        try {
            File temp = File.createTempFile("lkj", ".txt");
            out = new FileOutputStream(temp);
            IOUtils.copy(in, out);
            return temp;
        } catch (FileNotFoundException ex) {
            throw new RuntimeException(ex);
        } finally {
            out.close();
        }
    }

}
