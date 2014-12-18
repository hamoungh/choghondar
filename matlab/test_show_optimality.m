classdef test_simple_deployment
    properties
    end
    methods
        function twoVM_1PM_deploy(obj)
            obj.pms = 1;
            obj.total_capacity=ones(1,obj.pms)*1200; %in terms of mips

            %********* placement and resource allocation*****

            % app * pm  -> allocated_or_not
            obj.placement= ...
                [1
                1];

            % initial point of capacity allocared in terms of model unit (here mips)
            % app * pm  -> allocated amount
            obj.initial_cap =  ...
                [280
                280];



            %             obj.app_model = {
            %                 App_tracking_model()
            %                 App_tracking_model()};

            obj.app_model = {
                App_model()
                App_model()};

            obj.apps = 2;

            obj.demand = [5100 4100];
            obj.interarr = [22 22];
            obj.r_thresholds = [16 7];
            obj.max_utils = [10 10];

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
end