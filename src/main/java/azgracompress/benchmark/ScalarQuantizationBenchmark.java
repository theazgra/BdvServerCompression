package azgracompress.benchmark;

import azgracompress.U16;
import azgracompress.cli.ParsedCliOptions;
import azgracompress.de.DeException;
import azgracompress.de.shade.ILShadeSolver;
import azgracompress.io.ConcretePlaneLoader;
import azgracompress.quantization.QTrainIteration;
import azgracompress.quantization.QuantizationValueCache;
import azgracompress.quantization.scalar.LloydMaxU16ScalarQuantization;
import azgracompress.quantization.scalar.ScalarQuantizer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class ScalarQuantizationBenchmark extends BenchmarkBase {
    private boolean useDiffEvolution = false;

    public ScalarQuantizationBenchmark(final ParsedCliOptions options) {
        super(options);
    }


    @Override
    public void startBenchmark() {
        ConcretePlaneLoader planeLoader = null;
        try {
            planeLoader = new ConcretePlaneLoader(options.getInputFileInfo());
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Unable to create SCIFIO reader.");
            return;
        }

        if (planes.length < 1) {
            return;
        }
        boolean dirCreated = new File(this.outputDirectory).mkdirs();
        System.out.println(String.format("|CODEBOOK| = %d", codebookSize));
        ScalarQuantizer quantizer = null;

        if (hasCacheFolder) {
            System.out.println("Loading codebook from cache");
            QuantizationValueCache cache = new QuantizationValueCache(cacheFolder);
            try {
                final int[] quantizationValues = cache.readCachedValues(inputFile, codebookSize);
                quantizer = new ScalarQuantizer(U16.Min, U16.Max, quantizationValues);
            } catch (IOException e) {
                System.err.println("Failed to read quantization values from cache file.");
                e.printStackTrace();
                return;
            }
            System.out.println("Created quantizer from cache");
        } else if (hasReferencePlane) {
            final int[] refPlaneData;

            try {
                refPlaneData = planeLoader.loadPlaneU16(referencePlaneIndex).getData();
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Failed to load reference plane data.");
                return;
            }

            if (useDiffEvolution) {
                quantizer = trainDifferentialEvolution(refPlaneData, codebookSize);
            } else {
                quantizer = trainLloydMaxQuantizer(refPlaneData, codebookSize);
            }
            System.out.println("Created reference quantizer.");
        }

        for (final int planeIndex : planes) {
            System.out.println(String.format("Loading plane %d ...", planeIndex));
            // NOTE(Moravec): Actual planeIndex is zero based.
            final int[] planeData;
            try {
                planeData = planeLoader.loadPlaneU16(planeIndex).getData();
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Failed to load plane data.");
                return;
            }
            if (planeData.length == 0) {
                System.err.println(String.format("Failed to load plane %d data. Skipping plane.", planeIndex));
                return;
            }


            if (!hasGeneralQuantizer) {
                if (useDiffEvolution) {
                    quantizer = trainDifferentialEvolution(planeData, codebookSize);
                } else {
                    quantizer = trainLloydMaxQuantizer(planeData, codebookSize);
                }
                System.out.println("Created plane quantizer");
            }
            if (quantizer == null) {
                System.err.println("Failed to initialize scalar quantizer.");
                return;
            }


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
        LloydMaxU16ScalarQuantization lloydMax = new LloydMaxU16ScalarQuantization(data, codebookSize, workerCount);
        QTrainIteration[] trainingReport = lloydMax.train(false);

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
