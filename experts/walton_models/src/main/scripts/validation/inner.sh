#! /bin/bash
set -e
#params
export GENERATOR_ARGS="internal 2"
export FIREWORKS_REPEAT_COUNT=1
export FIREWORKS_NUM_SEEDS=100
export RESULT_DIR="inner"
export JOB_NAME="Inner_Validation"

./src/main/scripts/validation/buildAndExecuteValidation.sh

