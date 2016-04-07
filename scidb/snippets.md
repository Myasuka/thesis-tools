## SciDB related snippets
1. start SciDB cluster

~~~ shell
cd /opt/scidb/14.8
bin/scidb.py startall SciDB20
# config file locates in /opt/scidb/14.8/etc/config.ini
~~~

1. create random sparse matrix with `generateRandomSciMat.sh`

1. load those generated csv text matrix with `loadSparseSciMats.sh`
