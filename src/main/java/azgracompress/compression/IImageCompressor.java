package azgracompress.compression;

import java.io.DataOutputStream;

public interface IImageCompressor {

    // TODO(Moravec): Replace default Exception with better Exception type.
    void compress(DataOutputStream compressStream) throws Exception;
}
