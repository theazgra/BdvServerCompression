package azgracompress.benchmark;

import azgracompress.U16;
import azgracompress.cli.ParsedCliOptions;
import azgracompress.data.V3i;
import azgracompress.de.DeException;
import azgracompress.de.shade.ILShadeSolver;
import azgracompress.quantization.QTrainIteration;
import azgracompress.quantization.scalar.LloydMaxU16ScalarQuantization;
import azgracompress.quantization.scalar.ScalarQuantizer;

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

    public ScalarQuantizationBenchmark(final ParsedCliOptions options) {
        super(options);
    }


    @Override
    public void startBenchmark() {
        if (planes.length < 1) {
            return;
        }
        boolean dirCreated = new File(this.outputDirectory).mkdirs();

        ScalarQuantizer quantizer = null;
        if (hasReferencePlane) {
            final int[] refPlaneData = loadPlaneData(referencePlaneIndex);
            if (refPlaneData.length == 0) {
                System.err.println("Failed to load reference plane data.");
                return;
            }
            if (useDiffEvolution) {
                quantizer = trainDifferentialEvolution(refPlaneData, codebookSize);
            } else {
                quantizer = trainLloydMaxQuantizer(refPlaneData, codebookSize);
            }
        }

        for (final int planeIndex : planes) {
            System.out.println(String.format("Loading plane %d ...", planeIndex));
            // NOTE(Moravec): Actual planeIndex is zero based.
            final int[] planeData = loadPlaneData(planeIndex);
            if (planeData.length == 0) {
                System.err.println(String.format("Failed to load plane %d data. Skipping plane.", planeIndex));
                return;
            }
            System.out.println(String.format("|CODEBOOK| = %d", codebookSize));

            if (!hasReferencePlane) {
                if (useDiffEvolution) {
                    quantizer = trainDifferentialEvolution(planeData, codebookSize);
                } else {
                    quantizer = trainLloydMaxQuantizer(planeData, codebookSize);
                }
            }
            if (quantizer == null) {
                System.err.println("Failed to initialize scalar quantizer.");
                return;
            }
            System.out.println("Scalar quantizer ready.");

            final String quantizedFile = String.format(QUANTIZED_FILE_TEMPLATE, planeIndex, codebookSize);
            final String diffFile = String.format(DIFFERENCE_FILE_TEMPLATE, planeIndex, codebookSize);
            final String absoluteDiffFile = String.format(ABSOLUTE_DIFFERENCE_FILE_TEMPLATE,
                                                          planeIndex,
                                                          codebookSize);

            final int[] quantizedData = quantizer.quantize(planeData);

            if (!saveQuantizedPlaneData(quantizedData, quantizedFile)) {
                System.err.println("Failed to save quantized plane.");
                return;
            }

            saveDifference(planeData, quantizedData, diffFile, absoluteDiffFile);
        }
    }

    private void saveCentroids(final int[] centroids, final String centroidsFile) {
        final String outFile = getFileNamePathIntoOutDir(centroidsFile);
        try {
            FileOutputStream fileStream = new FileOutputStream(outFile);
            OutputStreamWriter writer = new OutputStreamWriter(fileStream);

            StringBuilder sb = new StringBuilder();


            for (final int entry : centroids) {
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

    private ScalarQuantizer trainLloydMaxQuantizer(final int[] data, final int codebookSize) {
        LloydMaxU16ScalarQuantization lloydMax = new LloydMaxU16ScalarQuantization(data, codebookSize);
        QTrainIteration[] trainingReport = lloydMax.train(true);

        //saveQTrainLog(String.format("p%d_cb_%d_lloyd.csv", planeIndex, codebookSize), trainingReport);

        return new ScalarQuantizer(U16.Min, U16.Max, lloydMax.getCentroids());
    }

    private ScalarQuantizer trainDifferentialEvolution(final int[] data,
                                                       final int codebookSize) {
        ILShadeSolver ilshade = new ILShadeSolver(codebookSize, 100, 2000, 15);
        ilshade.setTrainingData(data);

        try {
            ilshade.train();
        } catch (DeException deEx) {
            deEx.printStackTrace();
            return null;
        }
        return new ScalarQuantizer(U16.Min, U16.Max, ilshade.getBestSolution().getAttributes());
    }


    public boolean isUseDiffEvolution() {
        return useDiffEvolution;
    }

    public void setUseDiffEvolution(boolean useDiffEvolution) {
        this.useDiffEvolution = useDiffEvolution;
    }
}
