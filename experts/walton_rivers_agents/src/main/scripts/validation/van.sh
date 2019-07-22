#! /bin/bash
set -e
#params
export GENERATOR_ARGS="vandenbergh 3"
export FIREWORKS_REPEAT_COUNT=1
export FIREWORKS_NUM_SEEDS=10000
export RESULT_DIR="vandenbergh"
export JOB_NAME="Van_Validation"

./src/main/scripts/validation/buildAndExecuteValidation.sh

