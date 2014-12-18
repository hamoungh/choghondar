% dataset format
% demand interarr mips responset 

import ceras.simulation.cloudsim.*;
cloudsim.Log.setDisabled(true);

model = Tester;
model.parsePXL('C:\\my\\project\\cloudsim\\mytest\\ceras\\simulation\\cloudsim\\topology\\input.xml');

demand_ = (2000:100:4100);
vm_num_ = 1;
interarr_ = (22:20:100);
mips_ = (280:10:1000);
R=[];

for demand=demand_
    model.set_user_demand('User1',demand)
    for interarr=interarr_ 
        model.set_user_interarr('User1',interarr);
        for mips=mips_
            model.set_vm_mips('User1',1,mips);
            model.setupExperiment();
            cl = SimulationClient(model.getOutDoc());
            R=[R ; demand interarr mips cl.getResponseTime('User1')];
        end
    end
end
% plot(R(:,3),R(:,4)) 

