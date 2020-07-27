package azgracompress.benchmark;

import azgracompress.U16;
import azgracompress.cache.QuantizationCacheManager;
import azgracompress.cli.ParsedCliOptions;
import azgracompress.compression.CompressionOptions;
import azgracompress.data.*;
import azgracompress.io.loader.IPlaneLoader;
import azgracompress.io.loader.PlaneLoaderFactory;
import azgracompress.quantization.vector.LBGResult;
import azgracompress.quantization.vector.LBGVectorQuantizer;
import azgracompress.quantization.vector.VQCodebook;
import azgracompress.quantization.vector.VectorQuantizer;
import azgracompress.utilities.Utils;

import java.io.File;
import java.io.IOException;

public class VQBenchmark extends BenchmarkBase {

    final static V2i DEFAULT_QVECTOR = new V2i(3, 3);

    public VQBenchmark(final ParsedCliOptions options) {
        super(options);
    }

    @Override
    public void startBenchmark() {
        startBenchmark(DEFAULT_QVECTOR);
    }

    private ImageU16 reconstructImageFromQuantizedVectors(final ImageU16 plane,
                                                          final int[][] vectors,
                                                          final V2i qVector) {
        Chunk2D reconstructedChunk = new Chunk2D(new V2i(rawImageDims.getX(), rawImageDims.getY()));
        if (qVector.getY() > 1) {
            reconstructedChunk.reconstructFrom2DVectors(vectors, qVector);
        } else {
            // 1D vector
            reconstructedChunk.reconstructFromVectors(vectors);
        }
        return reconstructedChunk.asImageU16();
    }

    private int[][] getPlaneVectors(final ImageU16 plane, final V2i qVector) {
        return plane.toQuantizationVectors(qVector);
    }

    public void startBenchmark(final V2i qVector) {
        if (planes.length < 1) {
            return;
        }
        IPlaneLoader planeLoader;
        try {
            planeLoader = PlaneLoaderFactory.getPlaneLoaderForInputFile(options.getInputDataInfo());
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Unable to create SCIFIO reader.");
            return;
        }
        if (qVector.getY() > 1) {
            System.out.println("2D qVector");
        } else {
            System.out.println("1D qVector");
        }
        boolean dirCreated = new File(this.outputDirectory).mkdirs();
        System.out.println(String.format("|CODEBOOK| = %d", codebookSize));
        VectorQuantizer quantizer = null;

        if (options.getCodebookType() == CompressionOptions.CodebookType.Global) {
            System.out.println("Loading codebook from cache");
            QuantizationCacheManager cacheManager = new QuantizationCacheManager(cacheFolder);
            final VQCodebook codebook = cacheManager.loadVQCodebook(inputFile, codebookSize, qVector.toV3i());
            if (codebook == null) {
                System.err.println("Failed to read quantization vectors from cache.");
                return;
            }
            quantizer = new VectorQuantizer(codebook);
            System.out.println("Created quantizer from cache");

        } else if (options.getCodebookType() == CompressionOptions.CodebookType.MiddlePlane) {
            final int middlePlaneIndex = rawImageDims.getZ() / 2;
            final ImageU16 middlePlane;
            try {
                middlePlane = planeLoader.loadPlaneU16(middlePlaneIndex);
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Failed to load middle plane data.");
                return;
            }

            final int[][] refPlaneData = getPlaneVectors(middlePlane, qVector);
            LBGVectorQuantizer vqInitializer = new LBGVectorQuantizer(refPlaneData,
                    codebookSize,
                    workerCount,
                    qVector.toV3i());
            final LBGResult vqResult = vqInitializer.findOptimalCodebook();
            quantizer = new VectorQuantizer(vqResult.getCodebook());
            System.out.println("Created quantizer from middle plane.");
        }

        for (final int planeIndex : planes) {
            System.out.println(String.format("Loading plane %d ...", planeIndex));

            final ImageU16 plane;
            try {
                plane = planeLoader.loadPlaneU16(planeIndex);
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println(String.format("Failed to load plane %d data. Skipping plane.", planeIndex));
                return;
            }

            final int[][] planeData = getPlaneVectors(plane, qVector);


            if (options.getCodebookType() == CompressionOptions.CodebookType.Individual) {
                LBGVectorQuantizer vqInitializer = new LBGVectorQuantizer(planeData,
                        codebookSize,
                        workerCount,
                        qVector.toV3i());
                LBGResult vqResult = vqInitializer.findOptimalCodebook();
                quantizer = new VectorQuantizer(vqResult.getCodebook());
                System.out.println("Created plane quantizer.");
            }

            final String quantizedFile = String.format(QUANTIZED_FILE_TEMPLATE, planeIndex, codebookSize);
            final String diffFile = String.format(DIFFERENCE_FILE_TEMPLATE, planeIndex, codebookSize);
            final String absoluteDiffFile = String.format(ABSOLUTE_DIFFERENCE_FILE_TEMPLATE,
                    planeIndex,
                    codebookSize);

            final int[][] quantizedData = quantizer.quantize(planeData, workerCount);

            final ImageU16 quantizedImage = reconstructImageFromQuantizedVectors(plane, quantizedData, qVector);


            final int[] diffArray = Utils.getDifference(plane.getData(), quantizedImage.getData());
            final double mse = Utils.calculateMse(diffArray);
            final double PSNR = Utils.calculatePsnr(mse, U16.Max);
            System.out.println(String.format("MSE: %.4f\tPSNR: %.4f(dB)", mse, PSNR));

            if (!saveQuantizedPlaneData(quantizedImage.getData(), quantizedFile)) {
                System.err.println("Failed to save quantized plane.");
                return;
            }

            saveDifference(diffArray, diffFile, absoluteDiffFile);
        }
    }
}
