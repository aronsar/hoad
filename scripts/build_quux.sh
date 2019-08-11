#!/bin/bash

ROOT_DIR=`pwd`

# Deal with QUUX Agent
QUUX_DIR=$ROOT_DIR/experts/quux_models
cd $QUUX_DIR

# Build for quux
printf "\n######################### Step 1 #########################\n"
printf "Rebuild QUUX Agent Using Makefile\n\n"
make clean
make

# Put this at the end
cd $ROOT_DIR