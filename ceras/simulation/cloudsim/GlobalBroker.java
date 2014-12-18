package ceras.simulation.cloudsim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import cloudsim.Cloudlet;
import cloudsim.Datacenter;
import cloudsim.DatacenterBroker;
import cloudsim.Log;
import cloudsim.UtilizationModel;
import cloudsim.UtilizationModelFull;
import cloudsim.Vm;
import cloudsim.core.CloudSimTags;
import cloudsim.core.SimEvent;

public class GlobalBroker extends cloudsim.core.SimEntity {

	List<BrokerStub> brokers;

	// ////////////////////////

	protected static final int CREATE_BROKER = 0;
	protected static final int CREATE_VM = 1;
	protected static final int CLOUDLET_ARRIVAL = 2;
	protected static final int UPDATE_VM_SHARE = 3;

	public Map<String, DatacenterBroker> brokerList = new HashMap<String, DatacenterBroker>();
	public Map<String, BrokerStub> brokersData = new HashMap<String, BrokerStub>();

	// ////////////////////////

	public GlobalBroker(String name, Datacenter dc) {
		super(name);
	}

	public GlobalBroker(String name, List<BrokerStub> spec) {
		super(name);
		this.brokers = spec;
	}

	public void when_cloudlet_arrival(Cloudlet cloudlet, DatacenterBroker broker) {
		BrokerStub brokerData = brokersData.get(broker.getName());

		// update measures based on the executed cloudlet
		// List<Cloudlet> cls = broker.getCloudletReceivedList();
		if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
			cloudlet.getResourceId();
			cloudlet.getVmId();
			cloudlet.getActualCPUTime();
			cloudlet.getExecStartTime();
			cloudlet.getFinishTime();

			brokerData.updateMeasures(cloudlet.getFinishTime()
					- cloudlet.getSubmissionTime());

		}

		// update vm share

	}

	public double get_next_interarrival(DatacenterBroker broker) {
		double interarrival = brokersData.get(broker.getName()).interarrivalDistr
				.sample();
		// System.out.println("interarrival:"+ interarrival);
		return interarrival;
	}

	public double get_next_demand(DatacenterBroker broker) {
		double demand = brokersData.get(broker.getName()).demandDistr.mean;// .sample();
		// System.out.println("demand:"+demand);
		return demand;
	}

	private Vm getAFairVMforCloudlet(String broker_name) {
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
			BrokerStub brs = (BrokerStub) ev.getData();
			String brokerName = brs.name;
			brokersData.put(brokerName, brs);

			broker = createBroker(brokerName); // we put the name on data
			brokerList.put(brokerName, broker);
			Subscriber subscriber = new Subscriber() {
				public void notifyCloudletReturn(Cloudlet cloudlet) {
					when_cloudlet_arrival(cloudlet, broker);
				}
			};
			broker.subscribe(subscriber);
			break;

		case CREATE_VM:
			// ev.getData() is boker name as String
			Object[] data = (Object[]) ev.getData();
			broker_name = (String) data[0];
			VM_Spec vm_spec = (VM_Spec) data[1];
			broker = brokerList.get(broker_name);

			// Create VMs and send them to broker
			Vm vm = vm_spec.createVM(broker.getId());
			// vm.setHost(dc.getHostList().get(what_deployment_says))
			brokersData.get(broker_name).myVms.add(vm); // this is used for fair
			// distributer
			broker.submitVmList(new ArrayList<Vm>(Arrays.asList(vm))); // creating
			// 1 vm
			scheduleNow(broker.getId(), CloudSimTags.CREATE_VMS);
			break;

		case CLOUDLET_ARRIVAL:
			broker_name = (String) ev.getData();
			broker = brokerList.get(broker_name);
			BrokerStub broker_data = brokersData.get(broker.getName());
			// creating 1 cloudlet and submit
			Cloudlet cl = createCloudlet(broker.getId(),
					(long) get_next_demand(broker));
			cl.setVmId(getAFairVMforCloudlet(broker_name).getId());
			broker
					.submitCloudletList(new ArrayList<Cloudlet>(Arrays
							.asList(cl)));

			// schedule next cloudlet arrival
			if (++broker_data.arrivedNum < broker_data.ariivedNumLimit)
				schedule(getId(), (long) get_next_interarrival(broker),
						CLOUDLET_ARRIVAL, ev.getData()); // copy the broker name
			// for
			break;

		default:
			Log.printLine(getName() + ": unknown event type");
			break;
		}
	}

	@Override
	public void startEntity() {
		Log.printLine("GlobalBroker is starting...");

		int t = 10;
		for (BrokerStub br : brokers) {
			schedule(getId(), t++, CREATE_BROKER, br);
			for (VM_Spec vm_sp : br.vm_sp) {
				Object[] param = { br.name, vm_sp };
				schedule(getId(), t++, CREATE_VM, param);
			}
			// schedule first arrival
			schedule(getId(), t++, CLOUDLET_ARRIVAL, br.name);
		}

		// schedule(getId(), 13, UPDATE_VM_SHARE);
	}

	public static int cloudletIdShift = 1;

	private static Cloudlet createCloudlet(int userId, long length) {
		// Creates a container to store Cloudlets
		LinkedList<Cloudlet> list = new LinkedList<Cloudlet>();

		// cloudlet parameters
		// long length = 40000;
		long fileSize = 0;//300;
		long outputSize = 0;//300;
		int pesNumber = 1;
		UtilizationModel utilizationModel = new UtilizationModelFull();

		Cloudlet cloudlet = new Cloudlet(cloudletIdShift++, length, pesNumber,
				fileSize, outputSize, utilizationModel, utilizationModel,
				utilizationModel);
		// setting the owner of these Cloudlets
		cloudlet.setUserId(userId);

		return cloudlet;
	}

	// We strongly encourage users to develop their own broker policies, to
	// submit vms and cloudlets according
	// to the specific rules of the simulated scenario
	private static DatacenterBroker createBroker(String name) {

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

}
