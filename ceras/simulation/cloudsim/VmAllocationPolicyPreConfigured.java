package ceras.simulation.cloudsim;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cloudsim.Host;
import cloudsim.Vm;
import cloudsim.VmAllocationPolicy;
import cloudsim.VmAllocationPolicySimple;

public class VmAllocationPolicyPreConfigured extends VmAllocationPolicySimple {

	public VmAllocationPolicyPreConfigured(List<? extends Host> list) {
		super(list);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Allocates a host for a given VM.
	 *
	 * @param vm VM specification
	 *
	 * @return $true if the host could be allocated; $false otherwise
	 *
	 * @pre $none
	 * @post $none
	 */
	@Override
	public boolean allocateHostForVm(Vm vm) {
		int requiredPEs = vm.getPesNumber();
		boolean result = false;
		int tries = 0;
		int[] freePEsTemp = getFreePes().clone();

		if (!getVmTable().containsKey(vm.getUid())) { //if this vm was not created
			// I added this, to check if the vm wants to be on a specific host
			if (vm instanceof VM_PreDefinedHost && ((VM_PreDefinedHost)vm).desiredHostId != -1){
				for (Host h:getHostList()) {
					if (h.getId() == ((VM_PreDefinedHost)vm).desiredHostId){
						result = h.vmCreate(vm);
						if (result) { //if vm were succesfully created in the host
							getVmTable().put(vm.getUid(), h);
							
						}
					}
				}
			}
			if (result == false) {
				do {//we still trying until we find a host or untill we try all of them
					int moreFree = Integer.MIN_VALUE;
					int idx = -1;

					//we want the host with less pes in use
					for (int i=0; i < freePEsTemp.length; i++) {
						if (freePEsTemp[i] > moreFree) {
							moreFree = freePEsTemp[i];
							idx = i;
						}
					}

					Host host = getHostList().get(idx);
					result = host.vmCreate(vm);

					if (result) { //if vm were succesfully created in the host
						//Log.printLine("VmAllocationPolicy: VM #"+vm.getVmId()+ "Chosen host: #"+host.getMachineID()+" idx:"+idx);
						getVmTable().put(vm.getUid(), host);
						getUsedPes().put(vm.getUid(), requiredPEs);
						getFreePes()[idx] -= requiredPEs;
						result = true;
						break;
					} else {
						freePEsTemp[idx] = Integer.MIN_VALUE;
					}
					tries++;
				} while (!result && tries < getFreePes().length);
			}

		}

		return result;
	}

}
