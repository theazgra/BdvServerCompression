package cz.it4i.qcmp.fileformat;

import cz.it4i.qcmp.data.V2i;
import cz.it4i.qcmp.data.V3i;
import cz.it4i.qcmp.utilities.Utils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class CacheFileHeader {
    public static final String QCMP_CACHE_MAGIC_VALUE = "QCMPCACHE";
    private String magicValue;
    private QuantizationType quantizationType;

    private int codebookSize;

    private int trainFileNameSize;
    private String trainFileName;

    private int vectorSizeX;
    private int vectorSizeY;
    private int vectorSizeZ;

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

    public void setVectorSizeX(final int vectorSizeX) {
        this.vectorSizeX = vectorSizeX;
    }

    public void setVectorSizeY(final int vectorSizeY) {
        this.vectorSizeY = vectorSizeY;
    }

    public void setVectorSizeZ(final int vectorSizeZ) {
        this.vectorSizeZ = vectorSizeZ;
    }

    public QuantizationType getQuantizationType() {
        return quantizationType;
    }

    public int getCodebookSize() {
        return codebookSize;
    }

    public int getBitsPerCodebookIndex() {
        return (int) Utils.log2(codebookSize);
    }

    public int getTrainFileNameSize() {
        return trainFileNameSize;
    }

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

    public V3i getVectorDim() {
        return new V3i(vectorSizeX, vectorSizeY, vectorSizeZ);
    }

    /**
     * Write QCMP cache file header to stream.
     *
     * @param outputStream Data output stream.
     * @throws IOException when fails to write the header to stream.
     */
    public void writeToStream(final DataOutputStream outputStream) throws IOException {
        outputStream.writeBytes(QCMP_CACHE_MAGIC_VALUE);
        outputStream.writeByte(quantizationType.getValue());
        outputStream.writeShort(codebookSize);

        outputStream.writeShort(trainFileName.length());
        outputStream.writeBytes(trainFileName);

        outputStream.writeShort(vectorSizeX);
        outputStream.writeShort(vectorSizeY);
        outputStream.writeShort(vectorSizeZ);
    }

    public long getExpectedFileSize() {
        long expectedFileSize = 20 + trainFileNameSize; // Base header size
        expectedFileSize += (codebookSize * 8);         // Frequency values
        switch (quantizationType) {
            case Scalar:
                expectedFileSize += (codebookSize * 2); // Scalar quantization values
                break;
            case Vector1D:
            case Vector2D:
            case Vector3D:
                expectedFileSize += ((vectorSizeX * vectorSizeY * vectorSizeZ) * codebookSize * 2); // Quantization vectors
                break;
            case Invalid:
                return -1;
        }
        return expectedFileSize;
    }

    /**
     * Read header from the stream.
     *
     * @param inputStream Data input stream.
     */
    public void readFromStream(final DataInputStream inputStream) throws IOException {
        final int MIN_AVAIL = 9;
        if (inputStream.available() < MIN_AVAIL) {
            throw new IOException("Invalid file. File too small.");
        }

        final byte[] magicBuffer = new byte[QCMP_CACHE_MAGIC_VALUE.length()];

        int toRead = QCMP_CACHE_MAGIC_VALUE.length();
        while (toRead > 0) {
            final int read = inputStream.read(magicBuffer, QCMP_CACHE_MAGIC_VALUE.length() - toRead, toRead);
            if (read < 0) {
                throw new IOException("Invalid file type. Unable to read magic value");
            }
            toRead -= read;
        }

        magicValue = new String(magicBuffer);
        if (!magicValue.equals(QCMP_CACHE_MAGIC_VALUE)) {
            throw new IOException("Invalid file type. Wrong magic value.");
        }
        quantizationType = QuantizationType.fromByte(inputStream.readByte());
        codebookSize = inputStream.readUnsignedShort();

        trainFileNameSize = inputStream.readUnsignedShort();
        final byte[] fileNameBuffer = new byte[trainFileNameSize];


        toRead = trainFileNameSize;
        while (toRead > 0) {
            toRead -= inputStream.read(fileNameBuffer, trainFileNameSize - toRead, toRead);
        }


        trainFileName = new String(fileNameBuffer);

        vectorSizeX = inputStream.readUnsignedShort();
        vectorSizeY = inputStream.readUnsignedShort();
        vectorSizeZ = inputStream.readUnsignedShort();
    }

    public void setVectorDims(final V3i v3i) {
        this.vectorSizeX = v3i.getX();
        this.vectorSizeY = v3i.getY();
        this.vectorSizeZ = v3i.getZ();
    }

    public void report(final StringBuilder sb) {
        sb.append("Magic: ").append(magicValue).append('\n');

        sb.append("CodebookType: ");
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
        sb.append("CodebookSize: ").append(codebookSize).append('\n');
        sb.append("TrainFile: ").append(trainFileName).append('\n');
    }
}
