#!/bin/bash

ROOT_DIR=`pwd`

# Deal with QUUX Agent
FFLOWER_DIR=$ROOT_DIR/experts/fireflower_model
cd $FFLOWER_DIR

# Build for quux
printf "\n######################### Create Fireflower Data  #########################\n"
printf "Build Fireflower Agent\n\n"

export JAVA_HOME=/data1/shared/fireflowerenv/jre1.8.0_221
export PATH=$JAVA_HOME/bin:$PATH

# Put this at the end
cd $ROOT_DIR
