res = read.table('response_time_interarr.csv',  header = TRUE , sep = "," )
attach(res)

Demand,Interarr,Cap,ResponseT

postscript(file="testplot.eps",
           horizontal=TRUE)
plot(Cap[1:292],ResponseT[1:292],
	xlab="Capacity", 
	ylab="Response Time")
dev.off()
he
postscript(file="demand_response_time.eps",horizontal=TRUE)
seq = seq(44, 6424, by=292)
cbind (Cap[seq], Interarr[seq], Demand[seq],ResponseT[seq])
plot(Demand[seq],ResponseT[seq],
	xlab="Demand", 
	ylab="Response Time")

dev.off()

ps2pdfwr testplot.eps testplot.pdf
xpdf testplot.pdf 

# linear model
model <- lm(ResponseT ~ Demand+Interarr+Cap)
model <- lm(ResponseT ~ Demand+Interarr+Cap+Demand*Interarr+Cap*Interarr)

model
model$coefficient
# draw
lines(cbind(Cap,model$fitted.values))
summary(model)

(Demand[1:10]*Interarr[1:10])/(Cap[1:10]*Interarr[1:10]-Demand[1:10])

#nonlinear model
# see https://stat.ethz.ch/pipermail/r-help/2002-April/020736.html
nmodel<-nls(ResponseT[1:10] ~ (Demand[1:10]*Interarr[1:10])/(Cap[1:10]*Interarr[1:10]-Demand[1:10]))

nmodel<-nls(ResponseT ~ (b1*Demand+b2)/(b3*Cap-b4*(Demand/Interarr)+b5))
nmodel<-nls(ResponseT ~ (b1*Demand*Interarr)/(b2*Cap*Interarr-Demand))
nmodel<-nls(ResponseT ~ ((b1*Demand*Interarr+b2)/(b3*Cap*Interarr-b4*Demand)))
nmodel<-nls(ResponseT ~ ((b1*Demand*Interarr+b2)/(b3*Cap*Interarr+b4*Demand+b5)),
start = list(b1=1,b2=0,b3=1,b4=1,b5=0))
summary(nmodel)
 


