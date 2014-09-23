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

public class SilkConfigFileParser {
	
	Document doc = null;
	String configFile = "";
	
	public SilkConfigFileParser(String configFile) throws Exception{
		this.configFile = configFile;
		File fXmlFile = new File(configFile);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		doc = dBuilder.parse(fXmlFile);
		doc.getDocumentElement().normalize();
	}
	
	
	public void updateSourceFile(String sourceFileName) throws Exception {
		NodeList nList = doc.getElementsByTagName("DataSource");
		for(int i = 0; i < nList.getLength(); i++){
			Node dataSourceNode = nList.item(i);
			if (dataSourceNode.getNodeType() == Node.ELEMENT_NODE) {
				Element dataSourceElement = (Element) dataSourceNode;
				if(dataSourceElement.getAttribute("type").equals("file")) {
					NodeList paramNodes = dataSourceElement.getChildNodes();
					for (int j = 0; j < paramNodes.getLength(); j++ ) {
						Node paramNode = paramNodes.item(j);
						//System.out.println("\nCurrent Element :" + paramNode.getNodeName());
						if (paramNode.getNodeType() == Node.ELEMENT_NODE) {
						  Element paramElem = (Element) paramNode; 	
						  if(paramElem.getAttribute("name").equals("file")) {
							  String currentSourceFileName = paramElem.getAttribute("value");
							  System.out.println("Source file name: " + currentSourceFileName);
							  Attr fileName = paramElem.getAttributeNode("value");
							  fileName.setTextContent(getCurrentDir() + "/" + sourceFileName);
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
	
	public void updateOutputFile(String acceptFileName) throws Exception { 	
		NodeList nList = doc.getElementsByTagName("Output");
		for (int temp = 0; temp < nList.getLength(); temp++) {
			Node outputNode = nList.item(temp);
			//System.out.println("\nCurrent Element :" + outputNode.getNodeName());
			if (outputNode.getNodeType() == Node.ELEMENT_NODE) {
				Element outputElement = (Element) outputNode;
				//System.out.println("Output attribute: " + outputElement.getAttribute("type"));
				NodeList paramNodes = outputElement.getChildNodes();
				for (int i = 0; i < paramNodes.getLength(); i++ ) {
					Node paramNode = paramNodes.item(i);
					//System.out.println("\nCurrent Element :" + paramNode.getNodeName());
					if (paramNode.getNodeType() == Node.ELEMENT_NODE) {
					  Element paramElem = (Element) paramNode; 	
					  if(paramElem.getAttribute("name").equals("file")) {
						  String currentAcceptFileName = paramElem.getAttribute("value");
						  System.out.println("Current accept file name: " + currentAcceptFileName);
						  Attr fileName = paramElem.getAttributeNode("value");
						  fileName.setTextContent(getCurrentDir() + "/" + acceptFileName);
					  }
					
					}
				
				}
				
	 
			}
		}
	    
	}
	
	public void saveChanges() throws Exception {
		// write the content into xml file
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(new File(configFile));
		transformer.transform(source, result);
	}
	
	private String getCurrentDir() throws IOException {
		return new java.io.File( "." ).getCanonicalPath();
	}
	
	public static void main(String[] args) throws Exception {
		final String SILK_CONFIG_FILE = "src/test/resources/silk-test-config-file.xml";
		SilkConfigFileParser parser = new SilkConfigFileParser(SILK_CONFIG_FILE);
		parser.updateOutputFile("src/test/resources/accepted_links.nt");
		parser.updateSourceFile("src/test/resources/testfoaf.ttl");
		parser.saveChanges();
    }

}
