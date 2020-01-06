package compression.benchmark;

import compression.U16;
import compression.data.V3i;
import compression.de.DeException;
import compression.de.shade.ILShadeSolver;
import compression.io.RawDataIO;
import compression.quantization.QTrainIteration;
import compression.quantization.scalar.LloydMaxU16ScalarQuantization;
import compression.quantization.scalar.ScalarQuantizer;
import compression.utilities.TypeConverter;

public class ScalarQuantizationBenchmark extends BenchmarkBase {
    private boolean useDiffEvolution = false;

    public ScalarQuantizationBenchmark(final String inputFile,
                                       final String outputDirectory,
                                       final int[] planes,
                                       final V3i rawImageDims) {
        super(inputFile, outputDirectory, planes, rawImageDims);
    }


    @Override
    public void startBenchmark() {

        for (final int planeIndex : planes) {
            System.out.println(String.format("Loading plane %d ...", planeIndex));
            // NOTE(Moravec): Actual planeIndex is zero based.
            final short[] planeData = loadPlaneData(planeIndex - 1);
            if (planeData.length == 0) {
                System.err.println(String.format("Failed to load plane %d data. Skipping plane.", planeIndex));
                return;
            }

            // Test codebook sizes from 2^2 to 2^8
            for (int bitCount = 2; bitCount <= 8; bitCount++) {
                final int codebookSize = (int) Math.pow(2, bitCount);
                System.out.println(String.format("|CODEBOOK| = %d", codebookSize));

                ScalarQuantizer quantizer = null;
                if (useDiffEvolution) {
                    quantizer = trainDifferentialEvolution(planeData, codebookSize, planeIndex);
                } else {
                    quantizer = trainLloydMaxQuantizer(planeData, codebookSize, planeIndex);
                }
                if (quantizer == null) {
                    System.err.println("Failed to initialize scalar quantizer. Skipping plane.");
                    return;
                }
                System.out.println("Scalar quantizer ready.");

                final String method = useDiffEvolution ? "ilshade" : "lloyd";
                final String centroidsFile = getFileNamePathIntoOutDir(String.format("p%d_cb%d%s_centroids.raw",
                                                                                     (planeIndex + 1),
                                                                                     codebookSize,
                                                                                     method));

                // NOTE(Moravec): Centroids are saved in little endian order.
                if (!RawDataIO.writeDataI32(centroidsFile, quantizer.getCentroids(), true)) {
                    System.err.println("Failed to save quantizer centroids.");
                    return;
                }


                final String quantizedFile = String.format("p%d_cb%d%s.raw", planeIndex, codebookSize, method);
                final String diffFile = String.format("p%d_cb%d%s_diff.raw", planeIndex, codebookSize, method);
                final String absoluteDiffFile = String.format("p%d_cb%d%s_adiff.raw", planeIndex, codebookSize, method);

                final short[] quantizedData = quantizer.quantize(planeData);

                if (!saveQuantizedPlaneData(quantizedData, quantizedFile)) {
                    System.err.println("Failed to save quantized plane.");
                    return;
                }

                saveDifference(planeData, quantizedData, diffFile, absoluteDiffFile);
            }
        }
    }

    private ScalarQuantizer trainLloydMaxQuantizer(final short[] data, final int codebookSize, final int planeIndex) {
        LloydMaxU16ScalarQuantization lloydMax = new LloydMaxU16ScalarQuantization(data, codebookSize);
        QTrainIteration[] trainingReport = lloydMax.train();

        saveQTrainLog(String.format("p%d_cb_%d_lloyd.csv", planeIndex, codebookSize), trainingReport);

        return new ScalarQuantizer(U16.Min, U16.Max, lloydMax.getCentroids());
    }

    private ScalarQuantizer trainDifferentialEvolution(final short[] data,
                                                       final int codebookSize,
                                                       final int planeIndex) {
        ILShadeSolver ilshade = new ILShadeSolver(codebookSize, 100, 2000, 15);
        ilshade.setTrainingData(TypeConverter.shortArrayToIntArray(data));

        QTrainIteration[] trainingReport = null;
        try {
            trainingReport = ilshade.train();
        } catch (DeException deEx) {
            deEx.printStackTrace();
            return null;
        }
        saveQTrainLog(String.format("p%d_cb_%d_il_shade.csv", planeIndex, codebookSize), trainingReport);
        return new ScalarQuantizer(U16.Min, U16.Max, ilshade.getBestSolution().getAttributes());
    }


    public boolean isUseDiffEvolution() {
        return useDiffEvolution;
    }

    public void setUseDiffEvolution(boolean useDiffEvolution) {
        this.useDiffEvolution = useDiffEvolution;
    }
}
