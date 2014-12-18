ts0 = idpoly([1 -1.5 0.7],[]);
ir = sim(ts0,[1;zeros(24,1)]);
% Define the true covariance function
Ry0 = conv(ir,ir(25:-1:1));
e = idinput(200,'rgs');
% Define y vector
y = sim(ts0,e); 
% iddata object with sampling time 1
y = iddata(y)
plot(y)
figure

per = etfe(y);
speh = spa(y);
ffplot(per,speh,ts0)
figure
% Estimate a second-order AR model
ts2 = ar(y,2);
ffplot(speh,ts2,ts0,'sd',3)
figure
% Get covariance function estimates
Ryh = covf(y,25);
Ryh = [Ryh(end:-1:2),Ryh]';
ir2 = sim(ts2,[1;zeros(24,1)]);
Ry2 = conv(ir2,ir2(25:-1:1));
plot([-24:24]'*ones(1,3),[Ryh,Ry2,Ry0])
figure
% The prediction ability of the model
%compare(y,ts2,5)