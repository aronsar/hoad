import random
import numpy as np
import multiprocessing as mp
from itertools import repeat
import data_generator as dg


def sample_task_batch(labels, config):
    """
    Sample a batch for training / evaluating a task based on given class labels to be sampled from
    Shape: x_task (batch_size, height width, channel)
           y_task (batch_size, 1)
    """
    num_shots, num_classes = config[0], config[1]
    x_task_batch, y_task_batch = [], []

    # N-Way K-shot Sampling
    # First Sample first by N (num classes) then by K (num instances per class)
    n_way_labels = random.sample(range(len(labels)), len(labels))
    for i in range(num_classes):
        raw_label_id = labels[i]
        true_label = n_way_labels[i]
        # Set train batch

        # sampled_imgs_id = random.sample(
        #     range(len(DataGenerator.x_train[raw_label_id])), num_shots)
        img_count = dg.DataGenerator.dataset_obj.get_image_count_by_label(
            raw_label_id)
        sampled_imgs_id = random.sample(range(img_count), num_shots)
        task_imgs, task_labels = [], []
        for j, img_id in enumerate(sampled_imgs_id):
            # sampled_img = np.array(
            #     DataGenerator.x_train[raw_label_id][img_id])
            sampled_img = dg.DataGenerator.dataset_obj.get_image(raw_label_id,
                                                                 img_id)

            task_imgs.append(sampled_img)
            task_labels.append(true_label)  # y is 0 ~ self.num_classes

        x_task_batch.append(task_imgs)
        y_task_batch.append(task_labels)

    # Convert to numpy array
    x_task_batch, y_task_batch = np.array(
        x_task_batch), np.array(y_task_batch)

    # Convert from (N, K, 28, 28, 1) to (K, N, 28, 28, 1)
    x_task_batch = np.swapaxes(x_task_batch, 0, 1)
    y_task_batch = np.swapaxes(y_task_batch, 0, 1)

    # Shuffle along N mutually
    for i in range(num_shots):
        shuffled_id = random.sample(
            range(num_classes), num_classes)
        x_task_batch[i], y_task_batch[i] = x_task_batch[i][shuffled_id], y_task_batch[i][shuffled_id]

    return x_task_batch, y_task_batch


def _mp_batching(mp_func, task_ids_list, config, process_count=4):
    """
    Definition:
        Multi-Proceesing support for batching
    Return:
        train_batch, eval_batch
    """
    batch = []
    mp_args = zip(task_ids_list, repeat(config))
    with mp.Pool(process_count) as p:
        batch = p.starmap(mp_func, mp_args)

    return batch


def _loop_batching(func, task_ids_list, config):
    """
    Definition:
        Loop batching. Faster when batching is a light task
    Return:
        train_batch, eval_batch
    """
    batch = []
    for task in range(len(task_ids_list)):
        task_ids = task_ids_list[task]

        x_batch, y_batch = func(task_ids, config)
        batch.append((x_batch, y_batch))

    return batch
