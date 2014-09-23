package eu.fusepool.dedup.transformer;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.wymiwyg.commons.util.arguments.ArgumentHandler;

import eu.fusepool.p3.transformer.sample.Arguments;

public class SilkConfigFileParser {
	
	private static final Logger log = LoggerFactory.getLogger(SilkConfigFileParser.class);
	
	public String readSilkConfig(String configFile) throws Exception { 
		String accetFileName = "";
		File fXmlFile = new File(configFile);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(fXmlFile);
		doc.getDocumentElement().normalize();
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
						  accetFileName = paramElem.getAttribute("value");
					  }
					
					}
				
				}
				
	 
			}
		}
		
		return accetFileName;
	   
	}
	
	public static void main(String[] args) throws Exception {
		final String SILK_CONFIG_FILE = "src/main/resources/silk-config-file.xml";
		SilkConfigFileParser parser = new SilkConfigFileParser();
		System.out.println(parser.readSilkConfig(SILK_CONFIG_FILE));
		
        
    }

}
