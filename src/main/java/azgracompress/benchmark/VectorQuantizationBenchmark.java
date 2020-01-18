package azgracompress.benchmark;

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

    @Override
    public void startBenchmark() {
        startBenchmark(DEFAULT_QVECTOR);
    }

    private ImageU16 reconstructImageFromQuantizedVectors(final ImageU16 plane,
                                                          final int[][] vectors,
                                                          final V2i qVector) {
        Chunk2D reconstructedChunk = new Chunk2D(new V2i(rawImageDims.getX(), rawImageDims.getY()), new V2l(0, 0));
        if (qVector.getY() > 1) {
            var chunks = plane.as2dChunk().divideIntoChunks(qVector);
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

        if (qVector.getY() > 1) {
            System.out.println("2D qVector");
        } else {
            System.out.println("1D qVector");
        }

        boolean dirCreated = new File(this.outputDirectory).mkdirs();

        for (final int planeIndex : planes) {
            System.out.println(String.format("Loading plane %d ...", planeIndex));
            // NOTE(Moravec): Actual planeIndex is zero based.

            final ImageU16 plane = loadPlane(planeIndex - 1);

            if (plane == null) {
                System.err.println(String.format("Failed to load plane %d data. Skipping plane.", planeIndex));
                return;
            }

            final int[][] planeData = getPlaneVectors(plane, qVector);


            // Test codebook sizes from 2^2 to 2^8
            for (int bitCount = 2; bitCount <= 8; bitCount++) {
                final int codebookSize = (int) Math.pow(2, bitCount);
                System.out.println(String.format("|CODEBOOK| = %d", codebookSize));

                LBGVectorQuantizer vqInitializer = new LBGVectorQuantizer(planeData, codebookSize);
                LBGResult vqResult = vqInitializer.findOptimalCodebook();

                VectorQuantizer quantizer = new VectorQuantizer(vqResult.getCodebook());

                final String quantizedFile = String.format("p%d_cb%d.raw", planeIndex, codebookSize);
                final String diffFile = String.format("p%d_cb%d_diff.raw", planeIndex, codebookSize);
                final String absoluteDiffFile = String.format("p%d_cb%d_adiff.raw", planeIndex, codebookSize);

                final String codebookFile = String.format("p%d_cb%d_vectors.txt", planeIndex, codebookSize);
                saveCodebook(vqResult.getCodebook(), codebookFile);

                final int[][] quantizedData = quantizer.quantize(planeData);

                final ImageU16 quantizedImage = reconstructImageFromQuantizedVectors(plane, quantizedData, qVector);

                if (!saveQuantizedPlaneData(quantizedImage.getData(), quantizedFile)) {
                    System.err.println("Failed to save quantized plane.");
                    return;
                }

                saveDifference(plane.getData(), quantizedImage.getData(), diffFile, absoluteDiffFile);
            }
        }
    }

    private void saveCodebook(final CodebookEntry[] codebook, final String codebookFile) {
        final String outFile = getFileNamePathIntoOutDir(codebookFile);
        try {
            FileOutputStream fileStream = new FileOutputStream(outFile);
            OutputStreamWriter writer = new OutputStreamWriter(fileStream);

            for (final var entry : codebook) {
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
