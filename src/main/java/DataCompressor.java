import org.apache.commons.math3.distribution.CauchyDistribution;
import quantization.LloydMaxU16ScalarQuantization;
import quantization.Utils;
import quantization.de.DeException;
import quantization.de.DeHistory;
import quantization.de.jade.JadeSolver;
import quantization.utilities.Stopwatch;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
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
    public static void main(String[] args) throws IOException {

        final String sourceFile = "D:\\tmp\\server-dump\\small.bin";
        final int NumberOfBits = 4;
        final int Dimension = (int) Math.pow(2, NumberOfBits);
        int[] values = Utils.convertU16BytesToInt(Utils.readFileBytes(sourceFile));

//        LloydMaxU16ScalarQuantization quantization = new LloydMaxU16ScalarQuantization(values, NumberOfBits);
//        quantization.train();

        JadeSolver jadeSolver = new JadeSolver(Dimension, 10 * Dimension, 250, 0.05, 0.1);
        jadeSolver.setTrainingData(values);

        DeHistory[] solutionHistory = null;
        try {
            solutionHistory = jadeSolver.train();
        } catch (DeException e) {
            e.printStackTrace();
        }

        if (solutionHistory != null) {
            FileOutputStream os = new FileOutputStream("JadeSolutionHistory.csv");
            OutputStreamWriter writer = new OutputStreamWriter(os);
            writer.write("Generation;AvgCost;BestCost\n");
            for (final DeHistory hist : solutionHistory) {
                writer.write(String.format("%d;%.5f;%.5f\n", hist.getIteration(), hist.getAvgCost(), hist.getBestCost()));
            }
            writer.flush();
            writer.close();
            os.flush();
            os.close();
        }

        System.out.println("Finished learning...");
    }
}
