package com.jmatio;

import com.jmatio.io.MatFileWriter;
import com.jmatio.types.MLArray;
import com.jmatio.types.MLSparse;

import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.SynchronousQueue;


public class GenMnistMat {

    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.out.println("usage: GenMnistMat <matrix name> <read file> <save file>");
            System.exit(1);
        }
        long start = System.currentTimeMillis();
        String name = args[0];
        String readFile = args[1];
        String output = args[2];
        MLSparse mlSparse = new MLSparse(name, new int[] {8100000, 784}, MLArray.mxDOUBLE_CLASS, 1612242144);
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(readFile)));
        String line;
        int count = 0;
        String row = "0";
        int r = 0;
        int size = 24576;
        double[] values = new double[size];
        int[][] index = new int[size][2];
        int ind = 0;
        while ((line = reader.readLine())!= null){
            count ++;
            if(count % 20000000 == 0) {
                System.out.println("reading line " + count + ", already spent " + (System.currentTimeMillis() - start) + " mills");
            }
            String[] split = line.split("\\s+");
            index[ind][0] = Integer.parseInt(split[0]);
            index[ind][1] = Integer.parseInt(split[1]);
            values[ind] = Double.parseDouble(split[2]);
            ind ++;
            if(ind >= size) {
                for (int i = 0; i < ind; i++) {
                    mlSparse.set(values[i], index[i][0], index[i][1]);
                }
                ind = 0;
            }
        }
        for (int i = 0; i < ind; i++) {
            mlSparse.set(values[i], index[i][0], index[i][1]);
        }
        System.out.println("total counts: " + count);
        ArrayList<MLArray> list = new ArrayList<MLArray>();
        list.add( mlSparse );
        File outFile = new File(output);
        System.out.println("start to write file");
        new MatFileWriter(outFile, list);
        long end = System.currentTimeMillis();
        System.out.println("used time: " + (end - start) + " mills");
        reader.close();

    }
}
