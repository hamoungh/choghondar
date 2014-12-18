classdef placement < handle
   properties
   end
   
   methods
       function pl=geneate_placement(obj,pms,apps,pm_per_app)
            pms_=1:pms;
            degrees = randn(apps,1)+pm_per_app; % some change in pm_per_app
            degrees = round(min(max( degrees,1),pms)); % apply the constraints
            pl = zeros(apps,pms);
            for app=1:apps
                % shuffels pm list, selects first 'degrees(app)' pms and assign the app to them
                selected_pms = pms_(RANDPERM(length(pms_)));
                pl(app,selected_pms(1:degrees(app)))=1;
            end
       end

       function print(obj,pl)
          disp(obj.pl); 
       end
       
       % UtilityLib.print_figure(handle,9,7,'figures\vm_distribution');
       function handle = show_as_stacked_bar(obj,pl)
            handle = figure;
            bar(pl','stack')
            %legend('app1','app2','app3',3);
            title('VM placement over PMs as stacked bar chart.');
            xlabel('Physical Machiine');
            ylabel('VM');
       end
       
       % UtilityLib.print_figure(handle,9,7,'figures\vm_distribution_scatter'); 
       function handle = show_as_scatter(obj,pl)
           handle = figure;
            % scatter(X,Y)
            [i,j]=find(pl == 1);
            scatter(i,j,60,'s','filled',...
                            'LineWidth',2,...
                            'MarkerEdgeColor','b',...
                            'MarkerFaceColor',[.49 1 .63]);
            title('VM placement over PMs as scatter plot');
            xlabel('Physical Machiine');
            ylabel('VM');                        
       end
       
      % placement().sample
      function sample(obj)
            pms= 10 * 14; % 10 racks of 14 servers
            apps = 120;
            pm_per_app = 3;
            
%            total_capacity=ones(1,pms)*1200; %in terms of mips           
            pl = obj.geneate_placement(pms,apps,pm_per_app);
            obj.show_as_scatter(pl);
       end

   end
end





