package azgracompress.compression;

import azgracompress.cli.ParsedCliOptions;
import azgracompress.data.*;
import azgracompress.fileformat.QCMPFileHeader;
import azgracompress.io.InBitStream;
import azgracompress.utilities.TypeConverter;

import java.io.*;


public class ImageDecompressor extends CompressorDecompressorBase {

    public ImageDecompressor(ParsedCliOptions options) {
        super(options);
    }

    private long getExpectedDataSizeForScalarQuantization(final QCMPFileHeader header) {
        // Quantization value count.
        final int codebookSize = (int) Math.pow(2, header.getBitsPerPixel());

        // Total codebook size in bytes.
        long codebookDataSize = (2 * codebookSize) * (header.isCodebookPerPlane() ? header.getImageSizeZ() : 1);

        // Data size of single plane indices.
        final long planeIndicesDataSize =
                (long) Math.ceil(((header.getImageSizeX() * header.getImageSizeY()) * header.getBitsPerPixel()) / 8.0);

        // All planes data size.
        final long allPlaneIndicesDataSize = planeIndicesDataSize * header.getImageSizeZ();

        return (codebookDataSize + allPlaneIndicesDataSize);
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

    private long getExpectedDataSizeForVectorQuantization(final QCMPFileHeader header) {
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


    private long getExpectedDataSize(final QCMPFileHeader header) {
        switch (header.getQuantizationType()) {
            case Scalar: {
                return getExpectedDataSizeForScalarQuantization(header);
            }
            case Vector1D:
            case Vector2D:
            case Vector3D:
                return getExpectedDataSizeForVectorQuantization(header);
            case Invalid:
                return -1;
        }
        return -1;
    }

    private QCMPFileHeader readQCMPFileHeader(DataInputStream inputStream) throws IOException {
        QCMPFileHeader header = new QCMPFileHeader();
        if (!header.readHeader(inputStream)) {
            // Invalid QCMPFile header.
            return null;
        }
        return header;
    }

    public String inspectCompressedFile() throws IOException {
        StringBuilder logBuilder = new StringBuilder();
        boolean validFile = true;


        var fileInputStream = new FileInputStream(options.getInputFile());
        var dataInputStream = new DataInputStream(fileInputStream);

        final QCMPFileHeader header = readQCMPFileHeader(dataInputStream);

        fileInputStream.close();
        dataInputStream.close();

        if (header == null) {
            logBuilder.append("Input file is not valid QCMPFile\n");
            validFile = false;
        } else {


            final boolean validHeader = header.validateHeader();
            logBuilder.append("Header is:\t\t").append(validHeader ? "valid" : "invalid").append('\n');

            logBuilder.append("Magic value:\t\t").append(header.getMagicValue()).append('\n');
            logBuilder.append("Quantization type\t");
            switch (header.getQuantizationType()) {
                case Scalar:
                    logBuilder.append("Scalar\n");
                    break;
                case Vector1D:
                    logBuilder.append("Vector1D\n");
                    break;
                case Vector2D:
                    logBuilder.append("Vector2D\n");
                    break;
                case Vector3D:
                    logBuilder.append("Vector3D\n");
                    break;
                case Invalid:
                    logBuilder.append("INVALID\n");
                    break;
            }
            logBuilder.append("Bits per pixel:\t\t").append(header.getBitsPerPixel()).append('\n');
            logBuilder.append("Codebook:\t\t").append(header.isCodebookPerPlane() ? "one per plane\n" : "one for " +
                    "all\n");

            logBuilder.append("Image size X:\t\t").append(header.getImageSizeX()).append('\n');
            logBuilder.append("Image size Y:\t\t").append(header.getImageSizeY()).append('\n');
            logBuilder.append("Image size Z:\t\t").append(header.getImageSizeZ()).append('\n');

            logBuilder.append("Vector size X:\t\t").append(header.getVectorSizeX()).append('\n');
            logBuilder.append("Vector size Y:\t\t").append(header.getVectorSizeY()).append('\n');
            logBuilder.append("Vector size Z:\t\t").append(header.getVectorSizeZ()).append('\n');

            final long fileSize = new File(options.getInputFile()).length();
            final long dataSize = fileSize - QCMPFileHeader.QCMP_HEADER_SIZE;
            final long expectedDataSize = getExpectedDataSize(header);
            validFile = (dataSize == expectedDataSize);

            logBuilder.append("Data size:\t\t").append(dataSize).append(" Bytes ").append(dataSize == expectedDataSize ? "(correct)\n" : "(INVALID)\n");
        }

        logBuilder.append("\n=== Input file is ").append(validFile ? "VALID" : "INVALID").append(" ===\n");
        return logBuilder.toString();
    }

    public void decompress() throws Exception {

        var fileInputStream = new FileInputStream(options.getInputFile());
        var dataInputStream = new DataInputStream(fileInputStream);

        final QCMPFileHeader header = readQCMPFileHeader(dataInputStream);

        if (header == null) {
            throw new Exception("Failed to read QCMPFile header");
        }
        if (!header.validateHeader()) {
            throw new Exception("QCMPFile header is invalid");
        }

        final long fileSize = new File(options.getInputFile()).length();
        final long dataSize = fileSize - QCMPFileHeader.QCMP_HEADER_SIZE;
        final long expectedDataSize = getExpectedDataSize(header);
        if (dataSize != expectedDataSize) {
            throw new Exception("Invalid file size.");
        }

        FileOutputStream fos = new FileOutputStream(options.getOutputFile(), false);
        DataOutputStream decompressStream = new DataOutputStream(fos);

        switch (header.getQuantizationType()) {
            case Scalar:
                decompressUsingScalarQuantization(dataInputStream, decompressStream, header);
                break;
            case Vector1D:
            case Vector2D:
            case Vector3D:
                decompressUsingVectorQuantization(dataInputStream, decompressStream, header);
                break;
            case Invalid:
                throw new Exception("Invalid quantization type;");
        }

        dataInputStream.close();
        fileInputStream.close();

        decompressStream.flush();
        decompressStream.close();
        fos.flush();
        fos.close();
    }

    private int[] readScalarQuantizationValues(DataInputStream compressedStream, final int n) throws IOException {
        int[] quantizationValues = new int[n];
        for (int i = 0; i < n; i++) {
            quantizationValues[i] = compressedStream.readUnsignedShort();
        }
        return quantizationValues;
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
            // FIXME
            //            Chunk2D new Chunk2D(new V2i(width, height), new V2l(0, 0), data);
            //            var chunks = plane.as2dChunk().divideIntoChunks(qVector);

            var chunks = reconstructedChunk.divideIntoChunks(qVector);
            Chunk2D.updateChunkData(chunks, vectors);
            reconstructedChunk.reconstructFromChunks(chunks);

        } else {
            // 1D vector
            reconstructedChunk.reconstructFromVectors(vectors);
        }
        return reconstructedChunk.asImageU16();
    }

    private void decompressUsingVectorQuantization(DataInputStream compressedStream,
                                                   DataOutputStream decompressStream,
                                                   final QCMPFileHeader header) throws Exception {
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


    private void decompressUsingScalarQuantization(DataInputStream compressedStream,
                                                   DataOutputStream decompressStream,
                                                   final QCMPFileHeader header) throws Exception {

        final int codebookSize = (int) Math.pow(2, header.getBitsPerPixel());
        final int planeCountForDecompression = header.getImageSizeZ();

        final int planePixelCount = header.getImageSizeX() * header.getImageSizeY();
        final int planeIndicesDataSize = (int) Math.ceil((planePixelCount * header.getBitsPerPixel()) / 8.0);

        int[] quantizationValues = null;
        if (!header.isCodebookPerPlane()) {
            // There is only one codebook.
            Log("Loading reference codebook...");
            quantizationValues = readScalarQuantizationValues(compressedStream, codebookSize);
        }


        for (int planeIndex = 0; planeIndex < planeCountForDecompression; planeIndex++) {
            if (header.isCodebookPerPlane()) {
                Log("Loading plane codebook...");
                quantizationValues = readScalarQuantizationValues(compressedStream, codebookSize);
            }
            assert (quantizationValues != null);

            Log(String.format("Decompressing plane %d...", planeIndex));
            InBitStream inBitStream = new InBitStream(compressedStream, header.getBitsPerPixel(), planeIndicesDataSize);
            inBitStream.readToBuffer();
            inBitStream.setAllowReadFromUnderlyingStream(false);
            final int[] indices = inBitStream.readNValues(planePixelCount);

            short[] decompressedValues = new short[planePixelCount];
            for (int i = 0; i < planePixelCount; i++) {
                decompressedValues[i] = TypeConverter.intToShort(quantizationValues[indices[i]]);
            }
            final byte[] decompressedPlaneData = TypeConverter.shortArrayToByteArray(decompressedValues, false);

            decompressStream.write(decompressedPlaneData);
            Log(String.format("Decompressed plane %d.", planeIndex));
        }
    }
}