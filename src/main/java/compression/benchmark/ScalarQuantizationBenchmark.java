package compression.benchmark;

import compression.U16;
import compression.data.ImageU16;
import compression.data.V3i;
import compression.de.DeException;
import compression.de.shade.ILShadeSolver;
import compression.io.RawDataIO;
import compression.quantization.QTrainIteration;
import compression.quantization.scalar.LloydMaxU16ScalarQuantization;
import compression.quantization.scalar.ScalarQuantizer;
import compression.utilities.TypeConverter;
import compression.utilities.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class ScalarQuantizationBenchmark {
    private final String inputFile;
    private final String outputDirectory;
    private final int[] planes;
    private boolean useDiffEvolution = false;
    final V3i rawImageDims;

    public ScalarQuantizationBenchmark(final String inputFile,
                                       final String outputDirectory,
                                       final int[] planes,
                                       final V3i rawImageDims) {
        this.inputFile = inputFile;
        this.outputDirectory = outputDirectory;
        this.planes = planes;
        this.rawImageDims = rawImageDims;
    }

    private short[] loadPlaneData(final int planeIndex) {
        try {
            ImageU16 image = RawDataIO.loadImageU16(inputFile, rawImageDims, planeIndex);
            return image.getData();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return new short[0];
    }

    private boolean saveQuantizedPlaneData(final short[] data, final String filename) {
        ImageU16 img = new ImageU16(rawImageDims.getX(), rawImageDims.getY(), data);
        try {
            // NOTE(Moravec): Use big endian so that FIJI can read the image.
            RawDataIO.writeImageU16(getFileNamePath(filename), img, false);
            System.out.println(String.format("Saved %s", filename));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean saveDifference(final short[] originalData,
                                   final short[] transformedData,
                                   final String diffFile,
                                   final String absDiffFile) {

        final int[] differenceData = Utils.getDifference(originalData, transformedData);
        final int[] absDifferenceData = Utils.applyAbsToValues(differenceData);
        final String diffFilePath = getFileNamePath(diffFile);
        final String absDiffFilePath = getFileNamePath(absDiffFile);

        ImageU16 img = new ImageU16(rawImageDims.getX(),
                                    rawImageDims.getY(),
                                    TypeConverter.intArrayToShortArray(absDifferenceData));
        try {
            // NOTE(Moravec): Use little endian so that gnuplot can read the array.
            RawDataIO.writeImageU16(absDiffFilePath, img, true);
            System.out.println("Saved absolute difference to: " + absDiffFilePath);

            RawDataIO.writeDataI32(diffFilePath, differenceData, true);
            System.out.println("Saved difference to: " + absDiffFilePath);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to save difference.");
            return false;
        }
        return true;
    }

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
                final String centroidsFile = getFileNamePath(String.format("p%d_cb%d%s_centroids.raw",
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

    private String getFileNamePath(final String fileName) {
        final File file = new File(outputDirectory, fileName);
        return file.getAbsolutePath();
    }

    private ScalarQuantizer trainLloydMaxQuantizer(final short[] data, final int codebookSize, final int planeIndex) {
        LloydMaxU16ScalarQuantization lloydMax = new LloydMaxU16ScalarQuantization(data, codebookSize);
        QTrainIteration[] trainingReport = lloydMax.train();

        saveQTrainLog(getFileNamePath(String.format("p%d_cb_%d_lloyd.csv", planeIndex, codebookSize)), trainingReport);

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
        saveQTrainLog(getFileNamePath(String.format("p%d_cb_%d_il_shade.csv", planeIndex, codebookSize)),
                      trainingReport);
        return new ScalarQuantizer(U16.Min, U16.Max, ilshade.getBestSolution().getAttributes());
    }

    private void saveQTrainLog(final String filename, final QTrainIteration[] trainingLog) {
        final String CSV_HEADER = "It;AvgMSE;BestMSE;AvgPSNR;BestPSNR\n";
        try {
            FileOutputStream fileStream = new FileOutputStream(filename);
            OutputStreamWriter writer = new OutputStreamWriter(fileStream);

            writer.write(CSV_HEADER);

            for (final QTrainIteration it : trainingLog) {
                writer.write(String.format("%d;%.5f;%.5f;%.5f;%.5f\n",
                                           it.getIteration(),
                                           it.getAverageMSE(),
                                           it.getBestMSE(),
                                           it.getAveragePSNR(),
                                           it.getBestPSNR()));
            }
            writer.flush();
            fileStream.flush();
            fileStream.close();
        } catch (IOException ioE) {
            ioE.printStackTrace();
            System.err.println("Failed to save QTtrain log.");
        }
    }

    public boolean isUseDiffEvolution() {
        return useDiffEvolution;
    }

    public void setUseDiffEvolution(boolean useDiffEvolution) {
        this.useDiffEvolution = useDiffEvolution;
    }
}
