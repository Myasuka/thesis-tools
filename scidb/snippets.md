## SciDB related snippets
1. start SciDB cluster

~~~ shell
cd /opt/scidb/14.8
bin/scidb.py startall SciDB20
# config file locates in /opt/scidb/14.8/etc/config.ini
~~~

2. create random sparse matrix with `generateRandomSciMat.sh`

2. load those generated csv text matrix with `loadSparseSciMats.sh`

### kill load process
~~~ shell
#kill related tsv2scidb process
killall tsv2scidb
pssh -h scislaves "killall tsv2scidb"
~~~

### **deprecated plan**

generate sparse matrix in memory and then store

~~~
  store(
    redimension(
      apply
      (build(<val:double>[i1=0:2499,50,0,j1=0:2499,50,0], random()%1000000/1000000.0),
      j, iif( random()%100/100.0 <= 0.05 ,j1,null)),
      <val:double>[i=0:2499,1000,0,j=0:2499,1000,0]), a25k_s5);
~~~
