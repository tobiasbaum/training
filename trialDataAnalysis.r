library(lme4)

trialData <- read.csv("trialData.csv", sep = ";")

trialData$durationSeconds <- (trialData$end - trialData$start) / 1000

ich <- trialData[trialData$user=="usr-988707161",]

plot(ich$trialOverall, ich$durationSeconds)
plot(ich$trialForTask, ich$durationSeconds)

cor(ich$trialOverall, ich$durationSeconds)
cor(ich$trialForTask, ich$durationSeconds)

ichRichtig <- ich[ich$incorrect=="false",]
ichFalsch <- ich[ich$incorrect=="true",]

mixed.lmer <- lmer(durationSeconds ~ trialOverall + (1|task), data = ichRichtig)
summary(mixed.lmer)
