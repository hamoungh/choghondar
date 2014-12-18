package ceras.simulation.cloudsim;

import java.util.List;

public class SimulationSpec {		
	
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
