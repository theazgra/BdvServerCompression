import compression.de.DeException;
import compression.de.DeHistory;
import compression.de.IDESolver;
import compression.de.jade.JadeSolver;
import compression.de.shade.ILShadeSolver;
import compression.de.shade.LShadeSolver;
import compression.quantization.QuantizationValueCache;
import compression.quantization.scalar.LloydMaxIteration;
import compression.quantization.scalar.LloydMaxU16ScalarQuantization;
import compression.quantization.vector.LBGVectorQuantizer;
import compression.utilities.Utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class DataCompressor {
    public static void main(String[] args) throws IOException {

//        QuantizationValueCache cache = new QuantizationValueCache("D:\\tmp\\bdv_cache");
//        int[] centroids = new int[]{0, 125, 355, 400, 500, 700, 2550, 4000, 10000, 50000, 0xffff};
//        cache.saveQuantizationValue("test.bin", centroids);
//        int[] read = cache.readCachedValues("test.bin", centroids.length);

        String sourceFile = "D:\\tmp\\server-dump\\initial_load.bin";
        int NumberOfBits = 8;
        String output = "lloyd";

        final int Dimension = (int) Math.pow(2, NumberOfBits);
        int[] trainValues = Utils.convertU16ByteArrayToIntArray(Utils.readFileBytes(sourceFile));
        int[] part;
        {
//            int[] shuffled = null;
//            List<Integer> trainValueList = IntStream.of(trainValues).boxed().collect(Collectors.toList());
//            Collections.shuffle(trainValueList);
//            Integer[] tmp = new Integer[trainValues.length];
//            trainValueList.toArray(tmp);
//            shuffled = ArrayUtils.toPrimitive(tmp);
//            final int partSize = shuffled.length / 2;
//            part = Arrays.copyOf(shuffled, partSize);
            final int partSize = trainValues.length / 2;
            part = Arrays.copyOf(trainValues, partSize);
        }


        LBGVectorQuantizer vq = new LBGVectorQuantizer(part, Dimension, 4, 1);
        //LBGVectorQuantizer vq = new LBGVectorQuantizer(part, Dimension, 4, 1);
        vq.findOptimalCodebook();

        //benchmarkLloydMax(values, output);
        //lloydMax(NumberOfBits, values);
        //lshade(Dimension, values, output);
        //jade(values, Dimension, 5 * Dimension, 500, "JADE-5bits.csv");
        //lshade(values, Dimension, 5 * Dimension, 1000, output);
        //ilshade(values, Dimension, 100, 800, "iL-SHADE-2bits-800it.csv");
    }

    private static void benchmarkLloydMax(final int[] values, final String dir) {
        for (int bitCount = 8; bitCount < 9; bitCount++) {
            LloydMaxIteration[] solutionHistory = lloydMax(bitCount, values, dir);
            String fileName = String.format("%s/lloyd_max_%dbits.csv", dir, bitCount);
            saveLloydMaxSolutionHistory(solutionHistory, fileName);
        }
    }

    private static LloydMaxIteration[] lloydMax(final int noOfBits, final int[] values, final String dir) {
        LloydMaxU16ScalarQuantization quantization = new LloydMaxU16ScalarQuantization(values, noOfBits);
        LloydMaxIteration[] trainHistory = quantization.train(false);

        short[] quantized = quantization.quantizeToShortArray(values);
        byte[] buffer = new byte[quantized.length * 2];

        for (int i = 0, j = 0; i < quantized.length; i++) {
            final short s = quantized[i];
            buffer[j++] = (byte) ((s >> 8) & 0xff);
            buffer[j++] = (byte) (s & 0xff);
        }

        FileOutputStream dumpStream = null;
        try {
            dumpStream = new FileOutputStream(String.format("%s/quantizedValues%d.data", dir, noOfBits), true);
            dumpStream.write(buffer);
            dumpStream.flush();
            dumpStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


        return trainHistory;
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
