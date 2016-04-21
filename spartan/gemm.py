#!/usr/local/bin/python2.7
import time
import spartan as sp
import sys
from datetime import datetime

#argvLen = len(sys.argv);
#print "total fileds in sys.argv=",argvLen;
     
#for i,eachArg in enumerate(sys.argv):
#  print "[%d]=%s"%(i, eachArg);

m = int(sys.argv[1])
k = int(sys.argv[2])
n = int(sys.argv[3])

sp.initialize()
print "execute matrix multiplication, m: %d, k: %d, n: %d. date: %s\n"%(m, k , n, datetime.now())

a = sp.rand(m, k)
b = sp.rand(k, n)

start = time.time()
z = sp.dot(a,b).optimized()
z.evaluate()
end = time.time()
print 'z: ', z

print "finish matrix multiplication, diff time: %d. date: %s\n" % ((end - start), datetime.now())
