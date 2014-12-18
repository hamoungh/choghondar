classdef workload
    properties
    end
    methods(Static)       
        % [counts,urlCount]=workload.get_requests_per_min(0,60*60);
        % counts2=counts(1:2:end-1)+counts(2:2:end)
        % regexprep(mat2str(counts2),' ',',')
        % plot(counts)
        function [counts,urlCounts,intarr]=get_requests_per_sec(num_ignored_req,seconds)
            fid = fopen('C:\cygwin\home\w_common_log.out');
            % fid = fopen('/mnt/e/cygwin/home/w_common_log.out');
            
            tline = fgetl(fid);
            period = 60; %seconds
            counts = [];
            c_ = 0;
            t_first = 0;
            y=0;
            time = 0;
            t0=0;   
            urlCounts = struct();
            intarr = [];
            while (ischar(tline) && time-t0<seconds) %  y<length(counts)<num)
                try                    
                    % [tok mat] = regexp(tline,'\d+ - - \[(\d+)?/.+/.+:(\d+)?:(\d+)?:(\d+)? \+0000\] ".+htm.*" .+ .+','tokens', 'match');
                    [tok mat] = regexp(tline,'\d+ - - \[(\d+)?/.+/.+:(\d+)?:(\d+)?:(\d+)? \+0000\] "GET (.+htm.*)? HTTP/1.0" .+ .+','tokens', 'match');
                    if (~(isempty(tok)))
                       %  fprintf(1,'%s\n',tline);
                        url = genvarname(tok{:}(5));
                        url = url{1};
                        if isfield( urlCounts, url)
                            urlCounts.(url)=urlCounts.(url)+1; 
                        else
                            urlCounts.(url)=1;
                        end
                         time = str2num(char(tok{:}(1:4)))'*[24*60^2;60^2;60^1;1];
                         if (t0 ==  0); t0 = time; end;
                        if (t_first == 0);t_first=time; end;
                        if (time-t_first>period)
                            intarr = [intarr (time-t_first)/c_]; 
                            t_first=time;
                            counts = [counts c_];                            
                            c_ = 0;
                        else
                            c_ = c_ + 1;
                        end
                    end
                catch ME1
                     disp ME1
                end

                 % skip num_ignored_req requests
%                 for j=0:num_ignored_req
%                     tline = fgetl(fid);
%                 end
                    
                tline = fgetl(fid);  
                y = y+1;
            end
            %counts = [counts c_];
            fclose(fid);
        end %test

        % sample log line:
        % 110807 - - [14/May/1998:22:00:02 +0000] "GET
        % /english/images/team_hm_header_shad.gif HTTP/1.1" 200 1379
        function [times,interarr] =get_interarrival(num_ignored_req,secs)
             fid = fopen('C:\cygwin\home\w_common_log.out');
            % fid = open('/mnt/e/cygwin/home/w_common_log.out');
            
            tline = fgetl(fid); 
            interarr = [];
            times = [];
            t_last = 0;
            time = 0;
            t0=0;            
            while (ischar(tline) && time-t0<secs) % experiment for 4 hours
                if (ischar(tline)) 
                    try                                            
                    [tok mat] = regexp(tline,'\d+ - - \[(\d+)?/.+/.+:(\d+)?:(\d+)?:(\d+)? \+0000\] "GET (.+htm.*)? HTTP/1.0" .+ .+','tokens', 'match');
                        if (~(isempty(tok)))       
                            % fprintf(1,'%s\n',tline); %                                            
                             time = str2num(char(tok{:}(1:4)))'*[24*60^2;60^2;60^1;1];
                            if (t0 ==  0); t0 = time; end;
                            times = [times time-t0];
                            interarr = [interarr time-t_last];
                            t_last = time;
                        end
                    catch ME1
                        disp ME2
                    end
                    tline = fgetl(fid);
                    
%                     try
%                         % skip num_ignored_req requests
%                         for j=0:num_ignored_req
%                             tline = fgetl(fid);
%                         end
%                     catch ME2
%                     end
                end %if
            end %for
            fclose(fid);
            interarr = interarr(2:length(interarr));
            times = times(2:length(times));
        end 

        function test_get_interarrival
             handle=figure;
             [times,interarr] = workload.get_interarrival(30,6*61*60);            
             plot(times/60,interarr)
             title(strcat('Inter-Arrival Times'));
                xlabel('Time(min)');
                ylabel('Inter-Arrival time(sec)');
             UtilityLib.print_figure(handle,9,7,'figures\workload-interarr');
             save('dataset\interarr.mat','times','interarr','-ASCII')
        end
        
        % say 'global counts' in command line
        function test_get_requests_per_min
             global counts
             handle=figure;
             counts = workload.get_requests_per_sec(0 , 6*61*60)
             plot(counts)
             title(strcat('Arrival Rate'));
                xlabel('Time(min)');
                ylabel('Arrival Rate(req/min)');
             UtilityLib.print_figure(handle,9,7,'figures\workload-arr-rate2');
        end
    end %methods
end
