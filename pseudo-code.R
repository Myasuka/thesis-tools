layer1 <- 28 * 28
layer2 <- 300
layer3 <- 10
iterations <- 300
fraction <- 0.01
learningRate <- 0.1

# initialize weights
W <- Matrix.rand(layer1, layer2)
V <- Matrix.rand(layer2, layer3)
(data, labels) <- loadMnistImage(path)
batchSize <- fraction * data.numRows

for (i in 1:iterations ){
  # Propagate through the network
  (input, label ) <- loadBatches(data, fraction)
  wIn <- input %∗% W
  wOut <- activate(wIn)
  vIn <- wOut %∗% V
  vOut <- activate(vIn)
  # Back Propagate the errors
  vDelta <- dActivate(vIn) ∗ (vOut − label)
  wDelta <- dActivate(wIn) ∗ (vDelta %∗% V.t)
  # update weight matrix
  V <- V - learningRate ∗ wOut.t %∗% vDelta / batchSize
  W <- W - learningRate ∗ input.t %∗% wDelta / batchSize
}

max_iteration <- 5

# load input matrix
V <- load(path = ...)
(W, H) <- (RandomMatrix, RandomMatrix)

# iteratively update W and H
for (i in 1:max_iteration) {
  H <- H * (W.t %*% V) / (W.t %*% W %*% H)
  W <- W * (V %*% H.t) / (W %*% H %*% H.t)
}
