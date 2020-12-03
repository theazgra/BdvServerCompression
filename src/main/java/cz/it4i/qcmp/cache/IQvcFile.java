package cz.it4i.qcmp.cache;

import cz.it4i.qcmp.fileformat.IQvcHeader;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface IQvcFile {

    void writeToStream(DataOutputStream outputStream) throws IOException;

    void readFromStream(DataInputStream inputStream, IQvcHeader header) throws IOException;

    IQvcHeader getHeader();

    void report(StringBuilder builder);

    void convertToNewerVersion(final boolean inPlace, final String inputPath, final String outputPath) throws IOException;
}
