package cz.it4i.qcmp.benchmark;

import cz.it4i.qcmp.U16;
import cz.it4i.qcmp.cache.QuantizationCacheManager;
import cz.it4i.qcmp.cli.CompressionOptionsCLIParser;
import cz.it4i.qcmp.compression.CompressionOptions;
import cz.it4i.qcmp.io.loader.IPlaneLoader;
import cz.it4i.qcmp.io.loader.PlaneLoaderFactory;
import cz.it4i.qcmp.quantization.QTrainIteration;
import cz.it4i.qcmp.quantization.scalar.LloydMaxU16ScalarQuantization;
import cz.it4i.qcmp.quantization.scalar.SQCodebook;
import cz.it4i.qcmp.quantization.scalar.ScalarQuantizer;
import cz.it4i.qcmp.utilities.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class SQBenchmark extends BenchmarkBase {
    public SQBenchmark(final CompressionOptionsCLIParser options) {
        super(options);
    }


    @Override
    public void startBenchmark() {
        final IPlaneLoader planeLoader;
        try {
            planeLoader = PlaneLoaderFactory.getPlaneLoaderForInputFile(options.getInputDataInfo());
        } catch (final Exception e) {
            e.printStackTrace();
            System.err.println("Unable to create SCIFIO reader.");
            return;
        }

        if (planes.length < 1) {
            return;
        }
        final boolean dirCreated = new File(this.outputDirectory).mkdirs();
        System.out.println(String.format("|CODEBOOK| = %d", codebookSize));
        ScalarQuantizer quantizer = null;

        if (options.getCodebookType() == CompressionOptions.CodebookType.Global) {
            System.out.println("Loading codebook from cache");
            final QuantizationCacheManager cacheManager = new QuantizationCacheManager(cacheFolder);
            final SQCodebook codebook = cacheManager.loadSQCodebook(inputFile, codebookSize);

            if (codebook == null) {
                System.err.println("Failed to read quantization values from cache file.");
                return;
            }

            quantizer = new ScalarQuantizer(codebook);
            System.out.println("Created quantizer from cache");
        } else if (options.getCodebookType() == CompressionOptions.CodebookType.MiddlePlane) {
            final int middlePlaneIndex = rawImageDims.getZ() / 2;

            final int[] middlePlaneData;
            try {
                middlePlaneData = planeLoader.loadPlaneData(middlePlaneIndex);
            } catch (final IOException e) {
                e.printStackTrace();
                System.err.println("Failed to load middle plane data.");
                return;
            }
            quantizer = trainLloydMaxQuantizer(middlePlaneData, codebookSize, null);
            System.out.println("Created quantizer from middle plane.");
        }

        for (final int planeIndex : planes) {
            System.out.println(String.format("Loading plane %d ...", planeIndex));
            // NOTE(Moravec): Actual planeIndex is zero based.
            final int[] planeData;
            try {
                planeData = planeLoader.loadPlaneData(planeIndex);
            } catch (final IOException e) {
                e.printStackTrace();
                System.err.println("Failed to load plane data.");
                return;
            }
            if (planeData.length == 0) {
                System.err.println(String.format("Failed to load plane %d data. Skipping plane.", planeIndex));
                return;
            }

            final String quantizedFile = String.format(QUANTIZED_FILE_TEMPLATE, planeIndex, codebookSize);
            final String diffFile = String.format(DIFFERENCE_FILE_TEMPLATE, planeIndex, codebookSize);
            final String absoluteDiffFile = String.format(ABSOLUTE_DIFFERENCE_FILE_TEMPLATE,
                                                          planeIndex,
                                                          codebookSize);
            final String trainLogFile = String.format(TRAIN_FILE_TEMPLATE, planeIndex, codebookSize);

            if (options.getCodebookType() == CompressionOptions.CodebookType.Individual) {
                quantizer = trainLloydMaxQuantizer(planeData, codebookSize, trainLogFile);
                System.out.println("Created plane quantizer");
            }

            if (quantizer == null) {
                System.err.println("Failed to initialize scalar quantizer.");
                return;
            }

            final int[] quantizedData = quantizer.quantize(planeData);

            final int[] diffArray = Utils.getDifference(planeData, quantizedData);
            final double mse = Utils.calculateMse(diffArray);
            final double PSNR = Utils.calculatePsnr(mse, U16.Max);
            System.out.println(String.format("MSE: %.4f\tPSNR: %.4f(dB)", mse, PSNR));


            if (!saveQuantizedPlaneData(quantizedData, quantizedFile)) {
                System.err.println("Failed to save quantized plane.");
                return;
            }

            saveDifference(diffArray, diffFile, absoluteDiffFile);
        }
    }

    private void saveCentroids(final int[] centroids, final String centroidsFile) {
        final String outFile = getFileNamePathIntoOutDir(centroidsFile);
        try {
            final FileOutputStream fileStream = new FileOutputStream(outFile);
            final OutputStreamWriter writer = new OutputStreamWriter(fileStream);

            final StringBuilder sb = new StringBuilder();


            for (final int entry : centroids) {
                sb.append(entry);
                sb.append('\n');
            }

            writer.write(sb.toString());

            writer.flush();
            fileStream.flush();
            fileStream.close();
        } catch (final IOException ioE) {
            ioE.printStackTrace();
            System.err.println("Failed to save codebook vectors.");
        }
    }

    private ScalarQuantizer trainLloydMaxQuantizer(final int[] data,
                                                   final int codebookSize,
                                                   final String trainLogFile) {
        final LloydMaxU16ScalarQuantization lloydMax = new LloydMaxU16ScalarQuantization(data, codebookSize, workerCount);
        final QTrainIteration[] trainingReport = lloydMax.train();
        if (trainLogFile != null) {
            saveQTrainLog(trainLogFile, trainingReport);
            System.out.println("Saved the train log file to: " + trainLogFile);
        }
        return new ScalarQuantizer(U16.Min, U16.Max, lloydMax.getCodebook());
    }
}
