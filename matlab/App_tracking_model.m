%******************** per application model *****************
classdef App_tracking_model <  handle
    properties
        kalm;
        b;
        hfunc;
    end

    methods
        function obj=App_tracking_model()
            A = eye(5);               % no corelation betweeb b's
             Q=eye(5).*9;          % there are 5 unknown coefficients (b1 .... b5)
             R=1.*0.9;                 % there is only one observation, r, the rest is modeled as input u

            % slow filter
%             Q=eye(5).*2;
%             R=1.*1;
            
            % Initial condition on the state, obtained from offline regession anallysis
            b0 =[....
                1.0313
                46.0180
                1.0216
                0.6706
                13.7104];

            % begin symbolic
            syms b1 b2 b3 b4 b5 real
            syms demand capacity interarr real

            % see that the y=f(x,u), I consider u=[demand capacity interarr]
            % here we define what in our model is state and what is input
            % model:
            % b1..b5 -> state(1)..state(5)
            % demand,capacity,interarr -> input(1),..,input(3)
            obj.hfunc = (b1.*demand + b2) ./ (b3.*capacity - b4.*(demand./interarr) + b5);
            

            P = Q;
            B = zeros(5,3);
            obj.kalm = kalman_general(A, B , P, Q, R, b0, obj.hfunc, [b1 b2 b3 b4 b5],[demand capacity interarr]);            
%            obj.hfunc = hfunc;

            obj.b = b0; 
        end

        % updates the current kalman estimation
        function update_(obj, real_r, demand,interarr,capacity)
             capacity = sum(capacity);
            obj.b = obj.kalm.step(real_r, [demand, capacity,interarr]');
        end

        % get response time multiple times from updated model 
        function [r]=get_response_time(obj,demand,interarr,capacity)
            capacity = sum(capacity);
            r = (obj.b(1).*demand + obj.b(2)) ./ (obj.b(3).*capacity - obj.b(4).*(demand./interarr) + obj.b(5));
            %             r = ...
%             (double(subs(obj.hfunc, ...
%                 {'b1','b2','b3','b4','b5','demand','capacity','interarr'},...
%                 num2cell([obj.b',[demand,capacity,interarr]]))));

        end
    end %methods
    
     methods(Static)
        function test
            rr = []; xx=[];
            % sys = App_model();
            tr_model = App_tracking_model();

            t = 99;
            w = 5*sin(1:t);
            v = 0.1.*randn(t,1);

            lb = 22; ub = 44; N=t; period= 20;
            % workload = (lb+ub)/2+((ub-lb)/2)*sin((1:N).*(2*pi)/period); 
            workload = ones(1,N)* 22;
            demand = 4000;
            capacity = 280;
            
            
            
            for i=1:t
                b =[1.0313
                    46.0180 + 30*sin(i*pi/10)
                    1.0216
                    0.6706
                    13.7104];
                b1=b(1); b2=b(2); b3=b(3); b4=b(4); b5=b(5);

                % r1=sys.get_response_time(demand,workload(i),capacity);
                r1 = (b1.*demand + b2) ./ (b3.*capacity - b4.*(demand./workload(i)) + b5);
                tr_model.update_(r1+v(t), demand,workload(i),capacity);
                % tr_model.b(2)
                r2=tr_model.get_response_time(demand,workload(i),capacity);                
                rr = [rr ; [r1 r2]];    
                xx = [xx ; [tr_model.b(2) b2]];
            end
            
            plot (rr);           
            legend('r1','r2',2);
        end
     end %methods static
end