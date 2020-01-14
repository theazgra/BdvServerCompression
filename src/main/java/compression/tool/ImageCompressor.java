package compression.tool;

public class ImageCompressor extends CompressorDecompressorBase {
    public ImageCompressor(String outputDirectory, int bitsPerPixel, boolean verbose) {
        super(outputDirectory, bitsPerPixel, verbose);
    }

    public boolean compressRawImages(final String[] files) {
        for (final String file : files) {
            if (!compressRawImage(file)) {
                return false;
            }
        }
        return true;
    }

    public boolean compressRawImage(final String file) {
        return false;
    }
}
