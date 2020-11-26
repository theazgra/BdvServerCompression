package cz.it4i.qcmp.fileformat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface IFileHeader {
    String getMagicValue();

    int getHeaderVersion();

    boolean validateHeader();

    void writeToStream(final DataOutputStream stream) throws IOException;

    void readFromStream(final DataInputStream stream) throws IOException;

    void report(final StringBuilder builder);

    long getExpectedFileSize();
}
