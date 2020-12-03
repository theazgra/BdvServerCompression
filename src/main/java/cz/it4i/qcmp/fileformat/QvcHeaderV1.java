package cz.it4i.qcmp.fileformat;

import cz.it4i.qcmp.U16;
import cz.it4i.qcmp.data.V2i;
import cz.it4i.qcmp.data.V3i;
import cz.it4i.qcmp.io.RawDataIO;
import cz.it4i.qcmp.utilities.Utils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class QvcHeaderV1 implements IQvcHeader {
    //region Constants
    private static final int VERSION = 1;
    public static final String MAGIC_VALUE = "QCMPCACHE";
    public static final int BASE_HEADER_SIZE = 20;
    //endregion

    //region Header fields.
    protected QuantizationType quantizationType;
    protected int codebookSize;
    protected int trainFileNameSize;
    protected String trainFileName;
    protected int vectorSizeX;
    protected int vectorSizeY;
    protected int vectorSizeZ;
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
        if (!quantizationType.isOneOf(QuantizationType.Scalar,
                                      QuantizationType.Vector1D,
                                      QuantizationType.Vector2D,
                                      QuantizationType.Vector3D))
            return false;

        if (!U16.isInRange(codebookSize))
            return false;

        if (!U16.isInRange(trainFileNameSize) || trainFileName.length() != trainFileNameSize)
            return false;

        return U16.isInRange(vectorSizeX) && U16.isInRange(vectorSizeY) && U16.isInRange(vectorSizeZ);
    }

    /**
     * Read header from the stream.
     *
     * @param inputStream Data input stream.
     */
    @Override
    public void readFromStream(final DataInputStream inputStream) throws IOException {
        quantizationType = QuantizationType.fromByte(inputStream.readByte());
        codebookSize = inputStream.readUnsignedShort();

        trainFileNameSize = inputStream.readUnsignedShort();

        final byte[] fileNameBuffer = new byte[trainFileNameSize];
        RawDataIO.readFullBuffer(inputStream, fileNameBuffer);
        trainFileName = new String(fileNameBuffer);

        vectorSizeX = inputStream.readUnsignedShort();
        vectorSizeY = inputStream.readUnsignedShort();
        vectorSizeZ = inputStream.readUnsignedShort();
    }

    /**
     * Write QCMP cache file header to stream.
     *
     * @param outputStream Data output stream.
     * @throws IOException when fails to write the header to stream.
     */
    @Override
    public void writeToStream(final DataOutputStream outputStream) throws IOException {
        outputStream.writeBytes(getMagicValue());
        outputStream.writeByte(quantizationType.getValue());
        outputStream.writeShort(codebookSize);

        outputStream.writeShort(trainFileName.length());
        outputStream.writeBytes(trainFileName);

        outputStream.writeShort(vectorSizeX);
        outputStream.writeShort(vectorSizeY);
        outputStream.writeShort(vectorSizeZ);
    }

    @Override
    public long getExpectedDataSize() {
        long expectedFileSize = BASE_HEADER_SIZE + trainFileNameSize;
        expectedFileSize += codebookSize * 8L;          // Frequency values
        switch (quantizationType) {
            case Scalar:
                expectedFileSize += codebookSize * 2L;  // Scalar quantization values
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
        sb.append("HeaderVersion\t: ").append(getHeaderVersion()).append('\n');
        sb.append("Magic\t\t: ").append(getMagicValue()).append('\n');

        sb.append("CodebookType\t: ");
        switch (quantizationType) {
            case Scalar:
                sb.append("Scalar\n");
                break;
            case Vector1D:
                sb.append(String.format("Vector1D [%sx1]\n", vectorSizeX));
                break;
            case Vector2D:
                sb.append(String.format("Vector2D %s\n", new V2i(vectorSizeX, vectorSizeY).toString()));
                break;
            case Vector3D:
                sb.append(String.format("Vector3D %s\n", new V3i(vectorSizeX, vectorSizeY, vectorSizeZ).toString()));
                break;
        }
        sb.append("CodebookSize\t: ").append(codebookSize).append('\n');
        sb.append("TrainFile\t: ").append(trainFileName).append('\n');
    }
    //endregion

    //region Getters and Setters
    public void setQuantizationType(final QuantizationType quantizationType) {
        this.quantizationType = quantizationType;
    }

    public void setCodebookSize(final int codebookSize) {
        this.codebookSize = codebookSize;
    }


    public void setTrainFileName(final String trainFileName) {
        this.trainFileName = trainFileName;
        this.trainFileNameSize = this.trainFileName.length();
    }

    @Override
    public QuantizationType getQuantizationType() {
        return quantizationType;
    }

    @Override
    public int getCodebookSize() {
        return codebookSize;
    }

    @Override
    public int getBitsPerCodebookIndex() {
        return (int) Utils.log2(codebookSize);
    }

    @Override
    public String getTrainFileName() {
        return trainFileName;
    }

    public int getVectorSizeX() {
        return vectorSizeX;
    }

    public int getVectorSizeY() {
        return vectorSizeY;
    }

    public int getVectorSizeZ() {
        return vectorSizeZ;
    }

    @Override
    public V3i getVectorDim() {
        return new V3i(vectorSizeX, vectorSizeY, vectorSizeZ);
    }


    public void setVectorDims(final V3i v3i) {
        this.vectorSizeX = v3i.getX();
        this.vectorSizeY = v3i.getY();
        this.vectorSizeZ = v3i.getZ();
    }

    //endregion
}
