import os
import csv
import tensorflow as tf

def test_gpu():
    gpus = tf.config.experimental.list_physical_devices('GPU')
    if gpus:
        try:
            # Restrict TensorFlow to only use the fourth GPU
            tf.config.experimental.set_visible_devices(gpus[0], 'GPU')

            # Currently, memory growth needs to be the same across GPUs
            for gpu in gpus:
                tf.config.experimental.set_memory_growth(gpu, True)
            logical_gpus = tf.config.experimental.list_logical_devices('GPU')
            print(len(gpus), "Physical GPUs,", len(logical_gpus), "Logical GPUs")
        except RuntimeError as e:
            # Memory growth must be set before GPUs have been initialized
            print(e)

class Writer:
    def __init__(self, path):
        self.path = path
        self.file = open(path, 'w', newline = '')
        self.writer = csv.writer(self.file, delimiter = ',', quotechar = '|', quoting = csv.QUOTE_MINIMAL)

        self.is_set_header = False

    def set_header(self, headers):
        # with open(self.path, 'w', newline = '') as file:
            # writer = csv.writer(file, delimiter = ',', quotechar = '|', quoting = csv.QUOTE_MINIMAL)
        self.writer.writerow(headers)

    def write_batch(self, rows):
        # with open(self.path, 'w', newline = '') as file:
            # writer = csv.writer(file, delimiter = ',', quotechar = '|', quoting = csv.QUOTE_MINIMAL)
        for row in rows:
            self.writer.writerow(row)
    
    def write_row(self, row):
        # with open(self.path, 'w', newline = '') as file:
            # writer = csv.writer(file, delimiter = ',', quotechar = '|', quoting = csv.QUOTE_MINIMAL)
        self.writer.writerow(row)

    def close(self):
        self.file.close()
