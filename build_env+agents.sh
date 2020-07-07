#!/bin/bash

ROOT_DIR=`pwd`

printf "\n######################### Build Hanabi #########################\n"
sh hanabi_env/build_hanabi.sh

printf "\n######################### Build Walton #########################\n"
sh original_agents/build_scripts/build_walton.sh

printf "\n######################### Build QUUX #########################\n"
sh original_agents/build_scripts/build_quux.sh

# Put this at the end
cd $ROOT_DIR
