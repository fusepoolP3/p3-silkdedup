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
import java.util.HashMap;
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

    final String BASE_URI = "http://example.org/";
    final String TURTLE_MIME_TYPE = "text/turtle";
    final String RDF_MIME_TYPE = "application/rdf+xml";
    String silkDatasourceFormat = "text/turtle";
    String supportedFormat;
    MimeType mimeType;

    private static final Logger log = LoggerFactory.getLogger(DuplicatesTransformer.class);

    public DuplicatesTransformer() {
    }

    @Override
    public Set<MimeType> getSupportedInputFormats() {
        try {
              Set<MimeType> mimeSet = new HashSet<MimeType>();
              mimeSet.add(new MimeType(TURTLE_MIME_TYPE + ";charset=UTF-8"));
              mimeSet.add(new MimeType(RDF_MIME_TYPE + ";charset=UTF-8"));
              return Collections.unmodifiableSet(mimeSet);
        } catch (MimeTypeParseException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    protected TripleCollection generateRdf(HttpRequestEntity entity) throws IOException {
    	mimeType = entity.getType();
    	supportedFormat = entity.getType().getBaseType();
    	silkDatasourceFormat = mimeType.getBaseType().startsWith(RDF_MIME_TYPE) ? "RDF/XML" : "TURTLE"; 
        final InputStream inputRdfData = entity.getData();
        TripleCollection duplicates = findDuplicates(inputRdfData);
        return duplicates;
    }

    /**
     * Finds duplicates in a graph. The Silk configuration file is updated with the current source and output file paths. 
     * The updated configuration file and the input RDF data and the output files are stored in the /tmp/ folder.
     * @param inputRdf
     * @return
     * @throws IOException
     */
    protected TripleCollection findDuplicates(InputStream inputRdf) throws IOException {
        File configFile = FileUtil.inputStreamToFile(getClass().getResourceAsStream("silk-config-file.xml"), "silk-config-", ".xml");
        File rdfFile = File.createTempFile("input-rdf-", ".rdf");
        File outFile = File.createTempFile("output-", ".nt");
        //update the config file with the paths of the input and output files
        SilkConfigFileParser parser = new SilkConfigFileParser(configFile.getAbsolutePath());
		parser.updateOutputFile(outFile.getAbsolutePath());
		parser.updateSourceDataSourceFile(rdfFile.getAbsolutePath(), silkDatasourceFormat);
		parser.updateTargetDataSourceFile(rdfFile.getAbsolutePath(), silkDatasourceFormat);
		parser.saveChanges();
		//save the data coming from the stream into a temp file 
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
        parser.parse(inputGraph, inputRdfData, supportedFormat, null);
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

}
