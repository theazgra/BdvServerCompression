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
import compression.utilities.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class ScalarQuantizationBenchmark {
    private final String inputFile;
    private final String outputDirectory;
    private final int fromPlaneIndex;
    private final int toPlaneIndex;
    private boolean useDiffEvolution = false;
    final V3i rawImageDims;

    public ScalarQuantizationBenchmark(final String inputFile, final String outputDirectory,
                                       final int fromPlaneIndex, final int toPlaneIndex,
                                       final V3i rawImageDims) {
        this.inputFile = inputFile;
        this.outputDirectory = outputDirectory;
        this.fromPlaneIndex = fromPlaneIndex;
        this.toPlaneIndex = toPlaneIndex;
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
            RawDataIO.writeImageU16(getFileNamePath(filename), img);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean saveDifference(final short[] originalData, final short[] transformedData, final String filename) {
        final int[] differenceData = Utils.getAbsoluteDifference(originalData, transformedData);

        try {
            RawDataIO.writeDataI32(getFileNamePath(filename), differenceData);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void startBenchmark() {

        // Test codebook sizes from 2^2 to 2^8
        for (int bitCount = 2; bitCount <= 8; bitCount++) {
            final int codebookSize = (int) Math.pow(2, bitCount);
            System.out.println(String.format("Starting benchmark for codebook of size %d", codebookSize));

            for (int planeIndex = fromPlaneIndex; planeIndex <= toPlaneIndex; planeIndex++) {
                System.out.println(String.format("Loading plane %d ...", planeIndex));

                final short[] planeData = loadPlaneData(planeIndex);
                if (planeData.length == 0) {
                    System.err.println(String.format("Failed to load plane %d data. Skipping plane.", planeIndex));
                    continue;
                }

                ScalarQuantizer quantizer = null;
                if (useDiffEvolution) {
                    quantizer = trainDifferentialEvolution(planeData, codebookSize, planeIndex);
                } else {
                    quantizer = trainLloydMaxQuantizer(planeData, codebookSize, planeIndex);
                }
                if (quantizer == null) {
                    System.err.println("Failed to initialize scalar quantizer. Skipping plane.");
                    continue;
                }
                System.out.println("Scalar quantizer is initialized...");

                final String method = useDiffEvolution ? "ilshade" : "lloyd";
                final String quantizedFile = String.format("quantized_%s_plane_%d_cb_%d.raw", method, planeIndex, codebookSize);
                final String absoluteDiffFile = String.format("absolute_%s_plane_%d_cb_%d.raw", method, planeIndex, codebookSize);

                final short[] quantizedData = quantizer.quantize(planeData);
                if (saveQuantizedPlaneData(quantizedData, quantizedFile)) {
                    System.out.println(String.format("Quantized plane %d data and wrote to file...", planeIndex));
                } else {
                    System.err.println("Failed to save quantized plane.");
                }

                if (saveDifference(planeData, quantizedData, absoluteDiffFile)) {
                    System.out.println("Saved difference.");
                } else {
                    System.err.println("Failed to save difference.s");
                }


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

        saveQTrainLog(getFileNamePath(String.format("lloyd_max_plane_%d_CB_%d.csv", planeIndex, codebookSize)), trainingReport);

        return new ScalarQuantizer(U16.Min, U16.Max, lloydMax.getCentroids());
    }

    private ScalarQuantizer trainDifferentialEvolution(final short[] data, final int codebookSize, final int planeIndex) {
        ILShadeSolver ilshade = new ILShadeSolver(codebookSize, 100, 2000, 15);
        ilshade.setTrainingData(Utils.convertShortArrayToIntArray(data));

        QTrainIteration[] trainingReport = null;
        try {
            trainingReport = ilshade.train();
        } catch (DeException deEx) {
            deEx.printStackTrace();
            return null;
        }
        saveQTrainLog(getFileNamePath(String.format("il_shade_plane_%d_CB_%d.csv", planeIndex, codebookSize)), trainingReport);
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
