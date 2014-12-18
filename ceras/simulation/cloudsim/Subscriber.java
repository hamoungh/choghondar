package ceras.simulation.cloudsim;

import cloudsim.Cloudlet;

public abstract class Subscriber {
	public abstract void notifyCloudletReturn(Cloudlet cloudlet);
}
