package ceras.simulation.cloudsim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.transform.TransformerException;

import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.NodeIterator;

import cloudsim.Datacenter;
import cloudsim.DatacenterCharacteristics;
import cloudsim.Host;
import cloudsim.VmAllocationPolicySimple;

import com.sun.org.apache.xpath.internal.XPathAPI;

public class SimulationClient {

	DOMParser parser = null;
	Document resultdoc = null;

	public SimulationClient(Document resultdoc) {
		super();
		this.resultdoc = resultdoc;
	}

	DOMParser getParser() {

		if (parser != null)
			return parser;
		else {
			try {
				parser = new DOMParser();
				parser.setFeature(
						"http://apache.org/xml/features/dom/defer-node-expansion",
						true);
				parser.setFeature(
						"http://xml.org/sax/features/validation",
						true);
				parser.setFeature(
						"http://xml.org/sax/features/namespaces",
						true);
				parser.setFeature(
						"http://apache.org/xml/features/validation/schema",
						true);
				parser.setErrorHandler(new CSErrorHandler());
			} catch (org.xml.sax.SAXNotRecognizedException ex) {
			} catch (org.xml.sax.SAXNotSupportedException ex) {
			}

		}
		return parser;
	}
	
	public Map<String,Double> getResponseTime(){
		Map<String,Double> users = new HashMap<String,Double>();

		try {
			NodeIterator userIter=XPathAPI.selectNodeIterator(resultdoc, "Results/User");

			Node userN = userIter.nextNode(); 			
			while (userN!=null){
				Element user = (Element)userN;
				String name = user.getAttribute("userId");
				Double responseTime = Double.valueOf(user.getAttribute("ResponseTime")); 
				users.put(name, responseTime);				
				userN = userIter.nextNode();
			}
		} catch (TransformerException e) {

			e.printStackTrace();
		}
		return users; 
	}
	
	public double getResponseTime(String userId){
		double rs = 0;
		try {
			Node rsN=XPathAPI.selectSingleNode(resultdoc, "Results/User[@userId=\""+userId+"\"]/@ResponseTime"); 			
			rs = (rsN!=null && !(rsN.getNodeValue().length()==0)) ? 
					Double.valueOf(rsN.getNodeValue()) : 0; 
		} catch (Exception e) {
			e.printStackTrace();
		} 
		return rs;		
	}
	
}
