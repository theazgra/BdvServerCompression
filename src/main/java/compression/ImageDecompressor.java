package compression;

import cli.ParsedCliOptions;
import compression.fileformat.QCMPFileHeader;
import compression.io.InBitStream;
import compression.utilities.TypeConverter;

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

    private long getExpectedDataSizeForVectorQuantization(final QCMPFileHeader header) {
        // Vector count in codebook
        final int codebookSize = (int) Math.pow(2, header.getBitsPerPixel());

        // Single vector size in bytes.
        final int vectorDataSize = 2 * header.getVectorSizeX() * header.getVectorSizeY() * header.getImageSizeZ();

        // Total codebook size in bytes.
        final long codebookDataSize = (codebookSize * vectorDataSize) * (header.isCodebookPerPlane() ?
                header.getImageSizeZ() : 1);

        final int vectorXCount = (int) Math.ceil((double) header.getImageSizeX() / (double) header.getVectorSizeX());
        final int vectorYCount = (int) Math.ceil((double) header.getImageSizeY() / (double) header.getVectorSizeY());
        // Number of vectors per plane.
        final long vectorCount = vectorXCount * vectorYCount;

        // Data size of single plane indices.
        final long planeDataSize = (long) Math.ceil((vectorCount * header.getBitsPerPixel()) / 8.0);

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
                return getExpectedDataSizeForVectorQuantization(header);
            case Vector3D:
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
                // TODO!
                break;
            case Vector3D:
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