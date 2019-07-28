#!/bin/bash
##
# run_experiment - Runs results experiment and dumps it into the log.
# Created by Joseph Walton-Rivers 2016
#
# Run it, it generates the required files for the experimental log.
##
set -e

VERSION=0.1.0
SUBJECT=webpigeon
USAGE="stupidly simple"

if [[ -n $(git status -s) ]]
then
	echo "[ERROR] working directory not clean, aborting run"
	exit 1
fi

# work out important values
GIT_COMMIT=$(git rev-parse HEAD)
GIT_MESSAGE=$(git log -1 HEAD --pretty=format:%s)
MVN_VERSION="0.1-SNAPSHOT" # TODO automate this

## Result Variables
BASE_DIR=`pwd`/results
RESULT_DIR=$BASE_DIR/$GIT_COMMIT
RESULT_FILE=$RESULT_DIR/result.csv
RESULT_LOG=$RESULT_DIR/error.log
RESEARCH_LOG=$BASE_DIR/runs.log

echo this is commit $GIT_COMMIT - starting run

# make sure stuff exists
mkdir -p $RESULT_DIR
touch $RESEARCH_LOG

# the actual runner
mvn clean package > $RESULT_DIR/$GIT_COMMIT.build.log
cd target/
java -cp fireworks-$MVN_VERSION.jar com.fossgalaxy.games.fireworks.MixedAgentGame 1>$RESULT_FILE 2>$RESULT_LOG
echo $(date --iso-8601) $(hostname) $GIT_COMMIT $GIT_MESSAGE >> $RESEARCH_LOG
echo run finished.
