package compression.tool;

public class ImageDecompressor extends CompressorDecompressorBase {
    public ImageDecompressor(String outputDirectory, int bitsPerPixel, boolean verbose) {
        super(outputDirectory, bitsPerPixel, verbose);
    }

    public boolean decompressCompressedImages(final String[] files) {
        for (final String file : files) {
            if (!decompressCompressedImage(file)) {
                return false;
            }
        }
        return true;
    }

    public boolean decompressCompressedImage(final String file) {
        return false;
    }
}
