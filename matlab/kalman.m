classdef kalman
    properties
        hfunc;
        Hfunc;

        A, Q, R, P, x;
    end

    methods
        function obj=kalman(A, Q, R, x0)
            obj.Q=Q; obj.R=R; obj.A=A; obj.x = x0;
            obj.P = Q; %B*Q*B';     % Initial error covariance
            %%%%%%%%%%% begin symbolic %%%%%%%%%%
            syms b1 b2 b3 b4 b5 real
            syms r demand capacity interarr real

            %state vector
            b=[b1 b2 b3 b4 b5]

            % see that the y=f(x,u), I consider u=[demand capacity interarr]
            r = (b1.*demand + b2) ./ (b3.*capacity - b4.*(demand./interarr) + b5);

            obj.hfunc=r;
            obj.Hfunc = jacobian(r, b);

        end

        function  [x_out,h,errcov]=step(o,r_, capacity_,demand_,interarr_)
            disp 'kkk'
            %substitute all elements of derivitive function Hfunc with their values
            %(b1,...,b5,demand,capacity,interarr)
            H=o.Hfunc;
            H=double(subs(H, ...
                {'b1','b2','b3','b4','b5','demand','capacity','interarr'},...
                {o.x(1),o.x(2),o.x(3),o.x(4),o.x(5),demand_,capacity_,interarr_}));

            % Measurement update
            K = o.P*H'/(H*o.P*H'+o.R); %this is K

            %substitute all elements of hfunc with their values
            h=o.hfunc;
            h=double(subs(h, ...
                {'b1','b2','b3','b4','b5','demand','capacity','interarr'},...
                {o.x(1),o.x(2),o.x(3),o.x(4),o.x(5),demand_,capacity_,interarr_}));

            o.x = o.x + K * (r_ - h);   % x[n|n] filtering
            P = (eye(5)-K*H)*o.P;      % P[n|n]

            x_out = o.x;

            errcov = H*P*H';

            % Time update
            o.x=o.A*o.x;               % x[n+1|n] prediction
            o.P = o.A*o.P*o.A' + o.Q;      % P[n+1|n]
        end %step

    end %methods

        
    methods(Static)
        function test
            A = eye(5);             % no corelation betweeb b's
            Q=eye(5).*0.01;          % there are 5 unknown coefficients (b1 .... b5)
            R=1.*0.2;               % there is only one observation, r, the rest is modeled as input u

            % Initial condition on the state, obtained from offline anallysis
            x =[....
                1.0313
                46.0180
                1.0216
                0.6706
                13.7104];

            x0 = x;
            yest = [];
            xest = [];
            y_delta = [];
            rr = [];
            % ycov = zeros(length(t),1);
            
            t = 99;
            w = 0.1.*randn(t,5);
            v = 0.1.*randn(t,1);
            
            lb = 22; ub = 44; N=t; period= 20;
            workload = (lb+ub)/2+((ub-lb)/2)*sin((1:N).*(2*pi)/period); 
            
            sys=App_model();
            kalm = kalman(A, Q, R, x);
            demand = 4000;
            capacity = 280;
            interarr = 22;
            
            for i=1:t
                x = x + w(t);
                b1=x(1); b2=x(2); b3=x(3); b4=x(4); b5=x(5);
                % r = (b1.*demand + b2) ./ (b3.*capacity - b4.*(demand./interarr) + b5);
                r=sys.get_response_time(4000,workload(i),280);
                r = r + v(t);
                
                [x_est,h,errorcov]=kalm.step(r, 280,4000,workload(i));
                xest = [xest x_est];
                yest = [yest h];
                y_delta = [y_delta h-r];
                rr = [rr r];
                
            end
            
            %plot (xest-x0*ones(1,99));
            % plot (xest(5,:));
            plot (yest-rr);
            
        end
    end
end %classdef
