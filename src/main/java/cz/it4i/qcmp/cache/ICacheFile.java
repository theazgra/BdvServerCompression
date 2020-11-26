package cz.it4i.qcmp.cache;

import cz.it4i.qcmp.fileformat.CacheFileHeaderV1;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface ICacheFile {

    void writeToStream(DataOutputStream outputStream) throws IOException;

    void readFromStream(DataInputStream inputStream) throws IOException;

    void readFromStream(DataInputStream inputStream, CacheFileHeaderV1 header) throws IOException;

    CacheFileHeaderV1 getHeader();

    void report(StringBuilder builder);

    String klass();
}
