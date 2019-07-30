#! /bin/bash
##
# Make the damn thing work on the yarcc cluster
#
# Repeatable, well defined runs mode
##
set -e

VERSION=0.1.0
SUBJECT=webpigeon
JAR_FILE=fireworks-0.2.3-SNAPSHOT-jar-with-dependencies.jar
JOB_FILE=mixed.job
JOB_CHEAT_FILE=mixedCheat.job
GENERATOR_CLASS=com.fossgalaxy.games.fireworks.cluster.GenerateGames
MAX_JOB_SIZE=5000
JOB_NAME="Hanabi_Run"

# params
export FIREWORKS_NUM_SEEDS=200

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
TASK_DIR=`pwd`/results/tasks/`date -I`/$GIT_COMMIT

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
cp src/main/scripts/$JOB_FILE $TASK_DIR/$JOB_FILE
cp src/main/scripts/$JOB_CHEAT_FILE $TASK_DIR/$JOB_CHEAT_FILE
echo $GIT_COMMIT > $TASK_DIR/commit
mkdir -p $TASK_DIR/results/ # place to put our data :)
mkdir -p $TASK_DIR/results-cheat/ # place to put our data :)
echo "[OK] files in place"

##
# Job creation phase: generate arguments
##
echo "generating arguments..."
$JAVA_HOME/bin/java -cp $TASK_DIR/$JAR_FILE $GENERATOR_CLASS > $TASK_DIR/args.txt
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
    echo $QLOG > qsub.$i.log
    echo "[OK] job file submitted: $QLOG"

    # cheat jobs
    QLOG=$(qsub -t $i-$END_VAL -tc $CONCURRENT_TASKS -N ${JOB_NAME}_Cheat $JOB_CHEAT_FILE)
    echo $QLOG > qsub-cheat.$i.log
    echo "[OK] job file submitted: $QLOG"
done

#qsub -N Mail_Hanabi_Run -hold_j $JOB_NAME mail.job
#qsub -N Mail_Hanabi_Cheat -hold_j ${JOB_NAME}_Cheat mail.job
