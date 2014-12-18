classdef cloud_opt_simulation
    properties
        pms = 2;
        total_capacity=ones(1,pms)*1000; %in terms of mips

        %*************** PaaS level stuff utility model *******************************
        apps = 3;

        demand = [6100 4100 4100];
        interarr = [22 22 22];

        r_thresholds = [20 4 12];
        max_utils = [10 10 10];
    end

    methods
        
        % projected subgradient method applied to the primal problem
        function []=cloud_opt_prob()

            %***************** IaaS level stuff ***************************

            % for i = 1:apps
            %     util_functions{i} = @(r)utility_func(r,r_threshold(i),max_util(i));
            % end

            % app models
            % K1=[4;24] %rand(layers,apps)*10+10;
            % K2=rand(layers,apps)*10+10;

            %******************* placement and resource allocation*************************

            % app * pm  -> allocated_or_not
            placement= ...
                [1 0
                1 1
                0 1];


            % initial point of capacity allocared in terms of model unit (here mips)
            % app * pm  -> allocated amount
            x = [280 0
                280 280
                0   280];

            %***********************************************************
            app_model = App_model;

            %***********************************************************
            function u=utility_func(r,threshold,max_util)
                %         u = 0;
                %         if r<threshold
                %             u = max_util;
                %         end
                u = max_util*(atan(-(r-threshold))+pi/2)/pi;
            end

            function [global_util]=compute_global_util(cap)
                global_util = 0;
                for ii=1:apps
                    local_util = utility_func(...
                        app_model.get_response_time(demand(ii),interarr(ii),cap(ii,:)),...
                        r_thresholds(ii),...
                        max_utils(ii));

                    global_util = global_util + local_util;
                end
            end
            %*************************************************************


            epsilon=0.1;
            MAX_ITERS = 200;


            % algorithm
            r_=[];
            xx=[];
            fp = [-Inf]; fpbest = [-Inf];
            fval=[];
            k = 1;
            while k < MAX_ITERS

                % computing constraints values
                % one constraint for each PM: the sum of all apps'VMs deployed on that PM minus PM capacity
                for i=1:pms
                    current_perPM_allocated(i)=sum(x(:,i));
                    constraint_value(i)=current_perPM_allocated(i)-total_capacity(i);
                end

                % see if the point is feasible or not
                if max(constraint_value)<0
                    % util_per_app_layer=(-1.*K2.*(x-K1).^-2);
                    % primal objective values
                    % The objective value only changes for the iterations when x(k) is feasible
                    global_util = compute_global_util(x);

                    fp(end+1) = global_util;
                    fpbest(end+1) = max( global_util, fpbest(end) );

                    % x(k+1) = x(k)- ?f_0(x)
                    % subgradient calculation
                    % If the current point is feasible, we use an objective subgradient g(k)=?f0(x), as if the
                    % problem were unconstrained
                    for i=1:apps
                        for j=1:pms
                            delta=zeros(apps,pms); delta(i,j)=delta(i,j)+epsilon;
                            g(i,j)=(compute_global_util(x+delta)-compute_global_util(x))/epsilon;
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

                r_=[];
                for ii=1:apps
                    r_(ii)=app_model.get_response_time(demand(ii),interarr(ii),x(ii,:));
                end
                rr_(:,k)=r_;

                xx(:,:,k)=x;

                k = k + 1;
            end

            x
            r_
            figure
            %plot(fp);
            x_axis = (1:k-1);
            % plot(squeeze(xx(2,1,:)));
            plot(x_axis,squeeze(rr_(1,:)),...
                x_axis,squeeze(rr_(2,:)),...
                x_axis,squeeze(rr_(3,:)));
            h = legend('app1','app2','app3',3);
            set(h,'Interpreter','none')

            % plot(xx(2,:),xx(1,:))
            %plot(xx)

        end

    end

end