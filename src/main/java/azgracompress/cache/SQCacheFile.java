package azgracompress.cache;

import azgracompress.quantization.scalar.SQCodebook;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class SQCacheFile implements ICacheFile {
    private CacheFileHeader header;
    private SQCodebook codebook;

    public SQCacheFile() {
    }

    public SQCacheFile(final CacheFileHeader header, final SQCodebook codebook) {
        this.header = header;
        this.codebook = codebook;
        assert (header.getCodebookSize() == codebook.getCodebookSize());
    }

    public void writeToStream(DataOutputStream outputStream) throws IOException {
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

    public void readFromStream(DataInputStream inputStream) throws IOException {
        header = new CacheFileHeader();
        header.readFromStream(inputStream);
        readFromStream(inputStream, header);
    }

    public void readFromStream(DataInputStream inputStream, CacheFileHeader header) throws IOException {
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

    public CacheFileHeader getHeader() {
        return header;
    }

    public SQCodebook getCodebook() {
        return codebook;
    }

    @Override
    public void report(StringBuilder builder) {

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
