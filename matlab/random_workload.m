function y=random_workload(N)
% idpoly creates a model object containing parameters that describe the general multiple-input single-output model structure.
% m = idpoly(A,B) A, B specify the polynomial coefficients.
% in general Aq y(t)=Bq u1(t-k1)
% in this case y(t)-1.5q^-1.y(t)+0.7q^-2.y(t)=0
% or y(t)-1.5y(t-T)+0.7y(t-T)=u(t)+0.5u(t-T)
%y(t)= 1.5y(t-T)+0.7y(t-2T)


ts0 = idpoly([1 -1 0.7 -0.2],[])


%Generate N sampple for noise e with Gaussian distribution ('rgs')
% e is a column vector with N rows.
e = idinput(N,'rgs');

% Using 'y = sim(m,u)' as a way of obtaining a noise-corrupted simulation 
% with Gaussian noise. u is the noise sources
% a noise-free simulation is obtained by not having any u


%ir = sim(ts0,[1;zeros(N-1,1)]); % impulse response
y = sim(ts0,e);                 % Gaussian noise response

% iddata object with sampling time 1
% iddata is a class for storing time-domain and frequency-domain data
%y = iddata(y)


end