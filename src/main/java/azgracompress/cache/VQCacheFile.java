package azgracompress.cache;

import azgracompress.quantization.vector.VQCodebook;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class VQCacheFile implements ICacheFile {
    private CacheFileHeader header;
    private VQCodebook codebook;

    public VQCacheFile() {
    }

    public VQCacheFile(final CacheFileHeader header, final VQCodebook codebook) {
        this.header = header;
        this.codebook = codebook;
        assert (header.getCodebookSize() == codebook.getCodebookSize());
    }

    public void writeToStream(DataOutputStream outputStream) throws IOException {
        header.writeToStream(outputStream);

        final int[][] entries = codebook.getVectors();
        for (final int[] entry : entries) {
            for (final int vectorValue : entry) {
                outputStream.writeShort(vectorValue);
            }
        }

        final long[] frequencies = codebook.getVectorFrequencies();
        for (final long vF : frequencies) {
            outputStream.writeLong(vF);
        }
    }

    public void readFromStream(DataInputStream inputStream) throws IOException {
        header = new CacheFileHeader();
        header.readFromStream(inputStream);
        readFromStream(inputStream, header);
    }

    @Override
    public void readFromStream(DataInputStream inputStream, CacheFileHeader header) throws IOException {
        final int codebookSize = header.getCodebookSize();

        final int entrySize = header.getVectorSizeX() * header.getVectorSizeY() * header.getVectorSizeZ();
        final int[][] vectors = new int[codebookSize][entrySize];
        final long[] frequencies = new long[codebookSize];

        for (int i = 0; i < codebookSize; i++) {
            //int[] vector = new int[entrySize];
            for (int j = 0; j < entrySize; j++) {
                vectors[i][j] = inputStream.readUnsignedShort();
            }
        }

        for (int i = 0; i < codebookSize; i++) {
            frequencies[i] = inputStream.readLong();
        }
        codebook = new VQCodebook(header.getVectorDim(), vectors, frequencies);
    }

    public CacheFileHeader getHeader() {
        return header;
    }

    public VQCodebook getCodebook() {
        return codebook;
    }

    @Override
    public void report(StringBuilder builder) {
        final int[][] vectors = codebook.getVectors();
        for (int[] vector : vectors) {
            builder.append("- - - - - - - - - - - - - - - - - - - - - - - - -\n");
            for (final int x : vector) {
                builder.append(x).append(';');
            }
        }
    }
}
