package azgracompress.compression;

import azgracompress.cli.ParsedCliOptions;
import azgracompress.compression.exception.ImageDecompressionException;
import azgracompress.data.*;
import azgracompress.fileformat.QCMPFileHeader;
import azgracompress.io.InBitStream;
import azgracompress.utilities.Stopwatch;
import azgracompress.utilities.TypeConverter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class VQImageDecompressor extends CompressorDecompressorBase implements IImageDecompressor {
    public VQImageDecompressor(ParsedCliOptions options) {
        super(options);
    }

    private long calculatePlaneVectorCount(final QCMPFileHeader header) {
        final int vectorXCount = (int) Math.ceil((double) header.getImageSizeX() / (double) header.getVectorSizeX());
        final int vectorYCount = (int) Math.ceil((double) header.getImageSizeY() / (double) header.getVectorSizeY());
        // Number of vectors per plane.
        return (vectorXCount * vectorYCount);
    }

    private long calculatePlaneDataSize(final long planeVectorCount, final int bpp) {
        // Data size of single plane indices.
        return (long) Math.ceil((planeVectorCount * bpp) / 8.0);
    }

    private int[][] readCodebookVectors(DataInputStream compressedStream,
                                        final int codebookSize,
                                        final int vectorSize) throws ImageDecompressionException {

        int[][] codebook = new int[codebookSize][vectorSize];
        try {
            for (int codebookIndex = 0; codebookIndex < codebookSize; codebookIndex++) {
                for (int vecIndex = 0; vecIndex < vectorSize; vecIndex++) {
                    codebook[codebookIndex][vecIndex] = compressedStream.readUnsignedShort();
                }
            }
        } catch (IOException ioEx) {
            throw new ImageDecompressionException("Unable to read quantization values from compressed stream.", ioEx);
        }
        return codebook;
    }


    private ImageU16 reconstructImageFromQuantizedVectors(final int[][] vectors,
                                                          final V2i qVector,
                                                          final V3i imageDims) {

        Chunk2D reconstructedChunk = new Chunk2D(new V2i(imageDims.getX(), imageDims.getY()), new V2l(0, 0));
        if (qVector.getY() > 1) {
            reconstructedChunk.reconstructFrom2DVectors(vectors, qVector);
        } else {
            // 1D vector
            reconstructedChunk.reconstructFromVectors(vectors);
        }
        return reconstructedChunk.asImageU16();
    }

    @Override
    public long getExpectedDataSize(QCMPFileHeader header) {
        // Vector count in codebook
        final int codebookSize = (int) Math.pow(2, header.getBitsPerPixel());

        // Single vector size in bytes.
        assert (header.getVectorSizeZ() == 1);
        final int vectorDataSize = 2 * header.getVectorSizeX() * header.getVectorSizeY() * header.getVectorSizeZ();

        // Total codebook size in bytes.
        final long codebookDataSize = (codebookSize * vectorDataSize) * (header.isCodebookPerPlane() ?
                header.getImageSizeZ() : 1);

        // Number of vectors per plane.
        final long planeVectorCount = calculatePlaneVectorCount(header);

        // Data size of single plane indices.
        final long planeDataSize = calculatePlaneDataSize(planeVectorCount, header.getBitsPerPixel());

        // All planes data size.
        final long allPlanesDataSize = planeDataSize * header.getImageSizeZ();

        return (codebookDataSize + allPlanesDataSize);
    }

    @Override
    public void decompress(DataInputStream compressedStream,
                           DataOutputStream decompressStream,
                           QCMPFileHeader header) throws ImageDecompressionException {
        final int codebookSize = (int) Math.pow(2, header.getBitsPerPixel());
        assert (header.getVectorSizeZ() == 1);
        final int vectorSize = header.getVectorSizeX() * header.getVectorSizeY() * header.getVectorSizeZ();
        final int planeCountForDecompression = header.getImageSizeZ();
        final long planeVectorCount = calculatePlaneVectorCount(header);
        final long planeDataSize = calculatePlaneDataSize(planeVectorCount, header.getBitsPerPixel());
        final V2i qVector = new V2i(header.getVectorSizeX(), header.getVectorSizeY());


        int[][] quantizationVectors = null;
        if (!header.isCodebookPerPlane()) {
            // There is only one codebook.
            Log("Loading reference codebook...");
            quantizationVectors = readCodebookVectors(compressedStream, codebookSize, vectorSize);
        }

        Stopwatch stopwatch = new Stopwatch();
        for (int planeIndex = 0; planeIndex < planeCountForDecompression; planeIndex++) {
            stopwatch.restart();
            if (header.isCodebookPerPlane()) {
                Log("Loading plane codebook...");
                quantizationVectors = readCodebookVectors(compressedStream, codebookSize, vectorSize);
            }
            assert (quantizationVectors != null);

            Log(String.format("Decompressing plane %d...", planeIndex));

            byte[] decompressedPlaneData = null;

            try (InBitStream inBitStream = new InBitStream(compressedStream,
                                                           header.getBitsPerPixel(),
                                                           (int) planeDataSize)) {
                inBitStream.readToBuffer();
                inBitStream.setAllowReadFromUnderlyingStream(false);
                final int[] indices = inBitStream.readNValues((int) planeVectorCount);

                int[][] decompressedVectors = new int[(int) planeVectorCount][vectorSize];
                for (int vecIndex = 0; vecIndex < planeVectorCount; vecIndex++) {

                    System.arraycopy(quantizationVectors[indices[vecIndex]],
                                     0,
                                     decompressedVectors[vecIndex],
                                     0,
                                     vectorSize);
                }


                final ImageU16 decompressedPlane = reconstructImageFromQuantizedVectors(decompressedVectors,
                                                                                        qVector,
                                                                                        header.getImageDims());
                decompressedPlaneData =
                        TypeConverter.unsignedShortArrayToByteArray(decompressedPlane.getData(), false);
            } catch (Exception ex) {
                throw new ImageDecompressionException("Unable to read indices from InBitStream.", ex);
            }


            try {
                decompressStream.write(decompressedPlaneData);
            } catch (IOException e) {
                throw new ImageDecompressionException("Unable to write decompressed data to decompress stream.", e);
            }

            stopwatch.stop();
            Log(String.format("Decompressed plane %d in %s.", planeIndex, stopwatch.getElapsedTimeString()));
        }

    }
}
