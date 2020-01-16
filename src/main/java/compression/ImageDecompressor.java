package compression;

import cli.ParsedCliOptions;

public class ImageDecompressor extends CompressorDecompressorBase {
    public ImageDecompressor(ParsedCliOptions options) {
        super(options);
    }

    public boolean decompress() {
        return true;
    }
}
