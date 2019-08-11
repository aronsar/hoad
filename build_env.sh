#!/bin/bash

ROOT_DIR=`pwd`
SCRIPTS_DIR=$ROOT_DIR/scripts

printf "\n######################### Build Hanabi #########################\n"
sh $SCRIPTS_DIR/build_hanabi.sh

printf "\n######################### Build Walton #########################\n"
sh $SCRIPTS_DIR/build_walton.sh

printf "\n######################### Build QUUX #########################\n"
sh $SCRIPTS_DIR/build_quux.sh

# Put this at the end
cd $ROOT_DIR
