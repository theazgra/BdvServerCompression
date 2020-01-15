package compression.fileformat;

import compression.U16;

public class QCMPFileHeader {
    public static final String QCMP_MAGIC_VALUE = "QCMPFILE";

    private String magicValue;
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
}