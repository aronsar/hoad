#!/bin/bash

ROOT_DIR=`pwd`

# Deal with Walton Agent
WALTON_DIR=$ROOT_DIR/original_agents/walton_models
cd $WALTON_DIR

# Run maven to rebuild
printf "\n######################### Step 1 #########################\n"
printf "Rebuild Walton Agent Using Maven\n\n"
WALTON_TARGET_DIR=$WALTON_DIR/target
if [ -d "$WALTON_TARGET_DIR" ]; then
    rm -rf $WALTON_TARGET_DIR
fi
mvn clean package

# Create Walton.jar
printf "\n######################### Step 2 #########################\n"
printf "Create Walton.jar\n\n"
WALTON_JAR_FILE=$WALTON_DIR/walton.jar
ORIGINAL_JAR_FILE=$WALTON_TARGET_DIR/fireworks-0.2.6-SNAPSHOT-jar-with-dependencies.jar
if [ -f "$WALTON_JAR_FILE" ]; then
    rm $WALTON_JAR_FILE
fi
echo "Make link at $WALTON_JAR_FILE"
ln -s $ORIGINAL_JAR_FILE $WALTON_JAR_FILE

# Put this at the end
cd $ROOT_DIR
