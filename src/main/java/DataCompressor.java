import quantization.de.DeException;
import quantization.de.DeHistory;
import quantization.de.jade.JadeSolver;
import quantization.lloyd_max.LloydMaxIteration;
import quantization.lloyd_max.LloydMaxU16ScalarQuantization;
import quantization.utilities.Utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;


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

        final String sourceFile = "D:\\tmp\\server-dump\\initial_load.bin";
        final int NumberOfBits = 8;
        final int Dimension = (int) Math.pow(2, NumberOfBits);
        int[] values = Utils.convertU16BytesToInt(Utils.readFileBytes(sourceFile));

        //benchmarkLloydMax(values);
        //lloydMax(NumberOfBits, values);
        jade(Dimension, values);
    }

    private static void benchmarkLloydMax(final int[] values) {
        for (int bitCount = 2; bitCount < 9; bitCount++) {
            LloydMaxIteration[] solutionHistory = lloydMax(bitCount, values);
            String fileName = String.format("lloyd_max_%dbits.csv", bitCount);
            saveLloydMaxSolutionHistory(solutionHistory, fileName);
        }
    }

    private static LloydMaxIteration[] lloydMax(final int noOfBits, final int[] values) {
        LloydMaxU16ScalarQuantization quantization = new LloydMaxU16ScalarQuantization(values, noOfBits);
        return quantization.train(false);
    }

    private static void saveLloydMaxSolutionHistory(final LloydMaxIteration[] solutionHistory, String filename) {
        try {
            FileOutputStream os = new FileOutputStream(filename);
            OutputStreamWriter writer = new OutputStreamWriter(os);
            writer.write("Iteration;Mse;Psnr\n");
            for (final LloydMaxIteration lmi : solutionHistory) {
                writer.write(String.format("%d;%.5f;%.5f\n", lmi.getIteration(), lmi.getMse(), lmi.getPsnr()));
            }
            writer.flush();
            writer.close();
            os.flush();
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void jade(final int dimension, final int[] values) throws IOException {
        JadeSolver jadeSolver = new JadeSolver(dimension, 5 * dimension, 1000, 0.05, 0.1);
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
