package eu.fusepool.dedup.transformer;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.wymiwyg.commons.util.arguments.ArgumentHandler;

import eu.fusepool.p3.transformer.sample.Arguments;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;

import javax.xml.transform.TransformerException;

import org.apache.commons.io.IOUtils;
import org.xml.sax.SAXException;

public class SilkConfigFileParser {

    Document doc = null;
    String configFile = null;

    public SilkConfigFileParser(String configFile) {
        try {
            this.configFile = configFile;
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            doc = dBuilder.parse(configFile);
            doc.getDocumentElement().normalize();
        } catch (ParserConfigurationException ex) {
            throw new RuntimeException(ex);
        } catch (SAXException ex) {
            throw new RuntimeException(ex);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void updateSourceFile(String sourceFileName) throws IOException {
        NodeList nList = doc.getElementsByTagName("DataSource");
        for (int i = 0; i < nList.getLength(); i++) {
            Node dataSourceNode = nList.item(i);
            if (dataSourceNode.getNodeType() == Node.ELEMENT_NODE) {
                Element dataSourceElement = (Element) dataSourceNode;
                if (dataSourceElement.getAttribute("type").equals("file")) {
                    NodeList paramNodes = dataSourceElement.getChildNodes();
                    for (int j = 0; j < paramNodes.getLength(); j++) {
                        Node paramNode = paramNodes.item(j);
                        //System.out.println("\nCurrent Element :" + paramNode.getNodeName());
                        if (paramNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element paramElem = (Element) paramNode;
                            if (paramElem.getAttribute("name").equals("file")) {
                                String currentSourceFileName = paramElem.getAttribute("value");
                                System.out.println("Source file name: " + currentSourceFileName);
                                Attr fileName = paramElem.getAttributeNode("value");
                                String updatedSourceFileName = sourceFileName;
                                fileName.setTextContent(updatedSourceFileName);
                                System.out.println("New source file: " + updatedSourceFileName);
                            }

                        }

                    }
                }
            }
        }
    }

    public void updateTargetFile(String targetFileName) {
        NodeList nList = doc.getElementsByTagName("DataSource");
    }

    public void updateOutputFile(String acceptFileName) throws IOException {
        NodeList nList = doc.getElementsByTagName("Output");
        for (int temp = 0; temp < nList.getLength(); temp++) {
            Node outputNode = nList.item(temp);
            //System.out.println("\nCurrent Element :" + outputNode.getNodeName());
            if (outputNode.getNodeType() == Node.ELEMENT_NODE) {
                Element outputElement = (Element) outputNode;
                //System.out.println("Output attribute: " + outputElement.getAttribute("type"));
                NodeList paramNodes = outputElement.getChildNodes();
                for (int i = 0; i < paramNodes.getLength(); i++) {
                    Node paramNode = paramNodes.item(i);
                    //System.out.println("\nCurrent Element :" + paramNode.getNodeName());
                    if (paramNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element paramElem = (Element) paramNode;
                        if (paramElem.getAttribute("name").equals("file")) {
                            String currentAcceptFileName = paramElem.getAttribute("value");
                            System.out.println("Current accept file name: " + currentAcceptFileName);                            
                            Attr fileName = paramElem.getAttributeNode("value");
                            String updatedAcceptFileName = acceptFileName;
                            System.out.println("New accept file name: " + updatedAcceptFileName);
                            fileName.setTextContent(acceptFileName);
                        }

                    }

                }

            }
        }

    }

    public void saveChanges() {
        try {
            // write the content into xml file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(configFile));
            System.out.println("Saving updated file in: " + configFile);
            transformer.transform(source, result);
        } catch (TransformerConfigurationException ex) {
            throw new RuntimeException(ex);
        } catch (TransformerException ex) {
            throw new RuntimeException(ex);
        }
    }

    private String getCurrentDir() throws IOException {
        return new java.io.File(".").getCanonicalPath();
    }

    public static void main(String[] args) throws Exception {
        InputStream in = (SilkConfigFileParser.class).getResourceAsStream("silk-config-file.xml");
        File configFile = inputStreamToFile(in);
        File rdfFile = File.createTempFile("input-rdf-", ".ttl");
        File outFile = File.createTempFile("output-", ".nt");
        SilkConfigFileParser parser = new SilkConfigFileParser(configFile.getAbsolutePath());
		parser.updateOutputFile(outFile.getAbsolutePath());
		parser.updateSourceFile(rdfFile.getAbsolutePath());
		parser.saveChanges();
    }
    
    public static File inputStreamToFile(InputStream in) throws IOException {
        OutputStream out = null;
        try {
            File temp = File.createTempFile("silk-config-file-", ".xml");
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
