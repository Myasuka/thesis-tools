#! /usr/bin/python2.6
__author__ = 'Cloud'

import scidbpy
import math
import time
import numpy as np
import random
from scidbpy import connect


def remove(sdb):
    for key in sdb.list_arrays():
        if key.startswith('py'):
            sdb.remove(key)


def main():
    sdb = connect('http://localhost:8000')

    V = sdb.wrap_array("Vg")
    W = sdb.wrap_array("Wg")
    H = sdb.wrap_array("Hg")

    eps = 10e-8
    max_iteration = 5
    i = 0

    print "starts to run!"
    start = time.time()

    while i < max_iteration:
        begin = time.time()
        H = H * (sdb.dot(W.transpose(), V) / (sdb.dot(sdb.dot(W.transpose(), W), H) + eps))
        W = W * (sdb.dot(V, H.transpose()) / (sdb.dot(W, sdb.dot(H, H.transpose())) + eps))
        i = i + 1
        end = time.time()
        diff = end - begin
        print "iteration: %d, used time:%f secs\n" %(i, diff)

    finish = time.time()
    duration = finish - start
    print "all the %d iterations used time %f" %(max_iteration, duration)
    remove(sdb)


if __name__ == '__main__':
    main()
