#include <stdlib.h>
#include <stdio.h>
#include <matio.h>
#include <iostream>
#include <cmath>
#include <set>
#include <fstream>
#include <unordered_set>
#include <vector>
#include <algorithm>
#include <random>
#include <ctime>

using namespace std;


int
main(int argc,char **argv)
{
    mat_t    *matfp;
    matfp = Mat_CreateVer("mnist8m.mat2",NULL,MAT_FT_DEFAULT);
    matvar_t *matvar;
    mat_sparse_t  sparse = {0,};

    // int nnz = 1612242143;
    int nnz =  4100000;
    int rows = 8100000;
    // int cols = 784;
    int cols = 4;

    mat_int32_t  jc[cols + 1];

    ifstream infile("colptr");
    int size;
    int ind = 0;
    while (infile >> size) {
     jc[ind] = size;
     ind ++;
    }
    cout << "finish read colptr file, there are total " << size << " data to generate" <<endl;

    mat_int32_t  *ir = new mat_int32_t[nnz];

    int start = jc[0];
    clock_t begin = clock();
    for(int i = 1; i< cols + 1; i++){
      if (i % 25 == 0 ) {
        double elapsed_secs = double(clock() - begin) / CLOCKS_PER_SEC;
        cout << "start generating " << i << " line" << " used time: " << elapsed_secs << endl;
      }
       int set_size = jc[i] - start;
       unordered_set<int> irs;
       while( irs.size() < set_size) {
          irs.insert(rand() % rows);
       }
       vector<int> array(irs.begin(), irs.end() );
       sort(array.begin(), array.end());
       for(int j = 0; j < array.size(); j++) {
        ir[j + start] = array[j];
       }
       start = jc[i];
    }

    double *d = new double[nnz];
    for(int i = 0; i< nnz; i++){
       d[i] = rand() % 1000000 * 1.0 / 1000000.0;
    }
    // mat_int32_t  ir[25] = {0,4,1,2,3,0,4,1,2,3,0,4,1,2,3,0,4,1,2,3,0,4,1,2,3};
    // mat_int32_t  jc[11] = {0,2,5,7,10,12,15,17,20,22,25};

    // double    d[25] = {1,5,7,8,9,11,15,17,18,19,21,25,27,28,29,31,35,37,38,39,41,45,47,48,49};

    size_t dims[2] = {rows,cols};

    sparse.nzmax = nnz;
    sparse.nir   = nnz;
    sparse.ir    = ir;
    sparse.njc   = cols + 1;
    sparse.jc    = jc;
    sparse.ndata = nnz;
    sparse.data  = d;

    matvar = Mat_VarCreate("mnist8m",MAT_C_SPARSE,
                       MAT_T_DOUBLE,2,dims,&sparse,MAT_F_DONT_COPY_DATA);

    cout << "start writing matrix file" << endl;
    Mat_VarWrite(matfp,matvar,MAT_COMPRESSION_NONE);
    Mat_VarFree(matvar);
    Mat_Close(matfp);
    return EXIT_SUCCESS;
}
