%******************** per application model *****************
% this implements a linear model: we can approximate R as a linear or non-linear function, using Taylor series around the operational point.
% if you process a first order approximation further:
% F(x; y) = f(x0; y0) + fx(x0; y0)(x - x0) + fy(x0; y0)(y -y0) 
% you get the following formula:  
%   R= c0 + c1.cap + c2.tau + c3.demand


% good justification for that for on-line identification of the coeficients. I wrote the equation and the Kalman filter equations for computing the coefficients. 
classdef App_lin_tracking_model <  handle
    properties
        kalm;
        c;
        hfunc;
    end

    methods
        function obj = App_lin_tracking_model()
            A = eye(3);               % no corelation betweeb c's
            Q=eye(3).*9;              % there are 3 unknown coefficients (b1 .... b5)
            R=1.*0.9;                 % there is only one observation, r, the rest is modeled as input u
            
            % Initial condition on the state, obtained from offline regession anallysis
            % c_val =[2; 2; 2];
            c_val =[1.9936; 0.0182; 1.8727];            

            % begin symbolic
            syms c0 c1 c2  real
            syms demand capacity interarr real

            % see that the y=f(x,u), I consider u=[demand capacity interarr]
            % here we define what in our model is state and what is input
            % model:
            % c0..c3 -> state(1)..state(5)
            % capacity,interarr -> input(1),..,input(3)
            obj.hfunc =  c0 + c1*capacity + c2 * interarr;

            P = Q;
            B = zeros(3,2); %x=Ax+Bu
            obj.kalm = kalman_general(A, B , P, Q, R, c_val, obj.hfunc, [c0 c1 c2],[capacity interarr]);            
%            obj.hfunc = hfunc;

            obj.c = c_val; 
        end

        % updates the current kalman estimation
        function update_(obj, real_r, demand,interarr,capacity)
             capacity = sum(capacity);
            obj.c = obj.kalm.step(real_r, [capacity,interarr]');
            obj.c
        end

        % get response time multiple times from updated model 
        function [r]=get_response_time(obj,demand,interarr,capacity)
            capacity = sum(capacity);
            r = obj.c(1) + obj.c(2)*capacity + obj.c(3)*interarr;
            
           
%             r = ...
%             (double(subs(obj.hfunc, ...
%                 {'c0','c1','c2','capacity','interarr'},...
%                 num2cell([obj.c',[capacity,interarr]]))));

        end
    end %methods
end