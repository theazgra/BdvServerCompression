package azgracompress.fileformat;

import azgracompress.U16;
import azgracompress.compression.VQImageCompressor;
import azgracompress.data.V3i;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class QCMPFileHeader {
    public static final int BASE_QCMP_HEADER_SIZE = 23;
    public static final String QCMP_MAGIC_VALUE = "QCMPFILE";

    private String magicValue = QCMP_MAGIC_VALUE;
    private QuantizationType quantizationType;
    private byte bitsPerCodebookIndex;
    private boolean codebookPerPlane;

    private int imageSizeX;
    private int imageSizeY;
    private int imageSizeZ;

    private int vectorSizeX;
    private int vectorSizeY;
    private int vectorSizeZ;

    private long[] planeDataSizes;


    /**
     * Validate that all header values are in their valid range.
     *
     * @return True if this is valid QCMPFILE header.
     */
    public boolean validateHeader() {
        if (!magicValue.equals(QCMP_MAGIC_VALUE))
            return false;

        if (bitsPerCodebookIndex == 0)
            return false;

        if (!U16.isInRange(imageSizeX))
            return false;
        if (!U16.isInRange(imageSizeY))
            return false;
        if (!U16.isInRange(imageSizeZ))
            return false;

        if (!U16.isInRange(vectorSizeX))
            return false;
        if (!U16.isInRange(vectorSizeY))
            return false;
        if (!U16.isInRange(vectorSizeZ))
            return false;

        return true;
    }

    public void writeHeader(DataOutputStream outputStream) throws IOException {
        outputStream.writeBytes(QCMP_MAGIC_VALUE);

        outputStream.writeByte(quantizationType.getValue());
        outputStream.writeByte(bitsPerCodebookIndex);
        outputStream.writeBoolean(codebookPerPlane);

        outputStream.writeShort(imageSizeX);
        outputStream.writeShort(imageSizeY);
        outputStream.writeShort(imageSizeZ);

        outputStream.writeShort(vectorSizeX);
        outputStream.writeShort(vectorSizeY);
        outputStream.writeShort(vectorSizeZ);

        // NOTE(Moravec): Allocate space for plane/voxel layers data sizes. Offset: 23.
        final int chunkCount = (quantizationType != QuantizationType.Vector3D)
                ? imageSizeZ
                : VQImageCompressor.calculateVoxelLayerCount(imageSizeZ, vectorSizeZ);

        for (int i = 0; i < chunkCount; i++) {
            outputStream.writeInt(0x0);
        }
    }

    public boolean readHeader(DataInputStream inputStream) throws IOException {
        if (inputStream.available() < BASE_QCMP_HEADER_SIZE) {
            return false;
        }

        byte[] magicBuffer = new byte[QCMP_MAGIC_VALUE.length()];
        final int readFromMagic = inputStream.read(magicBuffer, 0, QCMP_MAGIC_VALUE.length());
        if (readFromMagic != QCMP_MAGIC_VALUE.length()) {
            // Invalid magic value.
            return false;
        }

        magicValue = new String(magicBuffer);
        if (!magicValue.equals(QCMP_MAGIC_VALUE)) {
            return false;
        }

        quantizationType = QuantizationType.fromByte(inputStream.readByte());
        bitsPerCodebookIndex = inputStream.readByte();
        codebookPerPlane = inputStream.readBoolean();

        imageSizeX = inputStream.readUnsignedShort();
        imageSizeY = inputStream.readUnsignedShort();
        imageSizeZ = inputStream.readUnsignedShort();

        vectorSizeX = inputStream.readUnsignedShort();
        vectorSizeY = inputStream.readUnsignedShort();
        vectorSizeZ = inputStream.readUnsignedShort();


        final int chunkCount = (quantizationType != QuantizationType.Vector3D)
                ? imageSizeZ
                : VQImageCompressor.calculateVoxelLayerCount(imageSizeZ, vectorSizeZ);

        planeDataSizes = new long[chunkCount];
        for (int i = 0; i < chunkCount; i++) {
            final long readValue = inputStream.readInt();
            planeDataSizes[i] = (readValue & 0x00000000FFFFFFFFL);
        }

        return true;
    }

    public QuantizationType getQuantizationType() {
        return quantizationType;
    }

    public void setQuantizationType(QuantizationType quantizationType) {
        this.quantizationType = quantizationType;
    }

    public byte getBitsPerCodebookIndex() {
        return bitsPerCodebookIndex;
    }

    public void setBitsPerCodebookIndex(byte bitsPerCodebookIndex) {
        this.bitsPerCodebookIndex = bitsPerCodebookIndex;
    }

    public boolean isCodebookPerPlane() {
        return codebookPerPlane;
    }

    public void setCodebookPerPlane(boolean codebookPerPlane) {
        this.codebookPerPlane = codebookPerPlane;
    }

    public int getImageSizeX() {
        return imageSizeX;
    }

    public void setImageSizeX(int imageSizeX) {
        this.imageSizeX = imageSizeX;
    }

    public int getImageSizeY() {
        return imageSizeY;
    }

    public void setImageSizeY(int imageSizeY) {
        this.imageSizeY = imageSizeY;
    }

    public int getImageSizeZ() {
        return imageSizeZ;
    }

    public V3i getImageDims() {
        return new V3i(imageSizeX, imageSizeY, imageSizeZ);
    }

    public void setImageSizeZ(int imageSizeZ) {
        this.imageSizeZ = imageSizeZ;
    }

    public int getVectorSizeX() {
        return vectorSizeX;
    }

    public void setVectorSizeX(int vectorSizeX) {
        this.vectorSizeX = vectorSizeX;
    }

    public int getVectorSizeY() {
        return vectorSizeY;
    }

    public void setVectorSizeY(int vectorSizeY) {
        this.vectorSizeY = vectorSizeY;
    }

    public int getVectorSizeZ() {
        return vectorSizeZ;
    }

    public void setVectorSizeZ(int vectorSizeZ) {
        this.vectorSizeZ = vectorSizeZ;
    }

    public String getMagicValue() {
        return magicValue;
    }

    public void setImageDimension(final V3i imageDims) {
        imageSizeX = imageDims.getX();
        imageSizeY = imageDims.getY();
        imageSizeZ = imageDims.getZ();
    }

    public void setVectorDimension(final V3i vectorDims) {
        vectorSizeX = vectorDims.getX();
        vectorSizeY = vectorDims.getY();
        vectorSizeZ = vectorDims.getZ();
    }

    public long[] getPlaneDataSizes() {
        return planeDataSizes;
    }

    public void setPlaneDataSizes(final long[] sizes) {
        planeDataSizes = sizes;
    }

    public long getHeaderSize() {
        final int chunkCount = (quantizationType != QuantizationType.Vector3D)
                ? imageSizeZ
                : VQImageCompressor.calculateVoxelLayerCount(imageSizeZ, vectorSizeZ);

        return BASE_QCMP_HEADER_SIZE + (chunkCount * 4);
    }
}