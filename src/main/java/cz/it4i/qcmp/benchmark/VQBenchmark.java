package cz.it4i.qcmp.benchmark;

import cz.it4i.qcmp.cli.CompressionOptionsCLIParser;
import cz.it4i.qcmp.data.Block;
import cz.it4i.qcmp.data.ImageU16;
import cz.it4i.qcmp.data.V2i;
import cz.it4i.qcmp.data.V3i;

public class VQBenchmark extends BenchmarkBase {

    public VQBenchmark(final CompressionOptionsCLIParser options) {
        super(options);
    }

    @Override
    public void startBenchmark() {
        startBenchmark(options.getQuantizationVector());
    }

    private ImageU16 reconstructImageFromQuantizedVectors(final int[][] vectors,
                                                          final V2i qVector) {
        final Block reconstructedChunk = new Block(new V2i(rawImageDims.getX(), rawImageDims.getY()));
        if (qVector.getY() > 1) {
            reconstructedChunk.reconstructFrom2DVectors(vectors, qVector);
        } else {
            // 1D vector
            reconstructedChunk.reconstructFromVectors(vectors);
        }
        return reconstructedChunk.asImageU16();
    }

    public void startBenchmark(final V3i qVector) {
        // NOTE(Moravec): This will be enabled once we need to benchmark something.
        //        if (planes.length < 1) {
        //            return;
        //        }
        //        IPlaneLoader planeLoader;
        //        try {
        //            planeLoader = PlaneLoaderFactory.getPlaneLoaderForInputFile(options.getInputDataInfo());
        //        } catch (Exception e) {
        //            e.printStackTrace();
        //            System.err.println("Unable to create specific reader.");
        //            return;
        //        }
        //        if (qVector.getY() > 1) {
        //            System.out.println("2D qVector");
        //        } else {
        //            System.out.println("1D qVector");
        //        }
        //        boolean dirCreated = new File(this.outputDirectory).mkdirs();
        //        System.out.printf("|CODEBOOK| = %d%n", codebookSize);
        //        VectorQuantizer quantizer = null;
        //
        //        if (options.getCodebookType() == CompressionOptions.CodebookType.Global) {
        //            System.out.println("Loading codebook from cache");
        //            QuantizationCacheManager cacheManager = new QuantizationCacheManager(cacheFolder);
        //            final VQCodebook codebook = cacheManager.loadVQCodebook(inputFile, codebookSize, qVector);
        //            if (codebook == null) {
        //                System.err.println("Failed to read quantization vectors from cache.");
        //                return;
        //            }
        //            quantizer = new VectorQuantizer(codebook);
        //            System.out.println("Created quantizer from cache");
        //
        //        } else if (options.getCodebookType() == CompressionOptions.CodebookType.MiddlePlane) {
        //            final int middlePlaneIndex = rawImageDims.getZ() / 2;
        //            int[][] refPlaneData;
        //            try {
        //                refPlaneData = planeLoader.loadVectorsFromPlaneRange(options, Utils.singlePlaneRange(middlePlaneIndex));
        //            } catch (ImageCompressionException e) {
        //                e.printStackTrace();
        //                System.err.println("Failed to load middle plane data.");
        //                return;
        //            }
        //
        //            LBGVectorQuantizer vqInitializer = new LBGVectorQuantizer(refPlaneData, codebookSize, workerCount, qVector);
        //            final LBGResult vqResult = vqInitializer.findOptimalCodebook();
        //
        //            quantizer = new VectorQuantizer(vqResult.getCodebook());
        //            System.out.println("Created quantizer from middle plane.");
        //        }
        //
        //        for (final int planeIndex : planes) {
        //            System.out.printf("Loading plane %d ...%n", planeIndex);
        //
        //            int[][] planeData;
        //            try {
        //                planeData = planeLoader.loadVectorsFromPlaneRange(options, Utils.singlePlaneRange(planeIndex));
        //            } catch (ImageCompressionException e) {
        //                e.printStackTrace();
        //                System.err.printf("Failed to load plane %d data.", planeIndex);
        //                return;
        //            }
        //
        //
        //            if (options.getCodebookType() == CompressionOptions.CodebookType.Individual) {
        //                LBGVectorQuantizer vqInitializer = new LBGVectorQuantizer(planeData, codebookSize, workerCount, qVector);
        //                LBGResult vqResult = vqInitializer.findOptimalCodebook();
        //                quantizer = new VectorQuantizer(vqResult.getCodebook());
        //                System.out.println("Created plane quantizer.");
        //            }
        //
        //            final String quantizedFile = String.format(QUANTIZED_FILE_TEMPLATE, planeIndex, codebookSize);
        //            final String diffFile = String.format(DIFFERENCE_FILE_TEMPLATE, planeIndex, codebookSize);
        //            final String absoluteDiffFile = String.format(ABSOLUTE_DIFFERENCE_FILE_TEMPLATE, planeIndex, codebookSize);
        //
        //            assert (quantizer != null);
        //            final int[][] quantizedData = quantizer.quantize(planeData, workerCount);
        //
        //            final ImageU16 quantizedImage = reconstructImageFromQuantizedVectors(quantizedData, qVector.toV2i());
        //
        //
        //            final int[] diffArray = Utils.getDifference(plane.getData(), quantizedImage.getData());
        //            final double mse = Utils.calculateMse(diffArray);
        //            final double PSNR = Utils.calculatePsnr(mse, U16.Max);
        //            System.out.printf("MSE: %.4f\tPSNR: %.4f(dB)%n", mse, PSNR);
        //
        //            if (!saveQuantizedPlaneData(quantizedImage.getData(), quantizedFile)) {
        //                System.err.println("Failed to save quantized plane.");
        //                return;
        //            }
        //
        //            saveDifference(diffArray, diffFile, absoluteDiffFile);
        //        }
    }
}
