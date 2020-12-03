package cz.it4i.qcmp.fileformat;

import cz.it4i.qcmp.U16;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class QvcHeaderV2 extends QvcHeaderV1 {
    //region Constants
    private static final int VERSION = 2;
    public static final String MAGIC_VALUE = "QVCFILEV2";
    public static final int RESERVED_BYTES_SIZE = 10;
    public static final int BASE_HEADER_SIZE = 32;
    //endregion

    //region Header fields.
    private int huffmanDataSize;
    //endregion

    //region IFileHeader implementation
    @Override
    public String getMagicValue() {
        return MAGIC_VALUE;
    }

    @Override
    public int getHeaderVersion() {
        return VERSION;
    }

    @Override
    public boolean validateHeader() {
        final boolean v1HeaderValidation = super.validateHeader();
        return U16.isInRange(huffmanDataSize) && v1HeaderValidation;
    }

    @Override
    public void readFromStream(final DataInputStream inputStream) throws IOException {
        super.readFromStream(inputStream);
        huffmanDataSize = inputStream.readUnsignedShort();
        final int skipped = inputStream.skipBytes(RESERVED_BYTES_SIZE);
        if (skipped != RESERVED_BYTES_SIZE)
            throw new IOException("Unable to read QvcHeaderV2. Unable to skip reserved bytes.");
    }

    @Override
    public void writeToStream(final DataOutputStream outputStream) throws IOException {
        super.writeToStream(outputStream);

        outputStream.writeShort(huffmanDataSize);

        for (int i = 0; i < RESERVED_BYTES_SIZE; i++) {
            outputStream.writeByte(0);
        }
    }

    @Override
    public long getExpectedDataSize() {
        long expectedFileSize = BASE_HEADER_SIZE + trainFileNameSize + huffmanDataSize;

        switch (quantizationType) {
            case Scalar:
                expectedFileSize += codebookSize * 2L; // Scalar quantization values
                break;
            case Vector1D:
            case Vector2D:
            case Vector3D:
                expectedFileSize += (((long) vectorSizeX * vectorSizeY * vectorSizeZ) * codebookSize * 2L); // Quantization vectors
                break;
            case Invalid:
                return -1;
        }
        return expectedFileSize;
    }

    @Override
    public void report(final StringBuilder sb, final String inputFile) {
        super.report(sb, inputFile);
        sb.append("HuffmanDataSize: ").append(huffmanDataSize).append('\n');
    }
    //endregion


    public int getHuffmanDataSize() {
        return huffmanDataSize;
    }

    public void setHuffmanDataSize(final int n) {
        huffmanDataSize = n;
    }
}
