import spartan as sp
from datetime import datetime
import numpy as np
import scipy.sparse
import multiprocessing
import time

wrows = vrows = 18000
hcols = vcols = 480000
wcols = hrows = 200

sp.initialize()

V = sp.sparse_rand((vrows, vcols), density=0.01, dtype=np.double, format=u'csr')
W = sp.rand(wrows, wcols)
H = sp.rand(hrows, hcols)

eps = 10e-8

max_iteration = 5
i = 0

print "starts to run!"
start = time.time()

while i < max_iteration:
    begin = time.time()
    H = H * (sp.dot(W.T, V) / (sp.dot(sp.dot(W.T, W), H) + eps))
    W = W * (sp.dot(V, H.T) / (sp.dot(W, sp.dot(H, H.T)) + eps))
    i = i + 1
    end = time.time()
    diff = end - start
    print "iteration: %d, used time:%f secs\n" %(i, diff)

W.optimized()
W.evaluate()
finish = time.time()
duration = finish - start
print 'w: ', W.glom()
print "all the %d iterations used time %f" %(max_iteration, duration)
