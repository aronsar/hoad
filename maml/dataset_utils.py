import os
import tensorflow.keras as tk
import tensorflow as tf


def read_imgs_in_directory(img_path, config):
    """
    Definition:
        Reads Images within the given directory / path.
        Convert the image into grayscale and resize to (28 * 28)

    Returns:
        List: [img1, img2 ....]
    """
    color_mode, target_size = config[0], config[1]
    imgs = os.listdir(img_path)
    imgs_in_dir = []
    for img in imgs:
        img_name = os.path.join(img_path, img)
        img_obj = tk.preprocessing.image.load_img(img_name,
                                                  color_mode=color_mode,
                                                  target_size=target_size)
        img_arr = tk.preprocessing.image.img_to_array(img_obj)
        img_arr = tf.cast(img_arr, tf.float32) / 255.0
        img_arr = 1 - img_arr
        imgs_in_dir.append(img_arr)

    return imgs_in_dir
