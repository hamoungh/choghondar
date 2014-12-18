package ceras.simulation.cloudsim;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
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
import org.xml.sax.InputSource;

import com.sun.org.apache.xpath.internal.XPathAPI;

import cloudsim.Cloudlet;
import cloudsim.CloudletScheduler;
import cloudsim.CloudletSchedulerSpaceShared;
import cloudsim.CloudletSchedulerTimeShared;
import cloudsim.Datacenter;
import cloudsim.DatacenterCharacteristics;
import cloudsim.Host;
import cloudsim.Log;
import cloudsim.Pe;
import cloudsim.Vm;
import cloudsim.VmAllocationPolicySimple;
import cloudsim.VmScheduler;
import cloudsim.VmSchedulerSpaceShared;
import cloudsim.VmSchedulerTimeShared;
import cloudsim.core.CloudSim;
import cloudsim.provisioners.BwProvisionerSimple;
import cloudsim.provisioners.PeProvisionerSimple;
import cloudsim.provisioners.RamProvisionerSimple;

public class Tester {
	
	Document doc = null;
	Document outDoc = null;
	String outXmlFile = null;
	DOMParser parser = null;
	PrintStream log;
	boolean verbose = false;

	public Document getOutDoc() {
		return outDoc;
	}

	public synchronized void saveDOMTree(OutputStream outs, Document doc)
	throws IOException {
		org.apache.xml.serialize.XMLSerializer out =
			new org.apache.xml.serialize.XMLSerializer(outs, null);
		out.serialize(doc);
		outs.close();
	}

	public void setOutXmlFile(String file){
		this.outXmlFile = file;
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

	public Document getResultsDocument() {
		return outDoc;
	}

	/**
	 * Prints the Cloudlet objects
	 * @param list  list of Cloudlets
	 */
	private void printCloudletList(List<Cloudlet> list) {
		int size = list.size();
		Cloudlet cloudlet;

		String indent = "    ";
		Log.printLine();
		Log.printLine("========== OUTPUT ==========");
		Log.printLine(
				"Cloudlet ID" 
				+ indent + "STATUS" 
				+ indent +"Data center ID" 
				+ indent + "VM ID"
				+ indent + "USER ID" 
				+ indent + indent + "Time"
				+ indent + indent + "Start Time"
				+ indent + "Duration");

		DecimalFormat dft = new DecimalFormat("###.##");
		for (int i = 0; i < size; i++) {
			cloudlet = list.get(i);
			Log.print(indent + cloudlet.getCloudletId() + indent + indent);

			if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS){
				Log.print("SUCCESS");

				Log.printLine(
						indent + indent + cloudlet.getResourceId() + 
						indent + indent + indent + cloudlet.getVmId() +
						indent + indent + indent + cloudlet.getUserId() +						
						indent + indent + indent + dft.format(cloudlet.getActualCPUTime()) +
						indent + indent + indent + dft.format(cloudlet.getExecStartTime()) +
						indent + indent + dft.format(cloudlet.getFinishTime()-cloudlet.getSubmissionTime())+ indent );
			}
		}

	}

	//	public void solve(){
	//		model.setOutDoc(new org.apache.xerces.dom.DocumentImpl());
	//		
	//		model.findConfigurations();
	//		results= model.getResultsDocument();
	//	}

	public List<Pe> createCores(String dcname, int hostId){
		List<Pe> peList = new ArrayList<Pe>();
		NodeIterator coreIter;
		try {
			coreIter = XPathAPI.selectNodeIterator(doc
					, "Model/Datacenter[@name=\""+dcname+"\"]/Host[@hostId=\""+hostId+"\"]/Core");

			Node coreN = coreIter.nextNode();
			while (coreN!=null){
				Element core = (Element)coreN;
				int coreId = Integer.valueOf(core.getAttribute("id"));
				int coreMips = Integer.valueOf(core.getAttribute("mips"));
				peList.add(new Pe(coreId, new PeProvisionerSimple(coreMips)));

				coreN = coreIter.nextNode();
			}
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return peList;
	}
	
	public List<Host> createHosts(String dcname){
		List<Host> hostList = new ArrayList<Host>();
		try {

			NodeIterator hostIter = XPathAPI.selectNodeIterator(doc, "Model/Datacenter[@name=\""+dcname+"\"]/Host");
			Node hostN = hostIter.nextNode(); 	
			while (hostN!=null){
				Element host = (Element)hostN;

				int hostId = Integer.valueOf(host.getAttribute("hostId"));
				int ram = Integer.valueOf(host.getAttribute("ram"));  //host memory (MB)
				long storage = Integer.valueOf(host.getAttribute("storage")); //host storage
				int bw = Integer.valueOf(host.getAttribute("bw"));

				List<Pe> peList = createCores(dcname, hostId);

				VmScheduler vmScheduler=
					host.getAttribute("vmScheduler").equalsIgnoreCase("TimeShared") ?
							new VmSchedulerTimeShared(peList) : new VmSchedulerSpaceShared(peList);

				hostList.add(
						new Host(hostId, new RamProvisionerSimple(ram),
								new BwProvisionerSimple(bw), storage, peList,
								vmScheduler)); 				

				hostN = hostIter.nextNode();
			}	
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return hostList;
	}

	@SuppressWarnings("unchecked")
	public List<Datacenter> createDatacenter(){
		List<Datacenter> datacenters = new ArrayList<Datacenter>();

		try {

			NodeIterator dcIter=XPathAPI.selectNodeIterator(doc, "Model/Datacenter");

			Node dcN = dcIter.nextNode(); 			
			while (dcN!=null){
				Element dc = (Element)dcN;

				String dcname = dc.getAttribute("name");
				String arch = dc.getAttribute("arch");      // system architecture
				String os = dc.getAttribute("os");          // operating system
				String vmm = dc.getAttribute("vmm");
				double time_zone = Double.valueOf(dc.getAttribute("time_zone"));         // time zone this resource located
				double cost = Double.valueOf(dc.getAttribute("cost"));              // the cost of using processing in this resource
				double costPerMem = Double.valueOf(dc.getAttribute("costPerMem"));		// the cost of using memory in this resource
				double costPerStorage = Double.valueOf(dc.getAttribute("costPerStorage"));	// the cost of using storage in this resource
				double costPerBw = Double.valueOf(dc.getAttribute("costPerBw"));			// the cost of using bw in this resource
				LinkedList storageList = new LinkedList();	//we are not adding SAN devices by now

				//hosts
				List<Host> hostList = createHosts(dcname);

				DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
						arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage, costPerBw);
				try {
					datacenters.add(
							//new Datacenter(dcname, characteristics, new VmAllocationPolicySimple(hostList), storageList, 0));
							new Datacenter(dcname, characteristics, new VmAllocationPolicyPreConfigured(hostList), storageList, 0));
				} catch (Exception e) {
					e.printStackTrace();
				}

				dcN = dcIter.nextNode();
			}
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return datacenters;
	}
	
	
	public List<VM_Spec> create_vms(String userId){

		List<VM_Spec> vm_sp = new ArrayList<VM_Spec>();
		try{
			NodeIterator vmIter	= XPathAPI.selectNodeIterator(doc
					, "Model/User[@userId=\""+userId+"\"]/VM");

			Node vmN = vmIter.nextNode();
			while (vmN!=null){
				Element vm = (Element)vmN;

				long size = Long.valueOf(vm.getAttribute("imageSize")); //image size (MB)
				int ram = Integer.valueOf(vm.getAttribute("ram")); //vm memory (MB)
				int mips = Integer.valueOf(vm.getAttribute("mips"));
				long bw = Long.valueOf(vm.getAttribute("bw"));
				int pesNumber = Integer.valueOf(vm.getAttribute("peNumber")); //number of cpus
				int priority = Integer.valueOf(vm.getAttribute("priority"));
				int preferedHostId = -1;
				if (vm.hasAttribute("preferedHostId")){
					preferedHostId = Integer.valueOf(vm.getAttribute("preferedHostId"));
				}
				
				String vmm = "Xen";

				CloudletScheduler cloudletScheduler=
					vm.getAttribute("cloudletScheduler").equalsIgnoreCase("TimeShared") ?
							new CloudletSchedulerTimeShared() : new CloudletSchedulerSpaceShared();

				vm_sp.add(new VM_Spec(size, ram, mips, bw, pesNumber,
						priority, vmm, cloudletScheduler, preferedHostId));

				vmN = vmIter.nextNode();
			} 		
		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("Unwanted errors happen");
		}
		return vm_sp;   

	}

	public List<BrokerStub> create_users(SimulationSpec spec){
		List<BrokerStub> brs = new ArrayList<BrokerStub>();	
		try{
		NodeIterator userIter	= XPathAPI.selectNodeIterator(doc
				, "Model/User");
		Node userN = userIter.nextNode();
		while (userN!=null){
			Element user = (Element)userN;
			String userId = user.getAttribute("userId"); //number of cpus
			
			// vms of user
			List<VM_Spec> vm_sp = create_vms(userId);
			
			// workload of user
			Element workload = (Element)XPathAPI.selectSingleNode(doc
					, "Model/User[@userId=\""+userId+"\"]/Workload");
			int meanDemand = Integer.valueOf(workload.getAttribute("meanDemand"));
			int meanInterArrival = Integer.valueOf(workload.getAttribute("meanInterArrival"));
			int arrivedNumLimit = Integer.valueOf(workload.getAttribute("arrivedNumLimit"));				
			
			
			BrokerStub broker = new BrokerStub(userId,meanDemand,meanInterArrival,arrivedNumLimit,vm_sp);
			broker.setSpec(spec);		
			brs.add(broker);
			userN = userIter.nextNode();				
		}
		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("Unwanted errors happen");
		}
		
		return brs;
	}


///////////////////////////////////// doc modifications //////////////////////////////////////////

	public void add_host(int host_id, int ram, int core_num, int mips){		
		try{			
			Element hostN;
			XPathAPI.selectSingleNode(doc, "Model/Datacenter[@name=\"Datacenter_0\"]")
								.appendChild(hostN = doc.createElement("Host")); 
			hostN.setAttribute("hostId", String.valueOf(host_id));
			
			// create host cores
			List<String> cores = new ArrayList<String>(); 
			for(int i=0; i< core_num; i++){
				Element coreN;
				hostN.appendChild(coreN = doc.createElement("Core"));
				coreN.setAttribute("id", String.valueOf(i));
				coreN.setAttribute("mips", String.valueOf(mips));
			}
		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("Unwanted errors happen");
		}		
	}

	public void add_application(String userId, int meanDemand, int meanInterArrival, int arrivedNumLimit){		
		try{			
			Element userN,workloadN;
			XPathAPI.selectSingleNode(doc, "Model")
								.appendChild(userN = doc.createElement("User")); 
			userN.setAttribute("userId", userId);
			
			// workload 
			userN.appendChild(workloadN = doc.createElement("Workload"));
			workloadN.setAttribute("meanDemand", String.valueOf(meanDemand));
			workloadN.setAttribute("meanInterArrival", String.valueOf(meanInterArrival));
			workloadN.setAttribute("arrivedNumLimit", String.valueOf(arrivedNumLimit));
		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("Unwanted errors happen");
		}		
	}

	public void add_VM_to_app(String userId, int imageSize, int ram, int mips, int bw,
		int priority, String cloudletScheduler, int peNumber, int preferedHostId)
	{		
		try{			
			Element vmN;
			// System.out.print("Model/User[@userId=\""+userId+"\"]");
			XPathAPI.selectSingleNode(doc, "Model/User[@userId=\""+userId+"\"]")
								.appendChild(vmN = doc.createElement("VM"));
			
			vmN.setAttribute("imageSize", String.valueOf(imageSize));
			vmN.setAttribute("ram", String.valueOf(ram));
			vmN.setAttribute("mips", String.valueOf(mips));
			vmN.setAttribute("bw", String.valueOf(bw));
			vmN.setAttribute("priority", String.valueOf(priority));
			vmN.setAttribute("cloudletScheduler", String.valueOf(cloudletScheduler));
			vmN.setAttribute("peNumber", String.valueOf(peNumber));
			vmN.setAttribute("preferedHostId", String.valueOf(preferedHostId));
			
		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("Unwanted errors happen");
		}		
	}
	
	public void set_vm_mips(String userId,int vm_id,int mips){
		try{
			// this.writeInputDocument("c:\\hi.xml");
			Node mipsN	= XPathAPI.selectSingleNode(doc
					, "Model/User[@userId=\""+userId+"\"]/VM[position()="+vm_id+"]/@mips");
			// System.out.println("userId:" + userId+ "; vm_id:" + vm_id+ "; mips:"+mips+"; mipsN:"+mipsN);
			mipsN.setNodeValue(String.valueOf(mips));
		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("Unwanted errors happen");
		}
		
	}

	public void set_user_interarr(String userId,int meanInterArrival){
		try{
			Node mipsN	= XPathAPI.selectSingleNode(doc
					, "Model/User[@userId=\""+userId+"\"]/Workload/@meanInterArrival");
			mipsN.setNodeValue(String.valueOf(meanInterArrival));
		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("Unwanted errors happen");
		}
		
	}

	public void set_user_demand(String userId,int meanDemand){
		try{
			Node mipsN	= XPathAPI.selectSingleNode(doc
					, "Model/User[@userId=\""+userId+"\"]/Workload/@meanDemand");
			mipsN.setNodeValue(String.valueOf(meanDemand));
		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("Unwanted errors happen");
		}
		
	}
	
//////////////////////////////////////////////////////////////////////////////////////////////////
	
	public Document parsePXL(String uri) {

		//Document doc = null;

		try {
			InputSource inputSource = new InputSource(new FileInputStream(uri));
			inputSource.setSystemId(uri);
			getParser().parse(inputSource);
			doc = getParser().getDocument();
			
//			getParser().parse(uri);
//			doc = getParser().getDocument();
			//System.out.print(doc);
		} catch (org.xml.sax.SAXException se) {
			se.printStackTrace(System.err);
			//throw new CSException(se.getMessage());
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}

		return doc;

	}
	
	public Map<BrokerStub,List<Result>> get_per_user_data(GlobalBroker globalBroker){
		Map<BrokerStub,List<Result>> result = new HashMap<BrokerStub,List<Result>>(); 		 
		for (BrokerStub brData:globalBroker.brokersData.values()){
			result.put(brData, brData.finalResult);
		}
		return result;
	}
	
	// Convert an array of strings to one string.
	// Put the 'separator' string between each element.
	public static String arrayToString(String[] a, String separator) {
	    StringBuffer result = new StringBuffer();
	    if (a.length > 0) {
	        result.append(a[0]);
	        for (int i=1; i<a.length; i++) {
	            result.append(separator);
	            result.append(a[i]);
	        }
	    }
	    return result.toString();
	}
	
	public Document createResultsDocument(Map<BrokerStub,List<Result>> result) {
		Document outDoc = new org.apache.xerces.dom.DocumentImpl();
		try {			
			Element root = outDoc.createElement("Results");
			outDoc.appendChild(root);
			for (BrokerStub user:result.keySet()){
				Element userN,responseTimeN;
				root.appendChild(userN = outDoc.createElement("User"));
				//userN.appendChild(responseTimeN = outDoc.createElement("ResponseTime"));
				List<String> rs = new ArrayList<String>(); 
				for(Result r: result.get(user)) rs.add(String.valueOf(r.duration));
		
				userN.setAttribute("userId", user.name);
				userN.setAttribute("ResponseTime", arrayToString(rs.toArray(new String[0]), ","));
			}
		} catch (Exception ex) {
			System.out.println("Error in writting:");
			ex.printStackTrace();
		}
		return outDoc;
	}

	public void writeResultsDocument(String outXmlFile, Document outDoc) {
		//				save the XML file
		try {

			File xmlFile = new File(outXmlFile);
			PrintStream xmlps = new PrintStream(new FileOutputStream(xmlFile));
			xmlps.close(); // this is a fix
			saveDOMTree(new FileOutputStream(xmlFile), outDoc);
		} catch (Exception e) {
			System.out.println(
				"An exception occured finding \n a configuration "
					+ e.toString());

		}
	}

	public void writeInputDocument(String outXmlFile) {
		//				save the XML file
		File xmlFile;
		try {

			xmlFile = new File(outXmlFile);
			//PrintStream xmlps = new PrintStream(new FileOutputStream(xmlFile));
			//xmlps.close(); // this is a fix
			saveDOMTree(new FileOutputStream(xmlFile), doc);
		} catch (Exception e) {
			System.out.println(
				"An exception occured finding \n a configuration "
					+ e.toString());

		}
	}
	
//	Host host = getVmAllocationPolicy().getHost(vmId, userId);
//    Vm vm = host.getVm(vmId, userId);
	
	public void setupExperiment(final int timeout){
		
		final Thread t2 = new Thread (new Runnable(){
			public void run(){
				//Document inputdoc = doc;
				// Log.setDisabled(false);
				Log.printLine("Starting CloudSimExample1...");
				try {
					int num_user = 1; // number of cloud users
					Calendar calendar = Calendar.getInstance();
					boolean trace_flag = false; // mean trace events

					// Initialize the CloudSim library
					CloudSim.init(num_user, calendar, trace_flag);

					// data center
					Datacenter datacenter0 = createDatacenter().get(0);

					// simulation spec
					int measurementCycle = Integer.valueOf(
							XPathAPI.selectSingleNode(doc, "Model/SimulationSpec/@measurementCycle").getNodeValue());
					SimulationSpec spec = new SimulationSpec(1267,measurementCycle);			

					//users
					GlobalBroker globalBroker = new GlobalBroker("GlobalBroker", create_users(spec));

					// start the simulation
					CloudSim.startSimulation();
					// Final step: Print results when simulation is over
					List<Cloudlet> newList = new ArrayList<Cloudlet>();
					for (String brname:globalBroker.brokerList.keySet()){
						newList.addAll(globalBroker.brokerList.get(brname).getCloudletReceivedList());
					}

					CloudSim.stopSimulation();

					Map<BrokerStub,List<Result>> result = get_per_user_data(globalBroker);
					
					//			for (Host host:datacenter0.getHostList()){
					//				for (Vm vm: host.getVmList()){
					//					System.out.println("host " + host.getId() +" has vm "+ vm.getId() + " for user "+ vm.getUserId());
					//				}
					//			}
								
					outDoc = createResultsDocument(result);
					if (outXmlFile!=null) writeResultsDocument(outXmlFile, outDoc);
					
					//printCloudletList(newList);
				
				//} catch (InterruptedException ex) {
					//Log.printLine("this step  was quited due to timeout");
				} catch (Exception e) {
					e.printStackTrace();
					Log.printLine("Unwanted errors happen");
				}

			}
		}, "simulation_thread");
		
		Thread timeout_thread = new Thread (new Runnable(){
			public void run(){
				try {
					Thread.currentThread().sleep(timeout);
					if (t2.isAlive()){
						Log.printLine("simulation step timeout");
						t2.stop();
					}
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}, "timeout");
		
		try{
			t2.start();	
			timeout_thread.start();		
			t2.join();			
		} catch (InterruptedException ex) {
			ex.printStackTrace();
		}
		
	}  
	
	public static void main(String[] args) {
		Tester model = new Tester();
		model.parsePXL("C:\\my\\project\\cloudsim\\mytest\\ceras\\simulation\\cloudsim\\topology\\2Hypers3App4VMsinput.xml");
		model.set_vm_mips("User1",1,280);
		
		model.setOutXmlFile("C:\\my\\project\\cloudsim\\mytest\\ceras\\simulation\\cloudsim\\topology\\2Hypers3App4VMsoutput.xml");
		
		model.setupExperiment(5000); // timeout of 5 secxonds
		SimulationClient cl = new SimulationClient(model.outDoc);
		
		
		System.out.println("rs:"+cl.getResponseTime("User1"));
	}


}
