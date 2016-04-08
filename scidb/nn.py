#! /usr/bin/python2.6
__author__ = 'Cloud'

import scidbpy
import math
from datetime import datetime
import numpy as np
import random
from scidbpy import connect


def remove(sdb):
    for key in sdb.list_arrays():
        if key.startswith('py'):
            sdb.remove(key)


def main():
    sdb = connect('http://localhost:8000')
    data = sdb.wrap_array("mnist8m_200")
    labels = sdb.wrap_array("label")
    V = sdb.wrap_array("V")
    W = sdb.wrap_array("W")
    V.approxdc()
    r = 8100000
    fra = 0.01
    learningRate = 0.1
    batchSize = int(fra * r)
    iterations = 15

    start = datetime.now()
    duration = 0
    print "nn starts to run!"
    for i in range(iterations):
        starti = datetime.now()
        index = random.randint(1, r - batchSize)
        indexEnd = index + batchSize
        input = data[index: indexEnd, :]
        label = labels[index: indexEnd, :]
        wIn = sdb.dot(input, W)
        wTemp = sdb.exp(-wIn)
        wOut = 1 / (1 + wTemp)
        vIn = sdb.dot(wOut, V)
        vTemp = sdb.exp(-vIn)
        vOut = 1 / (1 + vTemp)
        vDelta = vTemp / (1 + vTemp) ** 2 * (vOut - label)
        wDelta = wTemp / (1 + wTemp) ** 2 * sdb.dot(vDelta, V.transpose())
        V = V - learningRate * sdb.dot(wOut.transpose(), vDelta) / batchSize
        W = W - learningRate * sdb.dot(input.transpose(), wDelta) / batchSize
        endi = datetime.now()
        diff = endi - starti
        t = diff.microseconds / 1000000.0 + diff.seconds
        duration += t
        print "iteration:", i, " used time: ", t, "secs"
    end = datetime.now()
    print "duration:", duration
    diff = end - start
    print "all time:", diff.seconds
    remove(sdb)

if __name__ == '__main__':
    main()
