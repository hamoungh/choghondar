% movie(M,1,1);
function [s,M,sim] = test_single_workload_scaleup
       apps= 10;
       pms = 7;
%      apps= 2;
%      pms = 1;
    app_per_pm = 3;
    
    sim = cloud_opt_simulation();
    sim.simulation_steps = 7;
    steps = 1:sim.simulation_steps;

    w = ones(apps,sim.simulation_steps)*40;    
    w(1,:) = 40 - (((40-7) .* steps)/sim.simulation_steps );
    w=w';
    
    sim.initialize_deployment(apps,pms,app_per_pm);               
    s = sim.simulate(w);
    M = sim.animate_cap(s,600,1);
end





