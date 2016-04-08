#!/usr/bin/python
import time
import spartan as sp

sp.initialize()

a = sp.sparse_rand((25000, 25000), density=0.1)
b = sp.sparse_rand((25000, 25000), density=0.1)

start = time.time()
z = sp.dot(a,b).optimized()
print 'z: ', z

z.evaluate()

end = time.time()
print z[0:5, 0:5].glom()
print "numpy diff time:", (end - start)
