function simpleKalman

A = [1.1269   -0.4940    0.1129
    1.0000         0         0
    0    1.0000         0];

B = [-0.3832
    0.5919
    0.5191];

C = [1 0 0];
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
Q = 1; R = 1;

%Generate a sinusoidal input and process and measurement noise vectors and
t = [0:100]';
u = sin(t/5);

n = length(t)
randn('seed',0)
w = sqrt(Q)*randn(n,1);
v = sqrt(R)*randn(n,1);
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Use process noise w and measurement noise v generated above
sys = ss(A,B,C,0,-1);
y = lsim(sys,u+w);      % w = process noise
yv = y + v;             % v = measurement noise

%you can implement the time-varying filter with the following for loop.

P = B*Q*B';         % Initial error covariance
x0 = zeros(3,1);     % Initial condition on the state
ye = zeros(length(t),1);
ycov = zeros(length(t),1);


%********** kalman starts ********************
% hfunc = C * x + D * u;
syms x1 x2 x3 inp real
x = [x1 x2 x3]
hfunc = C * x';
% states = [x1 x2 x3];
inputs = inp;
kalm = kalman_general(A, B, P, Q, R, x0, hfunc, x ,inputs );


%*************************************************

for i=1:length(t)
    [x_out,h,errcov_]=kalm.step(yv(i), u(i));
    errcov(i) = errcov_;
    ye(i) = h;
end

%You can now compare the true and estimated output graphically.
subplot(211), plot(t,y,'--',t,ye,'-')
title('Time-varying Kalman filter response')
xlabel('No. of samples'), ylabel('Output')
subplot(212), plot(t,y-yv,'-.',t,y-ye,'-')
xlabel('No. of samples'), ylabel('Output')