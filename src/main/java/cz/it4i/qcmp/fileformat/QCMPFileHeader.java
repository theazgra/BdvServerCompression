package cz.it4i.qcmp.fileformat;

import cz.it4i.qcmp.U16;
import cz.it4i.qcmp.compression.VQImageCompressor;
import cz.it4i.qcmp.data.V3i;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class QCMPFileHeader implements Cloneable {
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

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public QCMPFileHeader copyOf() {
        try {
            return (QCMPFileHeader) this.clone();
        } catch (final CloneNotSupportedException e) {
            return null;
        }
    }

    public void writeHeader(final DataOutputStream outputStream) throws IOException {
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

    public boolean readHeader(final DataInputStream inputStream) throws IOException {
        if (inputStream.available() < BASE_QCMP_HEADER_SIZE) {
            return false;
        }

        final byte[] magicBuffer = new byte[QCMP_MAGIC_VALUE.length()];

        int toRead = QCMP_MAGIC_VALUE.length();
        while (toRead > 0) {
            final int read = inputStream.read(magicBuffer, QCMP_MAGIC_VALUE.length() - toRead, toRead);
            if (read < 0) {
                // Invalid magic value.
                return false;
            }
            toRead -= read;
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

    public void setQuantizationType(final QuantizationType quantizationType) {
        this.quantizationType = quantizationType;
    }

    public byte getBitsPerCodebookIndex() {
        return bitsPerCodebookIndex;
    }

    public void setBitsPerCodebookIndex(final byte bitsPerCodebookIndex) {
        this.bitsPerCodebookIndex = bitsPerCodebookIndex;
    }

    public boolean isCodebookPerPlane() {
        return codebookPerPlane;
    }

    public void setCodebookPerPlane(final boolean codebookPerPlane) {
        this.codebookPerPlane = codebookPerPlane;
    }

    public int getImageSizeX() {
        return imageSizeX;
    }

    public void setImageSizeX(final int imageSizeX) {
        this.imageSizeX = imageSizeX;
    }

    public int getImageSizeY() {
        return imageSizeY;
    }

    public void setImageSizeY(final int imageSizeY) {
        this.imageSizeY = imageSizeY;
    }

    public int getImageSizeZ() {
        return imageSizeZ;
    }

    public V3i getImageDims() {
        return new V3i(imageSizeX, imageSizeY, imageSizeZ);
    }

    public void setImageSizeZ(final int imageSizeZ) {
        this.imageSizeZ = imageSizeZ;
    }

    public int getVectorSizeX() {
        return vectorSizeX;
    }

    public void setVectorSizeX(final int vectorSizeX) {
        this.vectorSizeX = vectorSizeX;
    }

    public int getVectorSizeY() {
        return vectorSizeY;
    }

    public void setVectorSizeY(final int vectorSizeY) {
        this.vectorSizeY = vectorSizeY;
    }

    public int getVectorSizeZ() {
        return vectorSizeZ;
    }

    public void setVectorSizeZ(final int vectorSizeZ) {
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