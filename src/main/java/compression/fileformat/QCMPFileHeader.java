package compression.fileformat;

import compression.U16;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class QCMPFileHeader {
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

    private void writeHeader(DataOutputStream outputStream) throws IOException {
        outputStream.writeBytes(QCMP_MAGIC_VALUE);

        outputStream.writeByte(quantizationType.getValue());
        outputStream.writeByte(bitsPerPixel);
        outputStream.writeBoolean(codebookPerPlane);

        outputStream.writeInt(imageSizeX);
        outputStream.writeInt(imageSizeY);
        outputStream.writeInt(imageSizeZ);

        outputStream.writeInt(vectorSizeX);
        outputStream.writeInt(vectorSizeY);
        outputStream.writeInt(vectorSizeZ);
    }

    private void readHeader(DataInputStream inputStream) throws IOException {
        magicValue = new String(inputStream.readNBytes(8));

        quantizationType = QuantizationType.fromByte(inputStream.readByte());
        bitsPerPixel = inputStream.readByte();
        codebookPerPlane = inputStream.readBoolean();

        imageSizeX = inputStream.readInt();
        imageSizeY = inputStream.readInt();
        imageSizeZ = inputStream.readInt();

        vectorSizeX = inputStream.readInt();
        vectorSizeY = inputStream.readInt();
        vectorSizeZ = inputStream.readInt();
    }
}