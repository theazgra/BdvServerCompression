package azgracompress.compression;

import azgracompress.cli.ParsedCliOptions;
import azgracompress.fileformat.QCMPFileHeader;
import azgracompress.io.InBitStream;
import azgracompress.utilities.Stopwatch;
import azgracompress.utilities.TypeConverter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class SQImageDecompressor extends CompressorDecompressorBase implements IImageDecompressor {
    public SQImageDecompressor(ParsedCliOptions options) {
        super(options);
    }

    private int[] readScalarQuantizationValues(DataInputStream compressedStream, final int n) throws IOException {
        int[] quantizationValues = new int[n];
        for (int i = 0; i < n; i++) {
            quantizationValues[i] = compressedStream.readUnsignedShort();
        }
        return quantizationValues;
    }

    @Override
    public long getExpectedDataSize(QCMPFileHeader header) {
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

    @Override
    public void decompress(DataInputStream compressedStream,
                           DataOutputStream decompressStream,
                           QCMPFileHeader header) throws Exception {
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

        Stopwatch stopwatch = new Stopwatch();
        for (int planeIndex = 0; planeIndex < planeCountForDecompression; planeIndex++) {
            stopwatch.restart();
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

            int[] decompressedValues = new int[planePixelCount];
            for (int i = 0; i < planePixelCount; i++) {
                decompressedValues[i] = quantizationValues[indices[i]];
            }
            final byte[] decompressedPlaneData = TypeConverter.unsignedShortArrayToByteArray(decompressedValues, false);

            decompressStream.write(decompressedPlaneData);
            stopwatch.stop();
            Log(String.format("Decompressed plane %d in %s.", planeIndex, stopwatch.getElapsedTimeString()));
        }

    }
}
