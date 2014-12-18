N=100;
% ar_coeffs=arburg(y,3)
ts0 = idpoly([1 -1 0.7 -0.2],[])
e = idinput(N,'rgs');
y = sim(ts0,e);
mb = ar(y,5,'burg')
yy = sim(mb,[e;e(end).*ones(10,1)]);
plot(1:100,y,1:110,yy);
