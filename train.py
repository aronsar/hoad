import importlib
import gin
import os
from keras.callbacks import CSVLogger, TensorBoard, ModelCheckpoint

@gin.configurable
class TrainConfig(object):
    def __init__(self, args,
                 optimizer=None,
                 loss=None,
                 metrics=None,
                 batch_size=None,
                 epochs=None):
        self.optimizer = optimizer
        self.loss = loss
        self.metrics = metrics
        self.batch_size = batch_size
        self.epochs = epochs


def train_model(data, args):
    train_config = TrainConfig(args)
    mode_module = importlib.import_module("modes." + args.mode)

    train_generator = mode_module.DataGenerator(data.train_data)
    val_generator = mode_module.DataGenerator(data.validation_data)

    model = mode_module.build_model(args)

    model.compile(
        optimizer=train_config.optimizer,
        loss=train_config.loss,
        metrics=train_config.metrics
    )

    results_csv_file = os.path.join(args.results_dir, "results.csv")
    weight_file = os.path.join(args.checkpoints_dir, "Epoch-{epoch:02d}-Val-Acc-{val_acc:.4f}.hdf5")

    results_callback = CSVLogger(results_csv_file, append=True, separator=';')
    checkpoints_callback = ModelCheckpoint(weight_file, save_best_only=True)

    tensorboard_callback = TensorBoard(log_dir=os.path.join(args.results_dir), histogram_freq=0, write_graph=True, write_images=True)

    model.fit_generator(
        generator=train_generator,
        validation_data=val_generator,
        verbose=2,
        epochs=train_config.epochs,
        shuffle=True,
        callbacks=[results_callback, tensorboard_callback, checkpoints_callback]
    )

    return model

