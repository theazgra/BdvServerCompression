package compression;

public class CompressorDecompressorBase {
    private final String outputDirectory;
    private final int bitsPerPixel;
    private final boolean verbose;

    public CompressorDecompressorBase(final String outputDirectory, final int bitsPerPixel, final boolean verbose) {
        this.outputDirectory = outputDirectory;
        this.bitsPerPixel = bitsPerPixel;
        this.verbose = verbose;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public String getOutputDirectory() {
        return outputDirectory;
    }

    public int getBitsPerPixel() {
        return bitsPerPixel;
    }
}
