function r = responsetimeModel(beta,x)
%   YHAT = responsetime(BETA,X) gives the predicted values of the
%   reaction rate, YHAT, as a function of the vector of 
%   parameters, BETA, and the matrix of data, X.
%   BETA must have 5 elements and X must have three
%   columns.
%
%   The model form is:
%   y = (b1*x2 - x3/b5)./(1+b2*x1+b3*x2+b4*x3)



b1 = beta(1);
b2 = beta(2);
b3 = beta(3);
b4 = beta(4);
b5 = beta(5);

demand = x(:,1);
interarr =  x(:,2);
capacity = x(:,3);

r = (b1.*demand + b2) ./ (b3.*capacity - b4.*(demand./interarr) + b5);

%yhat = (b1*x2 - x3/b5)./(1+b2*x1+b3*x2+b4*x3);