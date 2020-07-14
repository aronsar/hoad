#!/bin/bash

ROOT_DIR=`pwd`

# Create CMake build dir
printf "\n######################### Step 1 #########################\n"
printf "Create cmake build dir for Hanabi\n\n"
HANABI_BUILD_DIR=$ROOT_DIR/hanabi_env/build/
if [ -d "$HANABI_BUILD_DIR" ]; then
    rm -rf $HANABI_BUILD_DIR
fi
echo "Make Directory at $HANABI_BUILD_DIR"
mkdir -p $HANABI_BUILD_DIR

# Run cmake
printf "\n######################### Step 2 #########################\n"
printf "Run cmake for Hanabi\n\n"
cd $HANABI_BUILD_DIR
cmake .. || { echo 'cmake failed'; exit 1; }
make || { echo 'make failed'; exit 1; }
cd $ROOT_DIR

# Move libraries and clean up
printf "\n######################### Step 3 #########################\n"
printf "Move libraries and clean up\n\n"
HANABI_SO_FILE=$ROOT_DIR/hanabi_env/libpyhanabi.so
if [ -f "$HANABI_SO_FILE" ]; then
    rm $HANABI_SO_FILE
fi
echo "Moving libpyhanabi.so to $HANABI_SO_FILE"
mv $ROOT_DIR/hanabi_env/build/libpyhanabi.so $HANABI_SO_FILE

HANABI_A_FILE=$ROOT_DIR/hanabi_env/hanabi_lib/libhanabi.a
if [ -f "$HANABI_A_FILE" ]; then
    rm $HANABI_A_FILE
fi
echo "Moving libhanabi.a to $HANABI_A_FILE"
mv $ROOT_DIR/hanabi_env/build/hanabi_lib/libhanabi.a $HANABI_A_FILE

rm -rf $HANABI_BUILD_DIR

# Put this at the end
cd $ROOT_DIR
