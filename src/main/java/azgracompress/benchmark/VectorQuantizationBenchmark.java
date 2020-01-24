package azgracompress.benchmark;

import azgracompress.cli.ParsedCliOptions;
import azgracompress.data.*;
import azgracompress.quantization.QuantizationValueCache;
import azgracompress.quantization.vector.CodebookEntry;
import azgracompress.quantization.vector.LBGResult;
import azgracompress.quantization.vector.LBGVectorQuantizer;
import azgracompress.quantization.vector.VectorQuantizer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class VectorQuantizationBenchmark extends BenchmarkBase {

    final static V2i DEFAULT_QVECTOR = new V2i(3, 3);

    public VectorQuantizationBenchmark(String inputFile, String outputDirectory, int[] planes, V3i rawImageDims) {
        super(inputFile, outputDirectory, planes, rawImageDims);
    }

    public VectorQuantizationBenchmark(final ParsedCliOptions options) {
        super(options);
    }

    @Override
    public void startBenchmark() {
        startBenchmark(DEFAULT_QVECTOR);
    }

    private ImageU16 reconstructImageFromQuantizedVectors(final ImageU16 plane,
                                                          final int[][] vectors,
                                                          final V2i qVector) {
        Chunk2D reconstructedChunk = new Chunk2D(new V2i(rawImageDims.getX(), rawImageDims.getY()), new V2l(0, 0));
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
        if (qVector.getY() > 1) {
            System.out.println("2D qVector");
        } else {
            System.out.println("1D qVector");
        }
        boolean dirCreated = new File(this.outputDirectory).mkdirs();
        System.out.println(String.format("|CODEBOOK| = %d", codebookSize));
        VectorQuantizer quantizer = null;

        if (hasCacheFolder) {
            System.out.println("Loading codebook from cache");
            QuantizationValueCache cache = new QuantizationValueCache(cacheFolder);
            try {
                final CodebookEntry[] codebook = cache.readCachedValues(inputFile,
                                                                        codebookSize,
                                                                        qVector.getX(),
                                                                        qVector.getY());
                quantizer = new VectorQuantizer(codebook);

            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Failed to read quantization vectors from cache.");
                return;
            }
            System.out.println("Created quantizer from cache");
        } else if (hasReferencePlane) {
            final ImageU16 plane = loadPlane(referencePlaneIndex);

            if (plane == null) {
                System.err.println("Failed to load reference plane data.");
                return;
            }

            final int[][] refPlaneData = getPlaneVectors(plane, qVector);
            LBGVectorQuantizer vqInitializer = new LBGVectorQuantizer(refPlaneData, codebookSize, 1);
            final LBGResult vqResult = vqInitializer.findOptimalCodebook();
            quantizer = new VectorQuantizer(vqResult.getCodebook());
            System.out.println("Created reference quantizer.");
        }

        for (final int planeIndex : planes) {
            System.out.println(String.format("Loading plane %d ...", planeIndex));
            // NOTE(Moravec): Actual planeIndex is zero based.

            final ImageU16 plane = loadPlane(planeIndex - 1);

            if (plane == null) {
                System.err.println(String.format("Failed to load plane %d data. Skipping plane.", planeIndex));
                return;
            }

            final int[][] planeData = getPlaneVectors(plane, qVector);


            if (!hasGeneralQuantizer) {
                LBGVectorQuantizer vqInitializer = new LBGVectorQuantizer(planeData, codebookSize,1);
                LBGResult vqResult = vqInitializer.findOptimalCodebook();
                quantizer = new VectorQuantizer(vqResult.getCodebook());
                System.out.println("Created plane quantizer.");
            }

            final String quantizedFile = String.format(QUANTIZED_FILE_TEMPLATE, planeIndex, codebookSize);
            final String diffFile = String.format(DIFFERENCE_FILE_TEMPLATE, planeIndex, codebookSize);
            final String absoluteDiffFile = String.format(ABSOLUTE_DIFFERENCE_FILE_TEMPLATE,
                                                          planeIndex,
                                                          codebookSize);

            final int[][] quantizedData = quantizer.quantize(planeData);

            final ImageU16 quantizedImage = reconstructImageFromQuantizedVectors(plane, quantizedData, qVector);

            if (!saveQuantizedPlaneData(quantizedImage.getData(), quantizedFile)) {
                System.err.println("Failed to save quantized plane.");
                return;
            }

            saveDifference(plane.getData(), quantizedImage.getData(), diffFile, absoluteDiffFile);
        }
    }

    private void saveCodebook(final CodebookEntry[] codebook, final String codebookFile) {
        final String outFile = getFileNamePathIntoOutDir(codebookFile);
        try {
            FileOutputStream fileStream = new FileOutputStream(outFile);
            OutputStreamWriter writer = new OutputStreamWriter(fileStream);

            for (final CodebookEntry entry : codebook) {
                writer.write(entry.getVectorString());
            }

            writer.flush();
            fileStream.flush();
            fileStream.close();
        } catch (IOException ioE) {
            ioE.printStackTrace();
            System.err.println("Failed to save codebook vectors.");
        }
    }


}
