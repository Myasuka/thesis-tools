## Spartan related snippets

### NeuralNetwork related work

I first try to use original offcial mnist withn softmax [solution](https://github.com/spartan-array/spartan/blob/master/spartan/examples/mnist_mlp.py). This solution use `scipy.sparse.loadmat` [method](http://docs.scipy.org/doc/scipy/reference/generated/scipy.io.loadmat.html), and the problem is how to generat binary Matlab matrix file, I give two solutions below:
1. java solution using [jmatio](https://github.com/gradusnikov/jmatio). However, as original text matrix is 22GB, this program runs OOM. The code can be found in file `GenMnistMat.java`.
1. c solution using [matio](https://github.com/tbeu/matio), the code can be found in file `generateMnist.cpp`. This solution runs much faster and can generate 19GB file in 2000 seconds. `colptr` file records the column information of the original big matrix. However, if the matrix dimension is  too large, both Octave and scipy cannot both load the generated `.mat` file. (While not so large-size matrix file can be loaded).

I use the final solution to generate random matrix in memory, which can be found in 'nn.py'
