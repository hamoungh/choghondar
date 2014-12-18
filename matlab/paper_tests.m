% running: paper_tests().compare_kalman_and_regression
classdef paper_tests
    methods
        
        % running: paper_tests().compare_kalman_and_regression()
        function compare_kalman_and_regression(obj)
            import ceras.simulation.cloudsim.*;
            cloudsim.Log.setDisabled(true);

            model = Tester;
            model.parsePXL('C:\\my\\project\\cloudsim\\mytest\\ceras\\simulation\\cloudsim\\topology\\2Hypers3App4VMsinput.xml');

            demand = 9000;
            vm_num = 1;
            interarr = 20;
            mipss = (300:5:1000);
            R=[];
            reg_model = App_model();
            tr_model = App_tracking_model();
            tr_lin_model = App_lin_tracking_model();

            compare = [];

            for mips=mipss
                model.set_vm_mips('User1',1,mips);
                model.setupExperiment();
                
                cl = SimulationClient(model.getOutDoc());
                real_r = cl.getResponseTime('User1');
                
                
                regress_r = reg_model.get_response_time(demand,interarr,mips); % model 1
                tr_r=tr_model.get_response_time(demand,interarr,mips);         % model 2     
                tr_lin_r=tr_lin_model.get_response_time(demand,interarr,mips);
               
                tr_model.update_(real_r, demand,interarr,mips);  %update model 2
                tr_lin_model.update_(real_r, demand,interarr,mips);  %update model 3
                
                compare =  [compare ; [real_r, regress_r, tr_r, tr_lin_r]];
            end
            plot( mipss,compare);
            h = legend('real_r','regressed_r','kalman_r','linear_tracked',4);
            set(h,'Interpreter','none')
            %end
        end

    end
end