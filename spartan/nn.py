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
  row = np.random.randint(r, size = length)
  col = np.random.randint(c, size = length)
  # s = scipy.sparse.rand(r, c, density=0.2358).toarray()
  s = scipy.sparse.coo_matrix((data, (row, col)), shape=(r, c)).toarray()
  l = np.random.randint(10, size=r)
  ll = np.zeros([r, 10])
  ll[range(r), l.astype(int).flat] = 1
  return (sp.from_numpy(s).evaluate(), sp.from_numpy(ll).evaluate())

def main():
    sp.initialize()

    l1 = 784
    l2 = 300
    l3 = 10

    # W = (sp.randn(l1, l2) * math.sqrt(4.0 / (l1 + l2))).evaluate() # 784 * 300 matrix
    W = (sp.randn(l2, l1) * math.sqrt(4.0 / (l1 + l2))).evaluate() # 300 * 784 matrix
    # V = (sp.randn(l2, l3) * math.sqrt(4.0 / (l2 + l3))).evaluate() # 300 * 10 matrix
    V = (sp.randn(l3, l2) * math.sqrt(4.0 / (l2 + l3))).evaluate() # 10 * 300 matrix
    print "V:", V.glom()
    print "W:", W.glom()

    # generate distributed matrix
    rows = 81000
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
    print "train data length: ", len(train_data)

    np.random.shuffle(train_data)
    count = 0
    for (mb_samples, mb_labels) in train_data:
        begin = time.time()
        num_samples = mb_samples.shape[0]
        # input = mb_samples      # sample * 784 matrix
        input = mb_samples.T      # 784 * sample matrix
        # if count > 20 or count == 0:
        #    print "input data:", input.glom()
        # label = mb_labels   # sample * 10 matrix
        label = mb_labels.T   # 10 * sample matrix

        # ff
        ffs = time.time()
        # wIn = sp.dot(input, W)  # sample * 300 matrix
        wIn = sp.dot(W, input)  # 300 * sample matrix
        wOut = sigmoid(wIn)
        print "V", V
        # vIn = sp.dot(wOut, V)  # sample * 10 matrix
        vIn = sp.dot(V, wOut)  # 10 * sample matrix
        vOut = sigmoid(vIn)
        print "W.shape:%s, input shape:%s, V.shpae:%s, wOut.shpae:%s"%(str(W.shape), str(input.shape), str(V.shape), str(wOut.shape))
        vOut.evaluate()
        ffe = time.time()
        #bp
        bps = time.time()
        # o = vOut - label
        # if count > 20 or count == 0:
        #     print "vOut - label", o.glom()
        # vDelta = dsigmoid(vIn) * o         # sample * 10 matrix
        vDelta = dsigmoid(vIn) * (vOut - label)         # 10 * sample matrix
        # wDelta = dsigmoid(wIn) * (sp.dot(vDelta, V.T))  # sample * 300 matrix
        wDelta = dsigmoid(wIn) * (sp.dot(V.T, vDelta))  # 300 * sample matrix
        # wDelta.evaluate()
        bpe = time.time()
        # update
        upb = time.time()
        # V -= eps_w * sp.dot(wOut.T, vDelta) / num_samples  # 300 * 10 matrix
        V -= eps_w * sp.dot(vDelta, wOut.T) / num_samples  # 10 * 300 matrix
        # W -= eps_w * sp.dot(input.T, wDelta) / num_samples # 784 * 300 matrix
        W -= eps_w * sp.dot(wDelta, input.T) / num_samples # 300 * 784  matrix

        V.evaluate()
        W.evaluate()
        count = count + 1
        # if count > 20:
        #     print "V:", V.glom()
        #     print "W:", W.glom()
        upe = time.time()
        print "ff used time: %f, bp used time: %f, update used time: %f, num_samples: %d \n" %(ffe - ffs, bpe - bps, upe - upb, num_samples    )
        diff = time.time() - begin
        print "iteration: %d, spent time: %f\n" %(count, diff)

if __name__ == '__main__':
  main()
