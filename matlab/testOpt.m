      
import ceras.simulation.cloudsim.*;
cloudsim.Log.setDisabled(true);

model = Tester;
model.parsePXL('C:\\my\\project\\cloudsim\\mytest\\ceras\\simulation\\cloudsim\\topology\\2Hypers3App4VMsinput.xml');
% model.setupExperiment();
% cl = SimulationClient(model.getOutDoc());
% cl.getResponseTime('User1')
% rss = hashmap2struct(cl.getResponseTime())

demand = 4100;
vm_num = 1;
interarr = 22;
mipss = (280:10:1000);
R=[];
for mips=mipss
    model.set_mips('User1',1,mips);
    model.setupExperiment();
    cl = SimulationClient(model.getOutDoc());
    R=[R cl.getResponseTime('User1')];
end
plot(mipss,R)