package azgracompress.cache;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface ICacheFile {

    void writeToStream(DataOutputStream outputStream) throws IOException;

    void readFromStream(DataInputStream inputStream) throws IOException;

    CacheFileHeader getHeader();
}
