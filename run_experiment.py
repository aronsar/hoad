"""
This script is meant as an end to end, data creator, trainer, and evaluater.
It is set up so that the tasks within can easily be done manually as well,
by splitting up the tasts into separate scripts/modules.
"""
import gin
from tensorflow.python import keras
from utils.parse_args import parse
from train import train_model
from create_load_data import create_load_data
from evaluate import evaluate_model


@gin.configurable
class RunConfig(object):
    def __init__(
            self,
            mode: str = gin.REQUIRED,
            data_dir: str = "./data",
            eval_data_path: str = None,
            expert_dir: str = "./experts",
            agents_to_use: [] = ["rainbow"],
            run_dir: str = None,
            checkpoint_dir: str = None,
            results_dir: str = None,
            output_dir: str = "./output",
            newrun: bool = False
    ):
        self.mode = mode
        self.data_dir = data_dir
        self.eval_data_path = eval_data_path
        self.expert_dir = expert_dir
        self.agents_to_use = agents_to_use
        self.run_dir = run_dir
        self.checkpoint_dir = checkpoint_dir
        self.results_dir = results_dir
        self.output_dir = output_dir
        self.newrun = newrun


def main():
    args = parse()
    config_path = args.config_path

    gin.external_configurable(keras.optimizers.Adam, module='tensorflow.python.keras.optimizers')
    gin.external_configurable(keras.losses.categorical_crossentropy, module='tensorflow.python.keras.losses')
    gin.parse_config_file(config_path)

    # args = RunConfig()
    # args.config_path = config_path

    data = create_load_data(args)

    model = train_model(data, args)

    evaluate_model(data, model, args)


if __name__ == "__main__":
    main()
