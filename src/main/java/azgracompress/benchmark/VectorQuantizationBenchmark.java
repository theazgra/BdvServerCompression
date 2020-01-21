package azgracompress.benchmark;

import azgracompress.cli.ParsedCliOptions;
import azgracompress.data.*;
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
            Chunk2D[] chunks = plane.as2dChunk().divideIntoChunks(qVector);
            Chunk2D.updateChunkData(chunks, vectors);
            reconstructedChunk.reconstructFromChunks(chunks);

        } else {
            // 1D vector
            reconstructedChunk.reconstructFromVectors(vectors);
        }
        return reconstructedChunk.asImageU16();
    }

    private int[][] getPlaneVectors(ImageU16 plane, V2i qVector) {
        if (qVector.getY() > 1) {
            return Chunk2D.chunksAsImageVectors(plane.as2dChunk().divideIntoChunks(qVector));
        } else {
            return plane.as2dChunk().divideInto1DVectors(qVector.getX());
        }
    }

    public void startBenchmark(final V2i qVector) {
        if (planes.length < 1) {
            return;
        }
        // TODO(Moravec): Support parsed CLI options.
        if (qVector.getY() > 1) {
            System.out.println("2D qVector");
        } else {
            System.out.println("1D qVector");
        }
        boolean dirCreated = new File(this.outputDirectory).mkdirs();

        VectorQuantizer quantizer = null;

        if (hasReferencePlane) {
            final ImageU16 plane = loadPlane(referencePlaneIndex);

            if (plane == null) {
                System.err.println("Failed to load reference plane data.");
                return;
            }

            final int[][] refPlaneData = getPlaneVectors(plane, qVector);
            LBGVectorQuantizer vqInitializer = new LBGVectorQuantizer(refPlaneData, codebookSize);
            final LBGResult vqResult = vqInitializer.findOptimalCodebook();
            quantizer = new VectorQuantizer(vqResult.getCodebook());
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

            System.out.println(String.format("|CODEBOOK| = %d", codebookSize));
            if (!hasReferencePlane) {
                LBGVectorQuantizer vqInitializer = new LBGVectorQuantizer(planeData, codebookSize);
                LBGResult vqResult = vqInitializer.findOptimalCodebook();
                quantizer = new VectorQuantizer(vqResult.getCodebook());
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
