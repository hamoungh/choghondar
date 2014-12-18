package ceras.simulation.cloudsim;

import cloudsim.*;

public class VM_PreDefinedHost extends Vm{

	public int desiredHostId;
	
	public VM_PreDefinedHost(int id, int userId, double mips, int pesNumber,
			int ram, long bw, long size, int priority, String vmm,
			CloudletScheduler cloudletScheduler, int desiredHostId) {
		super(id, userId, mips, pesNumber, ram, bw, size, priority, vmm,
				cloudletScheduler);
		this.desiredHostId = desiredHostId;
		// TODO Auto-generated constructor stub
	}

}
