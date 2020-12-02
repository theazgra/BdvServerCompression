package cz.it4i.qcmp.cache;

import cz.it4i.qcmp.fileformat.IQvcHeader;
import cz.it4i.qcmp.quantization.scalar.SQCodebook;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class SqQvcFile implements IQvcFile {
    private IQvcHeader header;
    private SQCodebook codebook;

    public SqQvcFile() {
    }

    public SqQvcFile(final IQvcHeader header, final SQCodebook codebook) {
        this.header = header;
        this.codebook = codebook;
        assert (header.getCodebookSize() == codebook.getCodebookSize());
    }

    @Override
    public void writeToStream(final DataOutputStream outputStream) throws IOException {
        // TODO
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

    @Override
    public void readFromStream(final DataInputStream inputStream, final IQvcHeader header) throws IOException {
        // TODO
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

    @Override
    public IQvcHeader getHeader() {
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
