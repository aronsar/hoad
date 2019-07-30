#! /bin/bash
set -e

VERSION=0.1.0
SUBJCET=pwillic
JAR_FILE=fireworks-0.1-SNAPSHOT-jar-with-dependencies.jar
JOB_FILE=validate.job
GENERATOR_CLASS=com.fossgalaxy.games.fireworks.cluster.GenerateValidationGames
MAX_JOB_SIZE=5000
# check the repo is clean (local changes mean pull could fail)
if [[ -n $(git status -s) ]]
then
	echo "[ERROR] working directory not clean, aborting run"
	exit 1
fi

##
# setup phase: create the structure we'll need going forward
##
git pull
GIT_COMMIT=$(git rev-parse HEAD)

# Create a folder to drop stuff into
TASK_DIR=`pwd`/results/tasks/`date -I`/$GIT_COMMIT/$RESULT_DIR

# Sanity check - don't permit the same run twice
if [ -d "$TASK_DIR" ]; then
	echo "[ERROR] task directory exists, i would over write data, abort!"
	exit 1
fi

mkdir -p $TASK_DIR

##
# build phase: create jar file
##
echo "building project..."
export JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk
module load maven
mvn clean package > $TASK_DIR/build.log
echo "[OK] project built"

##
# Deploy phase: copy the stuff we need to run
##
echo "copying files..."
# drop the stuff we need in our task directory
cp target/$JAR_FILE $TASK_DIR/$JAR_FILE
cp src/main/scripts/validation/$JOB_FILE $TASK_DIR/$JOB_FILE
echo $GIT_COMMIT > $TASK_DIR/commit
mkdir -p $TASK_DIR/results/ # place to put our data :)
echo "[OK] files in place"

##
# Job creation phase: generate arguments
##
echo "generating arguments..."
$JAVA_HOME/bin/java -cp $TASK_DIR/$JAR_FILE $GENERATOR_CLASS $GENERATOR_ARGS > $TASK_DIR/args.txt
ARG_COUNT=($(wc --lines $TASK_DIR/args.txt))
ARG_COUNT=${ARG_COUNT[0]}
echo "[OK] generated $ARG_COUNT setups."

##
# Job submission phase: submit jobs and prey
##
echo "submitting jobs..."
cd $TASK_DIR

CONCURRENT_TASKS=$(($ARG_COUNT<150?$ARG_COUNT:150))

# Run jobs in batches of MAX_JOB_SIZE
for i in `seq 1 $MAX_JOB_SIZE $ARG_COUNT`;
 do
   let LAST_VAL=i+$MAX_JOB_SIZE-1;
   END_VAL=$(($LAST_VAL<$ARG_COUNT?$LAST_VAL:$ARG_COUNT))

   echo "Creating job for $i to $END_VAL"

    # normal jobs
    QLOG=$(qsub -t $i-$END_VAL -tc $CONCURRENT_TASKS -N $JOB_NAME $JOB_FILE)
    echo $QLOG > qsub.log
    echo "[OK] job file submitted: $QLOG"
done