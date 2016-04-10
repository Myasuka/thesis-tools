import spartan as sp
import math
from datetime import datetime
import numpy as np
import random
import scipy.sparse
import multiprocessing
import time

def sigmoid(x):
    return 1 / (1 + sp.exp(-x))

def dsigmoid(x):
    t = sigmoid(x)
    return (1 - t) * t


def _split_sample(merged_mb):
  r = merged_mb[0]
  c = merged_mb[1]
  density = 0.23588
  data = merged_mb[2]
  length = int(r * c * density) + 1
  row = np.random.randint(c, size = length)
  col = np.random.randint(c, size = length)
  # s = scipy.sparse.rand(r, c, density=0.2358).toarray()
  s = scipy.sparse.coo_matrix((data, (row, col)), shape=(r, c)).toarray()
  l = np.random.randint(10, size=r)
  ll = np.zeros([r, 10])
  ll[range(r), l.astype(int).flat] = 1
  return (sp.from_numpy(s).evaluate(), sp.from_numpy(ll))

def main():
    sp.initialize()

    l1 = 784
    l2 = 300
    l3 = 10

    W = (sp.randn(l1, l2) * math.sqrt(4.0 / (l1 + l2))).evaluate() # 784 * 300 matrix
    V = (sp.randn(l2, l3) * math.sqrt(4.0 / (l2 + l3))).evaluate() # 300 * 10 matrix

    # generate distributed matrix
    rows = 8100000
    cols = 784
    num_workers = 100
    subrow = rows / num_workers
    density = 0.23588
    eps_w = 0.01
    length = int(subrow * cols * density) + 1
    a = np.random.rand(length)
    b = np.random.rand(length)
    c = np.random.rand(length)
    list = []
    for i in range(num_workers):
        r = i % 3
        if r == 0:
            list.append((subrow, cols, a))
        elif r == 1:
            list.append((subrow, cols, b))
        else:
            list.append((subrow, cols, c))

    print "starts to generate train_data!"
    start = datetime.now()
    train_data = map(_split_sample, list)
    end = datetime.now()
    diff = end - start
    t = diff.microseconds / 1000000.0 + diff.seconds
    print "generating all matrix spent time: ", t, "secs"

    np.random.shuffle(train_data)
    count = 0
    for (mb_samples, mb_labels) in train_data:
        begin = time.time()
        num_samples = mb_samples.shape[0]
        input = mb_samples      # sample * 784 matrix
        label = mb_labels   # sample * 10 matrix

        # ff
        wIn = sp.dot(input, W)  # sample * 300 matrix
        wOut = sigmoid(wIn)
        vIn = sp.dot(wOut, V)  # sample * 10 matrix
        vOut = dsigmoid(vIn)
        vDelta = dsigmoid(vIn) * (vOut - label)         # sample * 10 matrix
        wDelta = dsigmoid(wIn) * (sp.dot(vDelta, V.T))  # sample * 300 matrix
        # update
        V -= eps_w * sp.dot(wOut.T, vDelta) / num_samples  # 256 * 10 matrix
        W -= eps_w * sp.dot(input.T, wDelta) / num_samples # 784 * 300 matrix

        V.evaluate()
        W.evaluate()
        count = count + 1
        diff = time.time() - begin
        print "iteration: %d, spent time: %f" %(count, diff)

if __name__ == '__main__':
  main()
