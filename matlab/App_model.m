%******************** per application model *****************
classdef App_model
    properties
        beta =[....
            1.0313
            46.0180
            1.0216
            0.6706
            13.7104];

        b1, b2, b3, b4, b5;
%         b1 = beta(1);
%         b2 = beta(2);
%         b3 = beta(3);
%         b4 = beta(4);
%         b5 = beta(5);
    end

    methods
        function obj=App_model()
            obj.b1 = obj.beta(1);
            obj.b2 = obj.beta(2);
            obj.b3 = obj.beta(3);
            obj.b4 = obj.beta(4);
            obj.b5 = obj.beta(5);
        end

         % dummy method
        function update_(obj, real_r, demand,interarr,capacity)
            
        end
        
        function [r]=get_response_time(obj,demand,interarr,capacity)
            capacity = sum(capacity);
            r = (obj.b1.*demand + obj.b2) ./ (obj.b3.*capacity - obj.b4.*(demand./interarr) + obj.b5);
        end
        
        function set_beta(b)
            obj.beta = b;
        end
    end
end