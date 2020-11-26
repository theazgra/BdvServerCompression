package cz.it4i.qcmp.cache;

import cz.it4i.qcmp.fileformat.CacheFileHeaderV1;
import cz.it4i.qcmp.quantization.vector.VQCodebook;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class VQCacheFile implements ICacheFile {
    private CacheFileHeaderV1 header;
    private VQCodebook codebook;

    public VQCacheFile() {
    }

    public VQCacheFile(final CacheFileHeaderV1 header, final VQCodebook codebook) {
        this.header = header;
        this.codebook = codebook;
        assert (header.getCodebookSize() == codebook.getCodebookSize());
    }

    public void writeToStream(final DataOutputStream outputStream) throws IOException {
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

    public void readFromStream(final DataInputStream inputStream) throws IOException {
        header = new CacheFileHeaderV1();
        header.readFromStream(inputStream);
        readFromStream(inputStream, header);
    }

    @Override
    public void readFromStream(final DataInputStream inputStream, final CacheFileHeaderV1 header) throws IOException {
        this.header = header;
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

    public CacheFileHeaderV1 getHeader() {
        return header;
    }

    public VQCodebook getCodebook() {
        return codebook;
    }

    @Override
    public void report(final StringBuilder builder) {
        final int[][] vectors = codebook.getVectors();
        builder.append("\n- - - - - - - - - - - - - - - - - - - - - - - - -\n");
        for (final int[] vector : vectors) {
            for (final int x : vector) {
                builder.append(x).append(';');
            }
            builder.append("\n- - - - - - - - - - - - - - - - - - - - - - - - -\n");
        }
    }

    @Override
    public String klass() {
        return "VQCacheFile";
    }
}
