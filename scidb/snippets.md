## SciDB related snippets
1. start SciDB cluster

~~~ shell
cd /opt/scidb/14.8
bin/scidb.py startall SciDB20
# config file locates in /opt/scidb/14.8/etc/config.ini
~~~

## Neural Network
2. create random sparse matrix with `generateRandomSciMat.sh`

2. load those generated csv text matrix with `loadSparseSciMats.sh`

### kill load process
~~~ shell
#kill related tsv2scidb process
killall tsv2scidb
pssh -h scislaves "killall tsv2scidb"
~~~

### **deprecated neural network plan**

generate sparse matrix in memory and then store

~~~
  store(
    redimension(
      apply
      (build(<val:double>[i1=0:2499,50,0,j1=0:2499,50,0], random()%1000000/1000000.0),
      j, iif( random()%100/100.0 <= 0.05 ,j1,null)),
      <val:double>[i=0:2499,1000,0,j=0:2499,1000,0]), a25k_s5);
~~~

### GNMF
1. generate `W` and `H` in SciDB

~~~ shell
cd /opt/scidb/14.8
time bin/iquery -anq "store(build(<val:double>[i=0:17999,500,0,j=0:199,200,0],random()%1000000/1000000.0), Wg);"

time bin/iquery -anq "store(build(<val:double>[i=0:199,200,0,j=0:479999,500,0],random()%1000000/1000000.0), Hg);""
~~~

1. generate 'V' with program RandomSpMat, and then load into scidb
~~~ shell
time /opt/scidb/14.8/bin/loadcsv.py -t NNN -x -a "$Vg_raw" -s "<row:int64,col:int64,value:double>[i=0:*,1000000,0]" -A "Vg" -X -S "<value:double>[row=0:17999,400,0,col=0:479999,400,0]" -i $THESIS/sci-mats/sci-18k-480k-0.01
~~~
