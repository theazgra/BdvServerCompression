package cz.it4i.qcmp.cache;

import cz.it4i.qcmp.fileformat.IQvcHeader;
import cz.it4i.qcmp.quantization.vector.VQCodebook;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class VqQvcFile implements IQvcFile {
    private IQvcHeader header;
    private VQCodebook codebook;

    public VqQvcFile() {
    }

    public VqQvcFile(final IQvcHeader header, final VQCodebook codebook) {
        this.header = header;
        this.codebook = codebook;
        assert (header.getCodebookSize() == codebook.getCodebookSize());
    }

    @Override
    public void writeToStream(final DataOutputStream outputStream) throws IOException {
        // TODO
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

    @Override
    public void readFromStream(final DataInputStream inputStream, final IQvcHeader header) throws IOException {
        // TODO
        this.header = header;
        final int codebookSize = header.getCodebookSize();

        final int entrySize = header.getVectorDim().multiplyTogether();
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

    @Override
    public IQvcHeader getHeader() {
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
}
