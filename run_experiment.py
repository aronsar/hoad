# This script is meant as an end to end, data creator, trainer, and evaluater.
# It is set up so that the tasks within can easily be done manually as well,
# by splitting up the tasts into separate scripts/modules.

#import statements
from utils import parse_args, dir_utils
import load_data
import train
#import evaluate
import tensorflow as tf
import keras

def main():
    #parse arguments
    args = parse_args.parse_with_resolved_paths()    
    args = dir_utils.resolve_run_directory(args)

    import pdb; pdb.set_trace()
    #create/load data
    data = load_data.main(args)

    #train model/load model
    model = train.main(data, args)

    #evaluate model
    evaluate.main(data, model, args)

if __name__ == "__main__":
    main()
