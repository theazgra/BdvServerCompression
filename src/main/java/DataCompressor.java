import quantization.de.DeException;
import quantization.de.DeHistory;
import quantization.de.IDESolver;
import quantization.de.jade.JadeSolver;
import quantization.de.shade.ILShadeSolver;
import quantization.de.shade.LShadeSolver;
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

        String sourceFile = "D:\\tmp\\server-dump\\initial_load.bin";
        int NumberOfBits = 5;
        String output = "lloyd";

        if (args.length >= 3) {
            sourceFile = args[0];
            NumberOfBits = Integer.parseInt(args[1]);
            output = args[2];
            System.out.println(String.format("Input: %s, #of bits: %d, output to: %s ", sourceFile, NumberOfBits, output));
        }

        final int Dimension = (int) Math.pow(2, NumberOfBits);
        int[] values = Utils.convertU16BytesToInt(Utils.readFileBytes(sourceFile));

        //benchmarkLloydMax(values, outputDir);
        //lloydMax(NumberOfBits, values);
        //jade(Dimension, values);
        //lshade(Dimension, values, output);
        ilshade(values, Dimension, 5 * Dimension, 100, "report.csv");
    }

    private static void benchmarkLloydMax(final int[] values, final String dir) {
        for (int bitCount = 2; bitCount < 9; bitCount++) {
            LloydMaxIteration[] solutionHistory = lloydMax(bitCount, values);
            String fileName = String.format("%s/lloyd_max_%dbits.csv", dir, bitCount);
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

    private static void lshade(final int[] values, final int dimension,
                               final int populationSize, final int generationCount,
                               final String reportFile) {

        startDe(new LShadeSolver(dimension, populationSize, generationCount, 10), values, dimension,
                generationCount, populationSize, reportFile);
    }

    private static void ilshade(final int[] values, final int dimension,
                                final int populationSize, final int generationCount,
                                final String reportFile) {
        startDe(new ILShadeSolver(dimension, populationSize, generationCount, 10), values, dimension,
                generationCount, populationSize, reportFile);
    }

    private static void jade(final int[] values, final int dimension,
                             final int populationSize, final int generationCount,
                             final String reportFile) {
        startDe(new JadeSolver(dimension, populationSize, generationCount), values, dimension,
                generationCount, populationSize, reportFile);
    }

    private static void startDe(IDESolver solver,
                                final int[] values, final int dimension, final int generationCount,
                                final int populationSize, final String reportFile) {
        solver.setTrainingData(values);
        solver.setDimensionCount(dimension);
        solver.setGenerationCount(generationCount);
        try {
            solver.setPopulationSize(populationSize);
            DeHistory[] solution = solver.train();
            saveDESolution(solution, reportFile);
            System.out.println("Finished learning...");
        } catch (DeException e) {
            e.printStackTrace();
        }
    }


    private static void saveDESolution(final DeHistory[] solutionHistory, final String fileName) {

        try {
            FileOutputStream os = new FileOutputStream(fileName);
            OutputStreamWriter writer = new OutputStreamWriter(os);
            writer.write("Generation;AvgCost;BestCost;PSNR;BestPSNR\n");
            for (final DeHistory hist : solutionHistory) {
                writer.write(String.format("%d;%.5f;%.5f;%.5f;%.5f\n",
                        hist.getIteration(),
                        hist.getAvgCost(),
                        hist.getBestCost(),
                        hist.getAvgPsnr(),
                        hist.getBestPsnr()));
            }
            writer.flush();
            writer.close();
            os.flush();
            os.close();


        } catch (final Exception e) {
            e.printStackTrace();
        }
    }


}
