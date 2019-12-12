import compression.data.Chunk3D;
import compression.data.V3i;
import compression.data.V3l;

import java.io.IOException;
import java.util.Random;


public class DataCompressor {
    static int[] getRandomData(int len) {
        Random r = new Random();
        int[] data = new int[len];
        for (int i = 0; i < data.length; i++) {
            data[i] = r.nextInt(20000);
        }
        return data;
    }

    public static void main(String[] args) throws IOException {

//        ScalarQuantizationBenchmark sqBenchmark = new ScalarQuantizationBenchmark(
//                "D:\\biology\\tiff_data\\fused_tp_10_ch_1_16bit.raw",
//                "D:\\biology\\benchmark\\scalar",
//                358,
//                358,
//                new V3i(1041, 996, 946));
//
//        sqBenchmark.setUseDiffEvolution(true);
//
//        sqBenchmark.startBenchmark();


        //Chunk3D[] loadedChunks = ChunkIO.loadChunks("D:\\tmp\\server-dump\\chunks.bin");

        final int xs = 11;
        final int ys = 16;
        final int zs = 16;
        final int[] data = getRandomData(xs * ys * zs);
        final Chunk3D src = new Chunk3D(new V3i(xs, ys, zs), new V3l(0), data);
        final Chunk3D[] chunks = src.divideIntoChunks(new V3i(4));

        final Chunk3D reconstructed = new Chunk3D(new V3i(xs, ys, zs), new V3l(0));
        reconstructed.reconstructFromChunks(chunks);

        if (src.equals(reconstructed)) {
            System.out.println("Reconstruction successful.");
        } else {
            System.out.println("Reconstruction failed !!!");
        }
        /*

        if (chunks.length > 0) {
            return;
        }

        String sourceFile = "D:\\tmp\\server-dump\\initial_load.bin";
        int NumberOfBits = 3;
        String output = "lloyd";

        final int Dimension = (int) Math.pow(2, NumberOfBits);
        int[] trainValues = Utils.convertU16ByteArrayToIntArray(Utils.readFileBytes(sourceFile));
        int[] part;
        {
            final int partSize = trainValues.length / 2;
            part = Arrays.copyOf(trainValues, partSize);
        }


//        LBGVectorQuantizer vq = new LBGVectorQuantizer(trainValues, Dimension, 4, 1);
//        vq.findOptimalCodebook();

        benchmarkLBG(trainValues, "LBG_VectorQuantizerVec2.csv");

        //benchmarkLloydMax(values, output);
        //lloydMax(NumberOfBits, values);
        //lshade(Dimension, values, output);
        //jade(values, Dimension, 5 * Dimension, 500, "JADE-5bits.csv");
        //lshade(values, Dimension, 5 * Dimension, 1000, output);
        //ilshade(values, Dimension, 100, 800, "iL-SHADE-2bits-800it.csv");
        */
    }
    /*
    private static void appendLineToFile(final String fileName, final String line) {
        try {
            FileOutputStream os = new FileOutputStream(fileName, true);
            OutputStreamWriter writer = new OutputStreamWriter(os);
            writer.write(line);
            writer.write('\n');
            writer.flush();
            writer.close();
            os.flush();
            os.close();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    private static void benchmarkLBG(final int[] values, final String fileName) {

        //TODO: Try different vector sizes, maybe 2 then try different shapes, like box etc.
        appendLineToFile(fileName, "CodebookSize;MSE;PSNR");

        for (int bitCount = 2; bitCount < 8; bitCount++) {
            final int codebookSize = (int) (Math.pow(2, bitCount));
            System.out.println("Testing vector quantizer with codebook size of " + codebookSize);
            LBGVectorQuantizer lbg = new LBGVectorQuantizer(values, codebookSize, 2, 1);
            final LBGResult result = lbg.findOptimalCodebook();

            appendLineToFile(fileName, String.format("%d;%.5f;%.5f\n", result.getCodebookSize(),
                    result.getAverageMse(), result.getPsnr()));
        }
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

     */
}
