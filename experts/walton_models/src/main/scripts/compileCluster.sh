#! /bin/bash
##
# Generate matchups for the cluster runner, 1 argument set per line.
#
# This should be usedwith the MixedAgentGameSingle class to run the approprate game.
##

# YARCC defaults to java 5, we use lambdas.
export JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk.x86_64
module load maven

# Rebuild the project incase something changed
git pull
mvn clean package

# setup our envrioment variables
export FIREWORKS_NUM_SEEDS=250 # 250 = 5000 jobs (maximum permitted by the cluster) at current number of agents

# Generate the matchups for pure and mixed games
$JAVA_HOME/bin/java -cp target/fireworks-0.1-SNAPSHOT.jar com.fossgalaxy.games.fireworks.cluster.GenerateGames > mixedArgs.txt
$JAVA_HOME/bin/java -cp target/fireworks-0.1-SNAPSHOT.jar com.fossgalaxy.games.fireworks.cluster.GeneratePureGames > pureArgs.txt

# output the number of matchups, this MUST be less than 5000
wc --lines mixedArgs.txt
wc --lines pureArgs.txt
