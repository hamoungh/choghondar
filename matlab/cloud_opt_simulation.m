classdef cloud_opt_simulation < handle
    properties
        pms;
        total_capacity;
        % app * pm  -> allocated_or_not
        placement;

        % initial point of capacity allocared in terms of model unit (here mips)
        % app * pm  -> allocated amount
        initial_cap;

        % cell-array of application models
        app_model;
        model=[];
        %*************** PaaS level stuff utility model *******************
        apps;
        demand;
        interarr;

        %*************** utility function definitions *****************
        r_thresholds;
        max_utils;

        %simulation steps
        simulation_steps = 20;

        %**** optimizer iterations per control interval****
        MAX_ITERS = 300;
        step_timout = 5000;
    end

    methods

        function  obj=cloud_opt_simulation() %simulation_steps)
            import ceras.simulation.cloudsim.*;
            cloudsim.Log().setDisabled(true);

            obj.model = Tester;
            obj.model.parsePXL('C:\\my\\project\\cloudsim\\mytest\\ceras\\simulation\\cloudsim\\topology\\2Hypers3App4VMsinput.xml');
        end

        function u=utility_func(obj,r,threshold,max_util)
            u = max_util*(atan(-(r-threshold))+pi/2)/pi;
        end

        function [global_util]=calculate_global_util(obj,r)
            global_util = 0;
            for ii=1:obj.apps
                local_util = obj.utility_func(...
                    r(ii),...
                    obj.r_thresholds(ii),...
                    obj.max_utils(ii));

                global_util = global_util + local_util;
            end
        end

        function [r]=compute_r(obj,cap)
            %             global_util = 0;
            %             for ii=1:obj.apps
            %                 local_util = obj.utility_func(...
            %                     obj.app_model{ii}.get_response_time(obj.demand(ii),obj.interarr(ii),cap(ii,:)),...
            %                     obj.r_thresholds(ii),...
            %                     obj.max_utils(ii));
            %
            %                 global_util = global_util + local_util;
            %             end
            r=[];
            for ii=1:obj.apps
                r(ii)=obj.app_model{ii}.get_response_time(obj.demand(ii),obj.interarr(ii),cap(ii,:));
            end
        end

        function [global_util]=compute_global_util(obj,cap)
            global_util=obj.calculate_global_util(obj.compute_r(cap));
        end

        % projected subgradient method applied to the primal problem
        function [x,fpbest_final,xx,fpbest]=cloud_opt_prob(obj,cap_init)
            placement = obj.placement;
            apps = obj.apps;
            pms = obj.pms;

            x = cap_init;
            MAX_ITERS = obj.MAX_ITERS;
            fpbest_final = 0;
            epsilon=0.1;


            % algorithm
            % r_=[];
            xx=[];
            fp = [-Inf]; fpbest = [-Inf];
            % fval=[];
            k = 1; k_in=1;
            while k < MAX_ITERS

                % computing constraints values
                % one constraint for each PM: the sum of all apps'VMs deployed on that PM minus PM capacity
                for i=1:pms
                    current_perPM_allocated(i)=sum(x(:,i));
                    constraint_value(i)=current_perPM_allocated(i)-obj.total_capacity(i);
                end

                % see if the point is feasible or not
                if max(constraint_value)<0
                    % util_per_app_layer=(-1.*K2.*(x-K1).^-2);
                    % primal objective values
                    % The objective value only changes for the iterations when x(k) is feasible
                    global_util = obj.compute_global_util(x);

                    %just for logging
                    xx(:,:,k_in)=x; k_in = k_in + 1;

                    fp(end+1) = global_util;
                    fpbest(end+1) = max( global_util, fpbest(end) );
                    fpbest_final = fpbest(end);

                    % x(k+1) = x(k)- ?f_0(x)
                    % subgradient calculation
                    % If the current point is feasible, we use an objective subgradient g(k)=?f0(x), as if the
                    % problem were unconstrained
                    for i=1:apps
                        for j=1:pms
                            delta=zeros(apps,pms); delta(i,j)=delta(i,j)+epsilon;
                            g(i,j)=(obj.compute_global_util(x+delta)-obj.compute_global_util(x))/epsilon;
                        end
                    end

                    g(find(placement==0))=0; %dont change g using vms that are not really ddeployed


                    % step size selection
                    % we use the square summable step size with ?k = 1/k for the optimality update.
                    % alpha = (global_util)/norm(g)^2;
                    alpha = 1000;

                    % projected subgradient update
                    % in feasible case we are moving towards the objective function gradient.
                    x = x + alpha*g;


                else
                    % feasibility update
                    % x(k+1) = x(k)- ?_i ?f_i(x)
                    % If the current point is infeasible, we choose any violated constraint,
                    % and use a subgradient of the associated constraint function ?f_i(x).
                    % where f_i(x) is the value of most violated inequality at x(k) and i
                    % is its index

                    % in our case ?f_i(x) is the the matrix with dimentions same as x which
                    % has 1 in each element that affects the constraint. since each constraint is sum of
                    % perPM_allocated capacity for each VM, ?f(x)_PM_i/?x(layer,app)
                    % is 1 if placement(layer,app)=PM_i and 0 if otherwise.
                    g = zeros(apps,pms);
                    pm_index=find(constraint_value==max(constraint_value));
                    g(:,pm_index)=1;
                    g= double(g & placement);

                    % step size selection
                    % the step size for infeasible update is ? = (f_i(x) + eps) / |g|^2_2,
                    % while epsilon>0 is a tolerance which can be different numbers
                    % (e.g. 10^-3)
                    alpha = (max(constraint_value)+epsilon)/norm(g).^2;

                    % projected subgradient update
                    % in infeasible case we are projecting the current point onto the set (halfspace) of points that
                    % satisfy that particular inequality.
                    x = x - alpha*g;
                end

                % individual projection onto the feasible set (saturation function)
                x = max( x, 0);

                % calculate response times when utility function is
                % optimized
                %                 r_=[];
                %                 for ii=1:apps
                %                     r_(ii)=app_model{ii}.get_response_time(demand(ii),interarr(ii),x(ii,:));
                %                 end
                %                 rr_(:,k)=r_;

                %xx(:,:,k)=x;

                k = k + 1;
            end

            %           figure
            %plot(fp);
            %            x_axis = (1:k-1);
            % plot(squeeze(xx(2,1,:)));
            %             plot(x_axis,squeeze(rr_(1,:)),...
            %                 x_axis,squeeze(rr_(2,:)),...
            %                 x_axis,squeeze(rr_(3,:)));
            %             h = legend('app1','app2','app3',3);
            %             set(h,'Interpreter','none')

        end %cloud_opt_prob

        %gives the realtive vm_id of the app i which is deployed on PM j
        %we assume there is only one VM on behalf of each app on each hyper
        function vm_id=get_vm_by_pmAndApp(obj,i,j)
            mapping =...
                [1 0
                1 2
                0 1];
            vm_id = mapping(i,j);
            % vm_id = j;
        end

        function [r]=invoke_simulation(obj,demands,interarrs,mips)
            %            import ceras.simulation.cloudsim.SimulationClient;
            import ceras.simulation.cloudsim.*;

            for i=1:obj.apps
                vm_num = 0;
                for j=1:obj.pms
                    if obj.placement(i,j)==1
                        vm_num = vm_num + 1;
                        obj.model.set_vm_mips(strcat('User',num2str(i)), vm_num, mips(i,j));
                        %                     if mips(i,j)>0 %0 in allocation means there is no vm of that app[ deployed in the pm
                        %                         obj.model.set_vm_mips(strcat('User',num2str(i)),obj.get_vm_by_pmAndApp(i,j),mips(i,j));
                    end
                end
            end

            for i=1:obj.apps
                obj.model.set_user_interarr(strcat('User',num2str(i)), interarrs(i));
                obj.model.set_user_demand(strcat('User',num2str(i)), demands(i));
            end

            % added
            % obj.model.writeInputDocument('c:\\hi1.xml');

            obj.model.setupExperiment(obj.step_timout);
            cl = SimulationClient(obj.model.getOutDoc());

            r = [];
            for i=1:obj.apps
                r=[r cl.getResponseTime(strcat('User',num2str(i)))];
            end
        end

        % load('dataset\interarr.mat', 'times','interarr', '-mat')
        function y=fifa98_workload(obj,average,height,period,N)
            pwd
            vars = whos('-file', 'dataset/workload_1day_min.mat');
            load('dataset/workload_1day_min.mat', vars(1).name);
            y = average+height*sin((1:N).*(2*pi)/period);
            y=y';
        end

        % plot(cloud_opt_simulation().sinus_workload(10,4,20,80))
        function y=sinus_workload(obj,average,height,period,N)
            y = average+height*sin((1:N).*(2*pi)/period);
            y=y';
        end

        function y=sinus_workload2(obj,lb,ub,period,phase, N)
            y = (lb+ub)/2+((ub-lb)/2)*sin((1:N).*(2*pi)/period +phase);
            y=y';
        end

        % generate workload for each platform
        function w_ = generate_workload(obj)
            w_=[];

            lb = round(rand(obj.apps,1)*40+22);
            ub = round(rand(obj.apps,1) *40+44);
            period = round(ones(obj.apps,1) * 100); %round(rand(obj.apps,1) *40+100);
            phase = round(rand(obj.apps,1)*40);

            for i=1:obj.apps
                % arrival rate is a sinus oscilating between 22 sec and 42
                % sec with average 32
                % w=[w obj.sinus_workload(32,10,20,obj.simulation_steps)];
                w = 1./obj.sinus_workload2(1/lb(i),1/ub(i),period(i),phase(i), obj.simulation_steps);

                %use the fifa interarrival times
                % w = fifa98workload();

                w_=[w_ w];
            end
        end

        function [s]=simulate(obj, w_)
            r_=[]; cap_=[]; fp_=[]; fp_sys_=[];



            for i=1:obj.simulation_steps
                obj.interarr = w_(i,:);

                % get proper capacity from the optimizer
                [modified_cap,fpbest_final]=obj.cloud_opt_prob(obj.initial_cap);
                cap_(:,:,i) = modified_cap;
                % cap_=[cap_ modified_cap];

                modeled_r = obj.compute_r(modified_cap);

                fp_=[fp_ fpbest_final];
                % fpbest_final

                % invoke the simulator with this capacity values
                r = obj.invoke_simulation(obj.demand,obj.interarr,modified_cap);
                disp(strcat('demand:',num2str(obj.demand),' interarr',num2str(obj.interarr),' r:',num2str(r)));

                fp_sys_ = [fp_sys_ obj.calculate_global_util(r)];

                %update model based on updated r
                for k=1:obj.apps
                    obj.app_model{k}.update_(r(k), obj.demand(k), obj.interarr(k), modified_cap(k,:));
                end

                r_ = [ r_ ; r];
            end

            s=struct('w', w_, 'r', r_, 'cap', cap_, 'fp', fp_,'fp_sys',fp_sys_);
        end

        function update_bare_xml_model(obj,placement)
            dh = struct(...
                'ram', 1200, ...
                'core_num', 1 , ...
                'mips', 1200);

            filePath = 'C:\\my\\project\\cloudsim\\mytest\\ceras\\simulation\\cloudsim\\topology\\BareDatacenter.xml';
            obj.model.parsePXL(filePath);
            [apps,pms] = size(placement);

            % add host entries
            for host_id=1:pms
                obj.model.add_host(host_id, dh.ram, dh.core_num, dh.mips);
            end

            % add user entries
            for app_id=1:apps
                % int userId, int meanDemand, int meanInterArrival, int arrivedNumLimit
                obj.model.add_application(strcat('User',num2str(app_id)), 500, 22, 10);
            end

            % add VMs for applications
            [app_id,pm_id] = find(placement>0);
            for i = 1:length(app_id)
                % app_id(i)
                % pm_id(i)

                % add_VM_to_app(int userId, int imageSize, int ram, int mips, int bw,
                %   int priority, String cloudletScheduler, int peNumber, int preferedHostId)
                % imageSize="1000" ram="512" mips="400" bw="1000"
                % priority="1" cloudletScheduler="SpaceShared" peNumber="1"
                obj.model.add_VM_to_app(strcat('User',num2str(app_id(i))), 1000, 512, 400, 1000, 1, 'SpaceShared', 1,  pm_id(i));
            end

            % write the new bare xml file
            import java.lang.String;
            % filePath = String(strcat(pwd , '\newInput.xml')).replaceAll('\\','\\\\');
            % obj.model.writeInputDocument(filePath);
            % test:
            %
        end

        function initialize_deployment(obj, apps, pms, pm_per_app)
            obj.apps = apps;
            obj.pms = pms;
            pm_per_app = pm_per_app;
            obj.total_capacity=ones(1,obj.pms)*1200; %in terms of mips
            obj.placement = placement().geneate_placement(obj.pms,obj.apps,pm_per_app);
            obj.update_bare_xml_model(obj.placement);

            % this should be fixed
            obj.initial_cap =  zeros(obj.apps,obj.pms);
            for i=1:obj.pms
                num_vm_for_this_pm = length(find(obj.placement(:,i)==1));
                for j= find(obj.placement(:,i)==1)
                    obj.initial_cap(j,i) = 0.9* obj.total_capacity(i)/num_vm_for_this_pm;
                end
            end

            obj.app_model = {};
            for i=1:obj.apps
                obj.app_model = {obj.app_model{:} App_model()};
                %                App_model()};
                %                App_lin_tracking_model()};
            end

            obj.demand = ones(1,obj.apps).*4100 + randn(1,obj.apps).*250;
            obj.interarr = ones(1,obj.apps).*22 + randn(1,obj.apps).*4;

            %             obj.r_thresholds = ones(1,obj.apps).*14 + randn(1,obj.apps).*4;
            %             obj.max_utils = ones(1,obj.apps).*10;
            obj.r_thresholds = ones(1,obj.apps).*14;
            obj.max_utils = ones(1,obj.apps).*10;
        end

        function M=animate_cap(obj,res,w_max,new_plot)
            cap = res.cap;
            w = res.w;
            [i,j,h_] = size(cap);
            for k = 1:h_
                % if ((new_plot==1) && (k==1 || k==h_))
                if (new_plot==1)
                    figure;
                end;
                h= bar3([(1./res.w(k,:)) .* min(min(res.w)) .* w_max/2 ; zeros(3,obj.apps) ; res.cap(:,:,k)'] );
                xlabel('Applications','Interpreter','LaTex');
                ylabel('Physical Machines','Interpreter','LaTex');
                zlabel('Capacity','Interpreter','LaTex');
                title('VM Capacities');
                % axis equal
                %axis([xmin xmax ymin ymax zmin zmax cmin cmax])
                axis([0 obj.apps 0 obj.pms+5 0 w_max 5 300])
                caxis auto
                text(10,7,400,'\leftarrow Capacity','HorizontalAlignment','left');
                text(2,4,400,'\leftarrow Workload','HorizontalAlignment','left');
                %                zlim([0 1000]);
                M(k) = getframe;
            end
            %movie(M,1,1);
        end

    end %methods


    methods(Static)
        function sc=test_scalability
            for apps = 1:10
                for pms = 1:10
                    sim = cloud_opt_simulation();
                    sim.initialize_deployment(i,j,min(pms,3));
                    s = sim.simulate();
                end
            end
        end


        function [sim,s] = test_sim()
            sim = cloud_opt_simulation();
            sim.initialize_deployment(10,7,3);
            w = sim.generate_workload();
            s = sim.simulate(w);
            % plot(s.r(:,:));
            % h = legend('app1','app2','app3',3);
            % set(h,'Interpreter','none')
        end


        % cloud_opt_simulation().show_optimality
        function show_optimality
            sim = cloud_opt_simulation();
            sim.twoVM_1PM_deploy();
            [modified_cap,fpbest_final,xx,fpbest]=sim.cloud_opt_prob(sim.initial_cap)

            %%%%%%%%%%%% draw the the model %%%%%%%%%%%%%%
            [x1,x2] = meshgrid(280:10:1200, 280:10:1200);
            % we have to cover all the x1+x2<1200 area
            fp = [];
            s = size(x1);
            for i=1:s(1)
                for j=1:s(2)
                    fp(i,j) = sim.compute_global_util([x1(i,j) ; x2(i,j)]);
                end
            end
            %x(find(x1+x2>1200)) = NaN;
            a = x1+x2>1200;
            x1(a) = NaN;
            x2(a) = NaN;
            fp(a) = NaN;
            handle = figure;
            surf(x1,x2,fp);
            % contour3(x1,x2,fp,80)
            hold on
            %%%%%%%%%%%% draw the optimization of the model %%%%%%%%%%%%%%
            plot3(squeeze(xx(1,1,:)),squeeze(xx(2,1,:)) , fpbest(2:end)', '-ok');
            h = findobj('Type','patch');
            set(h,'LineWidth',2)
            % figure; plot(fpbest);
            UtilityLib.print_figure(handle,9,7,'figures\twoVM_1PM_optimality');
        end
    end
end %class

% usage:
% clear
% sim = cloud_opt_simulation();
% sim.simulate()