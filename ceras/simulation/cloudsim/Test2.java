package ceras.simulation.cloudsim;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import mytest.GlobalBroker.BrokerStub;

import cloudsim.Cloudlet;
import cloudsim.CloudletSchedulerSpaceShared;
import cloudsim.CloudletSchedulerTimeShared;
import cloudsim.Datacenter;
import cloudsim.DatacenterBroker;
import cloudsim.DatacenterCharacteristics;
import cloudsim.Host;
import cloudsim.Log;
import cloudsim.Pe;
import cloudsim.Storage;
import cloudsim.UtilizationModel;
import cloudsim.UtilizationModelFull;
import cloudsim.Vm;
import cloudsim.VmAllocationPolicySimple;
import cloudsim.VmSchedulerTimeShared;
import cloudsim.core.CloudSim;
import cloudsim.core.CloudSimTags;
import cloudsim.core.SimEntity;
import cloudsim.core.SimEvent;
import cloudsim.provisioners.BwProvisionerSimple;
import cloudsim.provisioners.PeProvisionerSimple;
import cloudsim.provisioners.RamProvisionerSimple;
import cloudsim.distributions.*;


class Result {
	public Result(double duration) {
		super();
		this.duration = duration;
	}

	public double duration; 
}


 class GlobalBroker1 extends GlobalBroker {

	public GlobalBroker1(String name, Datacenter dc, SimulationSpec spec) {
		super(name, dc, spec);
		// TODO Auto-generated constructor stub
	}

	public GlobalBroker1(String name, Datacenter dc) {
		//super(ariivedNumLimit, ariivedNumLimit, ariivedNumLimit, "");
		super( name, dc);
	}

	@Override
	public void startEntity() {	
		super.startEntity();
	}

	

	public void when_cloudlet_arrival(Cloudlet cloudlet, DatacenterBroker broker){
		BrokerStub brokerData = brokersData.get(broker.getName());
		
		// update measures based on the executed cloudlet
		//List<Cloudlet> cls = broker.getCloudletReceivedList();
		if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS){
			cloudlet.getResourceId();
			cloudlet.getVmId(); 
			cloudlet.getActualCPUTime();
			cloudlet.getExecStartTime();
			cloudlet.getFinishTime();

			brokerData.updateMeasures(cloudlet.getFinishTime()-cloudlet.getSubmissionTime());
			
		}
		
		// update vm share
		
	}
}

 class VM_Spec {
	public static int vmIdShift=1;
		
	//VM Parameters
	long size = 10000; //image size (MB)
	int ram = 512; //vm memory (MB)
	int mips = 400;
	long bw = 1000;
	int pesNumber = 1; //number of cpus
	int priority = 1;
	String vmm = "Xen"; //VMM name
	
	 public VM_Spec(int mips) {
		 //take default for the rest
		 this.mips = mips;
	 }
	 
	 public VM_Spec(long size, int ram, int mips, long bw, int pesNumber,
			int priority, String vmm) {
		super();
		this.size = size;
		this.ram = ram;
		this.mips = mips;
		this.bw = bw;
		this.pesNumber = pesNumber;
		this.priority = priority;
		this.vmm = vmm;
	}

	public Vm createVM(int userId) {
		//Creates a container to store VMs. This list is passed to the broker later
		//create VM
		Vm vm = new Vm(vmIdShift++, userId, mips, pesNumber, ram, bw, size, priority, vmm, new CloudletSchedulerSpaceShared());
		return vm;
	}

 }
	
 class Host_Spec {
	 
 }
 
 
 class SimulationSpec {		
	
	 int tuning_cycle = 1267;
	 int measurement_cycle = 60;
	 List<BrokerStub> brokers ;

	 public SimulationSpec(){
		 super();
	 }
			 
	 public SimulationSpec(List<BrokerStub> brokers) {
		 super();
		 this.brokers = brokers;
	 }

	 public SimulationSpec(int tuningCycle, int measurementCycle) {
		 super();
		 tuning_cycle = tuningCycle;
		 measurement_cycle = measurementCycle;
	 }
	 
	 public void setBrokers(List<BrokerStub> brokers) {
			this.brokers = brokers;
		}

 }

 abstract class GlobalBroker extends cloudsim.core.SimEntity {

	 SimulationSpec spec;

	//////////////////////////
	Datacenter dc;
	
	protected static final int CREATE_BROKER = 0;
	protected static final int CREATE_VM = 1;
	protected static final int CLOUDLET_ARRIVAL = 2;
	protected static final int UPDATE_VM_SHARE = 3;

	public Map<String,DatacenterBroker> brokerList = new HashMap<String,DatacenterBroker>();
	public Map<String,BrokerStub> brokersData = new HashMap<String,BrokerStub>();
	
	//////////////////////////
	
	public GlobalBroker(String name, Datacenter dc) {			
		super(name);
		this.dc = dc;
		//this.numOfVMs = numOfVMs;
	}
	
	public GlobalBroker(String name, Datacenter dc, SimulationSpec spec) {			
		super(name);
		this.spec = spec;
		this.dc = dc;
		//this.numOfVMs = numOfVMs;
	}

	public abstract void when_cloudlet_arrival(Cloudlet cloudlet,DatacenterBroker broker);

	public double get_next_interarrival(DatacenterBroker broker){
		double interarrival = brokersData.get(broker.getName()).interarrivalDistr.sample();
		//System.out.println("interarrival:"+ interarrival);
		return interarrival;
	}
	public double get_next_demand(DatacenterBroker broker){
		double demand = brokersData.get(broker.getName()).demandDistr.mean;//.sample();
		//System.out.println("demand:"+demand);
		return demand;
	}

	private Vm getAFairVMforCloudlet(String broker_name){		
		BrokerStub broker = brokersData.get(broker_name);
		List<Vm> myVms = broker.myVms;
		return myVms.get(broker.curVm++ % myVms.size());
	}
	
	@Override
	public void processEvent(SimEvent ev) {
		final DatacenterBroker broker;
		String broker_name;
		
		switch (ev.getTag()) {
		
		case CREATE_BROKER:
			BrokerStub brs = (BrokerStub)ev.getData();
			String brokerName = brs.name;
			brokersData.put(brokerName, brs);
									
			broker = createBroker(brokerName); //we put the name on data			
			brokerList.put(brokerName, broker);
			Subscriber subscriber = new Subscriber(){
				public void notifyCloudletReturn(Cloudlet cloudlet) {
					when_cloudlet_arrival(cloudlet, broker);
				}
			};	
			broker.subscribe(subscriber);
			break;

		case CREATE_VM:		
			// ev.getData() is boker name as String
			Object[] data = (Object[])ev.getData();
			broker_name= (String)data[0];
			VM_Spec vm_spec= (VM_Spec)data[1];
			broker = brokerList.get(broker_name);
		
			//Create VMs and send them to broker
			Vm vm=vm_spec.createVM(broker.getId());
			//vm.setHost(dc.getHostList().get(what_deployment_says))
			brokersData.get(broker_name).myVms.add(vm); //this is used for fair distributer
			broker.submitVmList(new ArrayList<Vm>(Arrays.asList(vm)));  //creating 1 vm
			 scheduleNow(broker.getId(), CloudSimTags.CREATE_VMS);
			break;
		
		case CLOUDLET_ARRIVAL:			
			broker_name= (String)ev.getData();
			broker = brokerList.get(broker_name);
			BrokerStub broker_data = brokersData.get(broker.getName());
			// creating 1 cloudlet and submit
			Cloudlet cl=createCloudlet(broker.getId(), (long)get_next_demand(broker));
			cl.setVmId(getAFairVMforCloudlet(broker_name).getId());
			broker.submitCloudletList(new ArrayList<Cloudlet>(Arrays.asList(cl)));
			
			//schedule next cloudlet arrival
			if (++broker_data.arrivedNum<broker_data.ariivedNumLimit) 
				schedule(getId(), (long)get_next_interarrival(broker), CLOUDLET_ARRIVAL, ev.getData()); //copy the broker name for
			break;
		
		case UPDATE_VM_SHARE:
			Map<Vm,Integer> shares=  get_new_vm_shares();
			for (Vm vm_ : shares.keySet()){
				Double[] args={400.0};
				vm_.getCloudletScheduler().setCurrentMipsShare(Arrays.asList(args));
				//vm_.setMips(shares.get(vm_));
			}
			
			//schedule(getId(), tuning_cycle, UPDATE_VM_SHARE );
			break;
		
		default:
			Log.printLine(getName() + ": unknown event type");
			break;
		}
	}
	
	// redo the experiment every time
	
	public Map<Vm,Integer> get_new_vm_shares(){
		Map<Vm,Integer> vm_shares = new HashMap<Vm,Integer>(); 
		for (BrokerStub broker_data:brokersData.values()){
			String br_name = broker_data.name;
			if (br_name.equalsIgnoreCase("Broker_1"))
				vm_shares.put(broker_data.myVms.get(0), 400);

			else if (br_name.equalsIgnoreCase("Broker_2")){
				vm_shares.put(broker_data.myVms.get(0), 300);
				vm_shares.put(broker_data.myVms.get(1), 300);				
			}

			else if (br_name.equalsIgnoreCase("Broker_3"))
				vm_shares.put(broker_data.myVms.get(0), 200);	

		}
		return vm_shares;
	}
	
	@Override
	public void startEntity() {
		Log.printLine("GlobalBroker is starting...");
		
		int t=10;
		for (BrokerStub br:spec.brokers){
			schedule(getId(), t++, CREATE_BROKER, br);
			for (VM_Spec vm_sp:br.vm_sp){
				Object[] param = {br.name , vm_sp};
				schedule(getId(), t++, CREATE_VM, param);
			}
			// schedule first arrival
			schedule(getId(), t++, CLOUDLET_ARRIVAL, br.name);
		}

		//schedule(getId(), 13, UPDATE_VM_SHARE);
	}

	
	public static int cloudletIdShift=1;
	private static Cloudlet createCloudlet(int userId,long length){
		// Creates a container to store Cloudlets
		LinkedList<Cloudlet> list = new LinkedList<Cloudlet>();

		//cloudlet parameters
		//long length = 40000;
		long fileSize = 300;
		long outputSize = 300;
		int pesNumber = 1;
		UtilizationModel utilizationModel = new UtilizationModelFull();

		Cloudlet cloudlet = new Cloudlet(cloudletIdShift++, length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
		// setting the owner of these Cloudlets
		cloudlet.setUserId(userId);

		return cloudlet;
	}
	

	//We strongly encourage users to develop their own broker policies, to submit vms and cloudlets according
	//to the specific rules of the simulated scenario
	private static DatacenterBroker createBroker(String name){

		DatacenterBroker broker = null;
		try {
			broker = new DatacenterBroker(name);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return broker;
	}


	@Override
	public void shutdownEntity() {
	}

	static class BrokerStub{
		
		String name;
		int meanDemand; 
		int meanInterArrival; 
		int ariivedNumLimit;
		int arrivedNum=0;
		ExponentialDistr demandDistr; 
		ExponentialDistr interarrivalDistr;
		List<Vm> myVms = new ArrayList<Vm>();
		int curVm=0;
		ArrayList<Result> stepResult=new ArrayList<Result>();
		ArrayList<Result> finalResult=new ArrayList<Result>();
		List<VM_Spec> vm_sp;
		SimulationSpec spec;
		
		public BrokerStub(String name, int meanDemand, int meanInterArrival,
				int ariivedNumLimit, List<VM_Spec> vm_sp) {
			super();
			this.name = name;
			this.meanDemand = meanDemand;
			this.meanInterArrival = meanInterArrival;
			this.ariivedNumLimit = ariivedNumLimit;
			demandDistr = new ExponentialDistr(meanDemand);
			interarrivalDistr = new ExponentialDistr(meanInterArrival);
			
			this.vm_sp = vm_sp;
		}
		
		public void setSpec(SimulationSpec spec) {
			this.spec = spec;
		}
		
		public void updateMeasures(double duration){
			stepResult.add(new Result(duration));
			//System.out.println(stepResult.size()+","+finalResult.size());
			if (stepResult.size() == spec.measurement_cycle ){
				double sum=0; for (Result r:stepResult) sum+=r.duration;
				
				finalResult.add(new Result(sum/spec.measurement_cycle));
				stepResult=new ArrayList<Result>();
			}			
		}
	}
	

//	public DatacenterBroker getBroker() {
//		return broker;
//	}

}

/**
 * A simple example showing how to create a datacenter with one host and run one
 * cloudlet on it.
 */
public class Test2 {
	

	/**
	 * Prints the Cloudlet objects
	 * @param list  list of Cloudlets
	 */
	private static void printCloudletList(List<Cloudlet> list) {
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

	@SuppressWarnings("unchecked")
	public static Datacenter createDatacenter(String name){

		// Here are the steps needed to create a PowerDatacenter:
		// 1. We need to create a list to store one or more
		//    Machines
		List<Host> hostList = new ArrayList<Host>();

		// 2. A Machine contains one or more PEs or CPUs/Cores. Therefore, should
		//    create a list to store these PEs before creating
		//    a Machine.
		List<Pe> peList1 = new ArrayList<Pe>();

		int mips = 1000;

		// 3. Create PEs and add these into the list.
		//for a quad-core machine, a list of 4 PEs is required:
		peList1.add(new Pe(0, new PeProvisionerSimple(mips))); // need to store Pe id and MIPS Rating
		//peList1.add(new Pe(1, new PeProvisionerSimple(mips)));
		//peList1.add(new Pe(2, new PeProvisionerSimple(mips)));
		//peList1.add(new Pe(3, new PeProvisionerSimple(mips)));

		//Another list, for a dual-core machine
		List<Pe> peList2 = new ArrayList<Pe>();

		peList2.add(new Pe(0, new PeProvisionerSimple(mips)));
		//peList2.add(new Pe(1, new PeProvisionerSimple(mips)));

		//4. Create Hosts with its id and list of PEs and add them to the list of machines
		int hostId=0;
		int ram = 1500; //16384; //host memory (MB)
		long storage = 1000000; //host storage
		int bw = 10000;

		hostList.add(
    			new Host(
    				hostId,
    				new RamProvisionerSimple(ram),
    				new BwProvisionerSimple(bw),
    				storage,
    				peList1,
    				new VmSchedulerTimeShared(peList1)
    			)
    		); // This is our first machine

		hostId++;

		hostList.add(
    			new Host(
    				hostId,
    				new RamProvisionerSimple(ram),
    				new BwProvisionerSimple(bw),
    				storage,
    				peList2,
    				new VmSchedulerTimeShared(peList2)
    			)
    		); // Second machine

		// 5. Create a DatacenterCharacteristics object that stores the
		//    properties of a data center: architecture, OS, list of
		//    Machines, allocation policy: time- or space-shared, time zone
		//    and its price (G$/Pe time unit).
		String arch = "x86";      // system architecture
		String os = "Linux";          // operating system
		String vmm = "Xen";
		double time_zone = 10.0;         // time zone this resource located
		double cost = 3.0;              // the cost of using processing in this resource
		double costPerMem = 0.05;		// the cost of using memory in this resource
		double costPerStorage = 0.1;	// the cost of using storage in this resource
		double costPerBw = 0.1;			// the cost of using bw in this resource
		LinkedList storageList = new LinkedList();	//we are not adding SAN devices by now

		DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage, costPerBw);


		// 6. Finally, we need to create a PowerDatacenter object.
		Datacenter datacenter = null;
		try {
			datacenter = new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), storageList, 0);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return datacenter;
	}
	
	public static void main1(String[] args) {
		Log.setDisabled(true);
		System.out.println(
				"demand\t"+
				"vm_num\t"+
				"mips_per_vm\t"+
				"arrival_rate)\t"+
				"dur\t");
		
		try {
			// First step: Initialize the CloudSim package. It should be called
			// before creating any entities.
			int num_user = 1; // number of cloud users
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false; // mean trace events

			int demand = 4100;
			int vm_num = 1;
			//int mips_per_vm  = 250;
			//double arrival_rate = 0.001;
			int interarr = 22;
			for (int mips_per_vm = 280; mips_per_vm < 1000; mips_per_vm = mips_per_vm+10){
				//interarr = (int)(1 / arrival_rate); 
				// Initialize the CloudSim library
				CloudSim.init(num_user, calendar, trace_flag);
	
				Datacenter datacenter0 = createDatacenter("Datacenter_0");
	
				//////////////////////////////////////
			
				SimulationSpec spec = new SimulationSpec(1267,60);			
				VM_Spec[] vm_sp = {new VM_Spec(mips_per_vm)};
				BrokerStub broker = new BrokerStub("Broker_1",demand,interarr,60,Arrays.asList(vm_sp));
				broker.setSpec(spec);			
				BrokerStub[] brs = {broker};		
				spec.setBrokers(Arrays.asList(brs));

				GlobalBroker1 globalBroker = new GlobalBroker1("GlobalBroker", datacenter0, spec);

				CloudSim.startSimulation();

				CloudSim.stopSimulation();

				for (BrokerStub brData:globalBroker.brokersData.values()){
					String dur="";
					for (Result r:brData.finalResult) {
						dur+= r.duration;				
					}
					System.out.println(
						//	demand+"\t"+
						//	vm_num+"\t"+
						//	mips_per_vm+"\t"+
						//	Math.round((1/interarr)*1000)+"\t"+
							dur+"\t");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("Unwanted errors happen");
		}
	}


	public static void main(String[] args) {
		// Log.setDisabled(true);
		Log.printLine("Starting CloudSimExample1...");
		try {
			// First step: Initialize the CloudSim package. It should be called
			// before creating any entities.
			int num_user = 1; // number of cloud users
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false; // mean trace events

			// Initialize the CloudSim library
			CloudSim.init(num_user, calendar, trace_flag);
			
			Datacenter datacenter0 = createDatacenter("Datacenter_0");

			//////////////////////////////////////
			//interarrival <<
			
			SimulationSpec spec = new SimulationSpec(1267,1000);			
			VM_Spec[] vm_sp = {new VM_Spec(1000),new VM_Spec(1000),new VM_Spec(1000),new VM_Spec(1000)};
			BrokerStub broker = new BrokerStub("Broker_1",4100,5,10,Arrays.asList(vm_sp));
			broker.setSpec(spec);			
			BrokerStub[] brs = {broker};		
			
			spec.setBrokers(Arrays.asList(brs));
			//////////////////////
			
			GlobalBroker1 globalBroker = new GlobalBroker1("GlobalBroker", datacenter0, spec);
			
			// Sixth step: Starts the simulation
			CloudSim.startSimulation();
			// Final step: Print results when simulation is over
			List<Cloudlet> newList = new ArrayList<Cloudlet>();
			for (String brname:globalBroker.brokerList.keySet()){
				newList.addAll(globalBroker.brokerList.get(brname).getCloudletReceivedList());
			}
			//Log.print("CloudSim clock: "+CloudSim.clock());
			CloudSim.stopSimulation();
			
			
			
			for (BrokerStub brData:globalBroker.brokersData.values()){
				String msg="";
				System.out.print("[");
				for (Result r:brData.finalResult) {
					System.out.printf("%6.2f,",r.duration);
					//msg+="," +r.duration;				
				}
				System.out.print("]\n");
			}
			
//			for (Host host:datacenter0.getHostList()){
//				for (Vm vm: host.getVmList()){
//					System.out.println("host " + host.getId() +" has vm "+ vm.getId() + " for user "+ vm.getUserId());
//				}
//			}
			
			printCloudletList(newList);
			
		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("Unwanted errors happen");
		}
	}


}

//create host a and vms by urself and send bunch of requests with them
//take the average or even instantanous for each task and draw