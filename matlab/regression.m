initial_beta = ones(5,1)
x=R(:,1:3);
y=R(:,4);
[betahat,resid,J] = nlinfit(x,y,@responsetimeModel,initial_beta);
betahat
 betaci = nlparci(betahat,resid,J)
 [yhat,delta] = nlpredci(@responsetimeModel,x,betahat,resid,J);
 opd = [y yhat delta];
 opd(1:40,:)


