#! /bin/bash
set -e
#params
export GENERATOR_ARGS="hat 5"
export FIREWORKS_REPEAT_COUNT=1
export FIREWORKS_NUM_SEEDS=1000000
export RESULT_DIR="hat"
export JOB_NAME="Hat_Validation"

./src/main/scripts/validation/buildAndExecuteValidation.sh

