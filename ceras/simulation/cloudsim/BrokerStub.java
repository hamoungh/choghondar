package ceras.simulation.cloudsim;

import java.util.ArrayList;
import java.util.List;

import cloudsim.Vm;
import cloudsim.distributions.ExponentialDistr;

public class BrokerStub{
	
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
