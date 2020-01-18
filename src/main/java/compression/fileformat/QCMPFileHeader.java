package compression.fileformat;

import compression.U16;
import compression.data.V2i;
import compression.data.V3i;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class QCMPFileHeader {
    public static final int QCMP_HEADER_SIZE = 23;
    public static final String QCMP_MAGIC_VALUE = "QCMPFILE";

    private String magicValue = QCMP_MAGIC_VALUE;
    private QuantizationType quantizationType;
    private byte bitsPerPixel;
    private boolean codebookPerPlane;

    private int imageSizeX;
    private int imageSizeY;
    private int imageSizeZ;

    private int vectorSizeX;
    private int vectorSizeY;
    private int vectorSizeZ;


    /**
     * Validate that all header values are in their valid range.
     *
     * @return True if this is valid QCMPFILE header.
     */
    public boolean validateHeader() {
        if (!magicValue.equals(QCMP_MAGIC_VALUE))
            return false;

        if (bitsPerPixel == 0)
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
        outputStream.writeByte(bitsPerPixel);
        outputStream.writeBoolean(codebookPerPlane);

        outputStream.writeShort(imageSizeX);
        outputStream.writeShort(imageSizeY);
        outputStream.writeShort(imageSizeZ);

        outputStream.writeShort(vectorSizeX);
        outputStream.writeShort(vectorSizeY);
        outputStream.writeShort(vectorSizeZ);
    }

    public boolean readHeader(DataInputStream inputStream) throws IOException {

        if (inputStream.available() < QCMP_HEADER_SIZE) {
            return false;
        }

        magicValue = new String(inputStream.readNBytes(8));
        if (!magicValue.equals(QCMP_MAGIC_VALUE)) {
            return false;
        }

        quantizationType = QuantizationType.fromByte(inputStream.readByte());
        bitsPerPixel = inputStream.readByte();
        codebookPerPlane = inputStream.readBoolean();

        imageSizeX = inputStream.readUnsignedShort();
        imageSizeY = inputStream.readUnsignedShort();
        imageSizeZ = inputStream.readUnsignedShort();

        vectorSizeX = inputStream.readUnsignedShort();
        vectorSizeY = inputStream.readUnsignedShort();
        vectorSizeZ = inputStream.readUnsignedShort();

        return true;
    }

    public QuantizationType getQuantizationType() {
        return quantizationType;
    }

    public void setQuantizationType(QuantizationType quantizationType) {
        this.quantizationType = quantizationType;
    }

    public byte getBitsPerPixel() {
        return bitsPerPixel;
    }

    public void setBitsPerPixel(byte bitsPerPixel) {
        this.bitsPerPixel = bitsPerPixel;
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

    public void setVectorDimension(final V2i vectorDims) {
        vectorSizeX = vectorDims.getX();
        vectorSizeY = vectorDims.getY();
        vectorSizeZ = 1;
    }
}