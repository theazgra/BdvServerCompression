package azgracompress.benchmark;

import azgracompress.U16;
import azgracompress.cache.QuantizationCacheManager;
import azgracompress.cli.ParsedCliOptions;
import azgracompress.io.IPlaneLoader;
import azgracompress.io.PlaneLoaderFactory;
import azgracompress.quantization.QTrainIteration;
import azgracompress.quantization.scalar.LloydMaxU16ScalarQuantization;
import azgracompress.quantization.scalar.SQCodebook;
import azgracompress.quantization.scalar.ScalarQuantizer;
import azgracompress.utilities.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class SQBenchmark extends BenchmarkBase {
    public SQBenchmark(final ParsedCliOptions options) {
        super(options);
    }


    @Override
    public void startBenchmark() {
        IPlaneLoader planeLoader;
        try {
            planeLoader = PlaneLoaderFactory.getPlaneLoaderForInputFile(options.getInputDataInfo());
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
            QuantizationCacheManager cacheManager = new QuantizationCacheManager(cacheFolder);
            final SQCodebook codebook = cacheManager.loadSQCodebook(inputFile, codebookSize);

            if (codebook == null) {
                System.err.println("Failed to read quantization values from cache file.");
                return;
            }

            quantizer = new ScalarQuantizer(codebook);
            System.out.println("Created quantizer from cache");
        } else if (useMiddlePlane) {
            final int middlePlaneIndex = rawImageDims.getZ() / 2;

            final int[] middlePlaneData;
            try {
                middlePlaneData = planeLoader.loadPlaneU16(middlePlaneIndex).getData();
            } catch (IOException e) {
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

            final String quantizedFile = String.format(QUANTIZED_FILE_TEMPLATE, planeIndex, codebookSize);
            final String diffFile = String.format(DIFFERENCE_FILE_TEMPLATE, planeIndex, codebookSize);
            final String absoluteDiffFile = String.format(ABSOLUTE_DIFFERENCE_FILE_TEMPLATE,
                    planeIndex,
                    codebookSize);
            final String trainLogFile = String.format(TRAIN_FILE_TEMPLATE, planeIndex, codebookSize);

            if (!hasGeneralQuantizer) {
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

    private ScalarQuantizer trainLloydMaxQuantizer(final int[] data,
                                                   final int codebookSize,
                                                   final String trainLogFile) {
        LloydMaxU16ScalarQuantization lloydMax = new LloydMaxU16ScalarQuantization(data, codebookSize, workerCount);
        QTrainIteration[] trainingReport = lloydMax.train(false);
        if (trainLogFile != null) {
            saveQTrainLog(trainLogFile, trainingReport);
            System.out.println("Saved the train log file to: " + trainLogFile);
        }
        return new ScalarQuantizer(U16.Min, U16.Max, lloydMax.getCodebook());
    }
}
