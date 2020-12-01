package cz.it4i.qcmp.cache;

import cz.it4i.qcmp.fileformat.QvcHeaderV1;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface ICacheFile {

    void writeToStream(DataOutputStream outputStream) throws IOException;

    void readFromStream(DataInputStream inputStream) throws IOException;

    void readFromStream(DataInputStream inputStream, QvcHeaderV1 header) throws IOException;

    QvcHeaderV1 getHeader();

    void report(StringBuilder builder);

    String klass();
}
