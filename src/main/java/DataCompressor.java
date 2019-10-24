import org.apache.commons.math3.distribution.CauchyDistribution;
import quantization.LloydMaxU16ScalarQuantization;
import quantization.Utils;
import quantization.de.DeException;
import quantization.de.jade.JadeSolver;
import quantization.utilities.Stopwatch;

import java.io.FileNotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


class RunnableTest implements Runnable {

    public long tid = -1;

    @Override
    public void run() {
        tid = Thread.currentThread().getId();
        //System.out.println(tid);
    }
}

public class DataCompressor {
    public static void main(String[] args) throws FileNotFoundException {


/*
        int coreCount = Runtime.getRuntime().availableProcessors() - 1;
//        Thread[] threads = new Thread[coreCount];
        ExecutorService es = Executors.newFixedThreadPool(coreCount);
        RunnableTest[] runnables = new RunnableTest[coreCount];
        for (int i = 0; i < coreCount; i++) {
            runnables[i] = new RunnableTest();
            es.execute(runnables[i]);
        }


        es.shutdown();
        try {
            es.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            System.out.println("Thread interrupted: " + e.getMessage());
        }

        for (int i = 0; i < coreCount; i++) {
            System.out.println(runnables[i].tid);
        }

        System.out.println("All threads finished");

 */

        final String sourceFile = "D:\\tmp\\server-dump\\small.bin";
        final int NumberOfBits = 4;
        final int Dimension = (int) Math.pow(2, NumberOfBits);
        int[] values = Utils.convertU16BytesToInt(Utils.readFileBytes(sourceFile));

//        LloydMaxU16ScalarQuantization quantization = new LloydMaxU16ScalarQuantization(values, NumberOfBits);
//        quantization.train();

        JadeSolver jadeSolver = new JadeSolver(Dimension, 10 * Dimension, 5, 0.05, 0.1);
        jadeSolver.setTrainingData(values);

        try {
            jadeSolver.train();
        } catch (DeException e) {
            e.printStackTrace();
        }

        System.out.println("Finished learning...");
    }
}
