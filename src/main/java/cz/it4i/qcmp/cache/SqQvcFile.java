package cz.it4i.qcmp.cache;

import cz.it4i.qcmp.fileformat.QvcHeaderV1;
import cz.it4i.qcmp.quantization.scalar.SQCodebook;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class SQCacheFile implements IQvcFile {
    private QvcHeaderV1 header;
    private SQCodebook codebook;

    public SQCacheFile() {
    }

    public SQCacheFile(final QvcHeaderV1 header, final SQCodebook codebook) {
        this.header = header;
        this.codebook = codebook;
        assert (header.getCodebookSize() == codebook.getCodebookSize());
    }

    public void writeToStream(final DataOutputStream outputStream) throws IOException {
        header.writeToStream(outputStream);
        final int[] quantizationValues = codebook.getCentroids();
        final long[] frequencies = codebook.getSymbolFrequencies();

        for (final int qV : quantizationValues) {
            outputStream.writeShort(qV);
        }
        for (final long sF : frequencies) {
            outputStream.writeLong(sF);
        }
    }

    public void readFromStream(final DataInputStream inputStream) throws IOException {
        header = new QvcHeaderV1();
        header.readFromStream(inputStream);
        readFromStream(inputStream, header);
    }

    public void readFromStream(final DataInputStream inputStream, final QvcHeaderV1 header) throws IOException {
        this.header = header;
        final int codebookSize = header.getCodebookSize();
        final int[] centroids = new int[codebookSize];
        final long[] frequencies = new long[codebookSize];

        for (int i = 0; i < codebookSize; i++) {
            centroids[i] = inputStream.readUnsignedShort();
        }
        for (int i = 0; i < codebookSize; i++) {
            frequencies[i] = inputStream.readLong();
        }
        codebook = new SQCodebook(centroids, frequencies);
    }

    public QvcHeaderV1 getHeader() {
        return header;
    }

    public SQCodebook getCodebook() {
        return codebook;
    }

    @Override
    public void report(final StringBuilder builder) {

        final int[] centroids = codebook.getCentroids();
        for (int i = 0; i < centroids.length; i++) {
            if (i != centroids.length - 1) {
                builder.append(centroids[i]).append(", ");
            } else {
                builder.append(centroids[i]).append('\n');
            }
        }
    }
}