#!/usr/local/bin/python2.7
import time
import spartan as sp
import sys
from datetime import datetime

sp.initialize()

m = sys.argv[1]
k = sys.argv[2]
n = sys.argv[3]

print "execute matrix multiplication, m: %d, k: %d, n: %d. date: %s\n"%(m, k , n, datetime.now())

a = sp.rand(m, k)
b = sp.rand(k, n)

start = time.time()
z = sp.dot(a,b).optimized()
z.evaluate()
end = time.time()
print 'z: ', z

print "finish matrix multiplication, diff time: %d. date: %s\n" % ((end - start), datetime.now())
