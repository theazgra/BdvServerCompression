package azgracompress.compression;

import azgracompress.cli.ParsedCliOptions;
import azgracompress.data.*;
import azgracompress.fileformat.QCMPFileHeader;
import azgracompress.io.InBitStream;
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
                                        final int vectorSize) throws IOException {

        int[][] codebook = new int[codebookSize][vectorSize];
        for (int codebookIndex = 0; codebookIndex < codebookSize; codebookIndex++) {
            for (int vecIndex = 0; vecIndex < vectorSize; vecIndex++) {
                codebook[codebookIndex][vecIndex] = compressedStream.readUnsignedShort();
            }
        }
        return codebook;
    }


    private ImageU16 reconstructImageFromQuantizedVectors(final int[][] vectors,
                                                          final V2i qVector,
                                                          final V3i imageDims) {

        Chunk2D reconstructedChunk = new Chunk2D(new V2i(imageDims.getX(), imageDims.getY()), new V2l(0, 0));
        if (qVector.getY() > 1) {
            var chunks = reconstructedChunk.divideIntoChunks(qVector);
            Chunk2D.updateChunkData(chunks, vectors);
            reconstructedChunk.reconstructFromChunks(chunks);

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
                           QCMPFileHeader header) throws Exception {
        final int codebookSize = (int) Math.pow(2, header.getBitsPerPixel());
        assert (header.getVectorSizeZ() == 1);
        final int vectorSize = header.getVectorSizeX() * header.getVectorSizeY() * header.getVectorSizeZ();
        final int planeCountForDecompression = header.getImageSizeZ();
        final int planePixelCount = header.getImageSizeX() * header.getImageSizeY();
        final long planeVectorCount = calculatePlaneVectorCount(header);
        final long planeDataSize = calculatePlaneDataSize(planeVectorCount, header.getBitsPerPixel());
        final V2i qVector = new V2i(header.getVectorSizeX(), header.getVectorSizeY());


        int[][] quantizationVectors = null;
        if (!header.isCodebookPerPlane()) {
            // There is only one codebook.
            Log("Loading reference codebook...");
            quantizationVectors = readCodebookVectors(compressedStream, codebookSize, vectorSize);
        }


        for (int planeIndex = 0; planeIndex < planeCountForDecompression; planeIndex++) {
            if (header.isCodebookPerPlane()) {
                Log("Loading plane codebook...");
                quantizationVectors = readCodebookVectors(compressedStream, codebookSize, vectorSize);
            }
            assert (quantizationVectors != null);

            Log(String.format("Decompressing plane %d...", planeIndex));
            InBitStream inBitStream = new InBitStream(compressedStream, header.getBitsPerPixel(), (int) planeDataSize);
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

            //            int[] decompressedValues = new int[planePixelCount];
            //            for (int vecIndex = 0; vecIndex < planeVectorCount; vecIndex++) {
            //                System.arraycopy(quantizationVectors[indices[vecIndex]],
            //                                 0,
            //                                 decompressedValues,
            //                                 (vecIndex * vectorSize),
            //                                 vectorSize);
            //            }
            final ImageU16 decompressedPlane = reconstructImageFromQuantizedVectors(decompressedVectors,
                                                                                    qVector,
                                                                                    header.getImageDims());
            final byte[] decompressedPlaneData = TypeConverter.shortArrayToByteArray(decompressedPlane.getData(),
                                                                                     false);
            decompressStream.write(decompressedPlaneData);
            Log(String.format("Decompressed plane %d.", planeIndex));
        }

    }
}