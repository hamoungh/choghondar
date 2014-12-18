package ceras.simulation.cloudsim;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import cloudsim.Cloudlet;
import cloudsim.Datacenter;
import cloudsim.Log;
import cloudsim.core.CloudSim;

import com.sun.org.apache.xpath.internal.XPathAPI;

public class TestThread {
	static int i = 0;
	
	public static void main(String[] args){
		System.out.println("started");   
		final int  timeout = 2000; // 20 sec
	
		
		final Thread t2 = new Thread (new Runnable(){
			public void run(){				
				try{					
					while(true){
						i = (i + 1) % 65000; 						
					}					

					//				} catch (InterruptedException ex) {
					//					Log.printLine("this step  was quited due to timeout");
				} catch (Exception e) {
					// System.out.print("after loop:"+i);
					e.printStackTrace();
				}

			}
		}, "simulation_thread");

		Thread timeout_thread = new Thread (new Runnable(){
			public void run(){
				try {
					System.out.println("slept");
					Thread.currentThread().sleep(timeout);
					System.out.println("wokeup");
					// t2.interrupt();
					 System.out.print("after loop: i is "+i+" t2.isAlive() is "+ t2.isAlive());
					t2.stop();
					// System.exit(0);
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
		
		System.out.println("main is done");
	}
}
