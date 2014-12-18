classdef kalman_general <  handle
    properties
        A, B, Q, R, P, x, state_vars_num;
        
        my_h_sub, my_H_sub;
    end

    methods      
        
        function obj=kalman_general(A, B, P, Q, R, x0, hfunc,state_sym,input_sym)        
            obj.state_vars_num = length(x0);
            obj.Q=Q; obj.R=R; obj.A=A; obj.x = x0; obj.P = P; obj.B = B;
            
           
            my_h_sub= @(state,input)...
                (double(subs(hfunc, ...
                sym2cell([state_sym,input_sym]),...
                num2cell([state,input]))));
            
            %here we symbolicly compute the derivitive of hfunc over state
            %vector (here b)
            Hfunc = jacobian(hfunc, state_sym);
            my_H_sub= @(state,input)...
                (double(subs(Hfunc, ...
                sym2cell([state_sym,input_sym]),...
                num2cell([state,input]))));
            
            obj. my_h_sub =  my_h_sub;
            obj.my_H_sub = my_H_sub; 
        end

        % h=f(x,u) thus some symbols of h are going to be substituted by
        % state vector elements while others are substituted by measurement
        % vector
        function  [x_out,h,errcov]=step(o,measured_output, measured_input)

            %substitute all elements of derivitive function Hfunc with their values
            H=o.my_H_sub(o.x',measured_input');
            %@(state,input)(double(subs(Hfunc,[state_sym,input_sym],num2cell([state,input]))))
            
            % Measurement update
            K = o.P*H'/(H*o.P*H'+o.R); %this is K

            %substitute all elements of hfunc with their values
            h=o.my_h_sub(o.x',measured_input');
            o.x = o.x + K * (measured_output - h);   % x[n|n] filtering            
            o.P = (eye(o.state_vars_num)-K*H)*o.P;      % P[n|n]

            %refine h based on filtered x's
            h=o.my_h_sub(o.x',measured_input');
            x_out = o.x;            
            errcov = H*o.P*H';

            % Time update
            o.x=o.A*o.x + o.B*measured_input;               % x[n+1|n] prediction
             o.P = o.A*o.P*o.A' + o.Q;      % P[n+1|n]
            % o.P = o.A*o.P*o.A' + o.B*o.Q*o.B';        % P[n+1|n]
        end %step

    end %methods

    methods(Static)
      function f=sym2cell(g)
            n = numel(g(:));
            f = cell(1, n);
            for index = 1:n
                f{index} = char(g(index));
            end
      end    
    end
end %classdef
