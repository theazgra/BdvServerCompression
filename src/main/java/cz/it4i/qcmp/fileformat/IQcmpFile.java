package cz.it4i.qcmp.fileformat;

import java.io.IOException;

public interface IQcmpFile {
    IFileHeader getHeader();

    void convertToNewerVersion(final boolean inPlace, final String inputPath, final String outputPath) throws IOException;
}
