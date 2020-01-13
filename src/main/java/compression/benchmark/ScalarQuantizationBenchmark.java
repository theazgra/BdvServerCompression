package compression.benchmark;

import compression.U16;
import compression.data.V3i;
import compression.de.DeException;
import compression.de.shade.ILShadeSolver;
import compression.quantization.QTrainIteration;
import compression.quantization.scalar.LloydMaxU16ScalarQuantization;
import compression.quantization.scalar.ScalarQuantizer;
import compression.utilities.TypeConverter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

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
        boolean dirCreated = new File(this.outputDirectory).mkdirs();
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
                final String centroidsFile = String.format("p%d_cb%d%s_centroids.txt",
                                                           planeIndex,
                                                           codebookSize,
                                                           method);

                saveCentroids(quantizer.getCentroids(), centroidsFile);


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

    private void saveCentroids(final int[] centroids, final String centroidsFile) {
        final String outFile = getFileNamePathIntoOutDir(centroidsFile);
        try {
            FileOutputStream fileStream = new FileOutputStream(outFile);
            OutputStreamWriter writer = new OutputStreamWriter(fileStream);

            StringBuilder sb = new StringBuilder();


            for (final var entry : centroids) {
                sb.append(entry);
                sb.append('\n');
            }

            writer.write(sb.toString());

            writer.flush();
            fileStream.flush();
            fileStream.close();
        } catch (IOException ioE) {
            ioE.printStackTrace();
            System.err.println("Failed to save codebook vectors.");
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
