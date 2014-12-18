classdef test_simple_deployment
    properties
    end
    methods
        function deploy_simple(obj,sim)
            sim.pms = 2;
            sim.total_capacity=ones(1,sim.pms)*1300; %in terms of mips

            %********* placement and resource allocation*****

            % app * pm  -> allocated_or_not
            sim.placement= ...
                [1 0
                1 1
                0 1];

            % initial point of capacity allocared in terms of model unit (here mips)
            % app * pm  -> allocated amount
            sim.initial_cap =  ...
                [280  0
                280  280
                0    280];

            sim.app_model = {
                App_tracking_model()
                App_tracking_model()
                App_tracking_model()};

%             sim.app_model = {
%                 App_model()
%                 App_model()
%                 App_model()};

            sim.apps = 3;

            sim.demand = [4100 5700 500];
            % sim.interarr = [22 22 22];
            sim.r_thresholds = [15 16 2];
            sim.max_utils = [20 10 5];
        end

        function test_sim1(obj)
            subplot_ = 0;
            sim = cloud_opt_simulation();
            sim.simulation_steps = 117; 
            steps = sim.simulation_steps;
            obj.deploy_simple(sim);      
            
            % import workload and devide it to applications (2h for each of
            % 3 apps) 
            load('.\dataset\workload_interarr_6h_min_final.mat', 'times','intarr', '-mat');   
            w(:,1)=intarr(1         :1*118)'.* 14;
            w(:,2)=intarr(118+1     :2*118)'.* 11;
            w(:,3)=intarr(2*118+1   :3*118)'.* 8;
           
            
%             w = [sim.sinus_workload2(22,44,10,1, steps), ...
%                 sim.sinus_workload2(22,44,10,2, steps),...
%                 sim.sinus_workload2(22,44,10,4, steps)];
            
            res = sim.simulate(w);
            if subplot_; handle=figure; end;
            color = {'r' 'b' 'g'}; % consequtively for apps
            
            %%%%%%%%%%%%% plot workloads %%%%%%%%%%%%%%
            if subplot_; subplot(3,2,1); else; figure; end;
            % plot(res.w)
            for ii=1:sim.apps
                plot(1./w(:,ii) , strcat(color{ii},'-'));
                hold on;
            end
            title('Workloads');
            xlabel('Time');
            ylabel('Arrival Rate(\lambda)');
            legend('app1','app2','app3',3);

            %%%%%%%%% plot the response times %%%%%%%%%%%
            if subplot_; subplot(3,2,3); else; figure; end;            
            for ii=1:sim.apps
                plot(1:steps , res.r(:,ii) , strcat(color{ii},'-'))
                plt =line([1 steps],[sim.r_thresholds(ii) sim.r_thresholds(ii)],'Color',color{ii},'LineStyle','-.');
                % 1:steps , sim.r_thresholds(ii)*ones(steps,1) , strcat(color{ii},'--'));
                % Exclude line from legend
                set(get(get(plt,'Annotation'),'LegendInformation'),'IconDisplayStyle','off');
                hold on
            end
            h = legend('app1','app2','app3',3);
            title(strcat('Applications Response Times' ));
            xlabel('Tiime');
            ylabel('Response Time(sec)');

            %%%%%%%%%%% plot gained utilities %%%%%%%%%%
            if subplot_; subplot(3,2,4); else; figure; end;            
            plot(res.fp,'r-')
            hold on
            plot(res.fp_sys)
            title('Gained Utilities');
            xlabel('Tiime');
            ylabel('Utility');
            h = legend('modeled util','measured util',2);

            %%%%%%%%%%% plot capacity given to each app per server %%%%%%%%%%%%%%
            for ii=1:sim.pms
                if subplot_; subplot(3,2,ii+4); else; figure; end;                            
                bhs = bar((squeeze(res.cap(:,ii,:)))','stacked');                                
                title(strcat('Server ', num2str(ii) ,' CPU Allocation' ));
                xlabel('Tiime');
                ylabel('Utility');

                % find out what apps are in this pm
                [i,j]=find( sim.placement==1);
                app_ids = i(j==ii);

                for app_=1:sim.apps
                    set(bhs(app_),'facecolor',color{app_}); 
                     set(bhs(app_),'EdgeColor',color{app_});
                end
%                
                app_names = {};
                for k=1:sim.apps
                    str = strcat('app',num2str(k));
                    app_names = {app_names{:}  str};
                end
                legend(app_names);               
            end

            % UtilityLib.print_figure(handle,9,7,'figures\case_study');

        end
    end
end