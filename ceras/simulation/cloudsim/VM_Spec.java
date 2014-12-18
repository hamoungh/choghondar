package ceras.simulation.cloudsim;

import cloudsim.CloudletScheduler;
import cloudsim.Vm;

public class VM_Spec {
	public static int vmIdShift=1;
		
	//VM Parameters
	long size = 10000; //image size (MB)
	int ram = 512; //vm memory (MB)
	int mips = 400;
	long bw = 1000;
	int pesNumber = 1; //number of cpus
	int priority = 1;
	String vmm = "Xen"; //VMM name
	CloudletScheduler cloudletScheduler;
	int preferedhost;
	
	 public VM_Spec(int mips) {
		 //take default for the rest
		 this.mips = mips;
	 }
	 
	 public VM_Spec(long size, int ram, int mips, long bw, int pesNumber,
			int priority, String vmm, CloudletScheduler cloudletScheduler, int preferedhost) {
		super();
		this.size = size;
		this.ram = ram;
		this.mips = mips;
		this.bw = bw;
		this.pesNumber = pesNumber;
		this.priority = priority;
		this.vmm = vmm; 
		this.cloudletScheduler = cloudletScheduler;
		this.preferedhost = preferedhost;
	}
	 
	 
	 public Vm createVM(int userId) {
			//Creates a container to store VMs. This list is passed to the broker later
			//create VM
		 Vm vm;
		 if (this.preferedhost==-1)
			vm = new Vm(vmIdShift++, userId, mips, pesNumber, ram, bw, size, priority, vmm, cloudletScheduler);
		 else
			 vm = new VM_PreDefinedHost(vmIdShift++, userId, mips, pesNumber, ram, bw, size, priority, vmm, cloudletScheduler, preferedhost); 
		
		 return vm;
		}
}