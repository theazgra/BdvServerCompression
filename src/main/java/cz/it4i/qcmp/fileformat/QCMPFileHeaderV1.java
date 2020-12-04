package cz.it4i.qcmp.fileformat;

import cz.it4i.qcmp.U16;
import cz.it4i.qcmp.compression.VQImageCompressor;
import cz.it4i.qcmp.data.V3i;
import cz.it4i.qcmp.io.RawDataIO;
import cz.it4i.qcmp.utilities.Utils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

public class QCMPFileHeaderV1 implements IFileHeader, Cloneable {
    //region Constants
    private static final int VERSION = 1;
    private static final int BASE_QCMP_HEADER_SIZE = 23;
    public static final String MAGIC_VALUE = "QCMPFILE";
    //endregion

    //region Header fields
    protected String magicValue = MAGIC_VALUE;
    protected QuantizationType quantizationType;
    protected byte bitsPerCodebookIndex;
    protected boolean codebookPerPlane;

    protected int imageSizeX;
    protected int imageSizeY;
    protected int imageSizeZ;

    protected int vectorSizeX;
    protected int vectorSizeY;
    protected int vectorSizeZ;

    protected long[] chunkDataSizes;
    //endregion

    //region IFileHeader implementation

    /**
     * Validate that all header values are in their valid range.
     *
     * @return True if this is valid QCMPFILE header.
     */
    @Override
    public boolean validateHeader() {
        if (!magicValue.equals(getMagicValue()))
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
        return U16.isInRange(vectorSizeZ);
    }

    @Override
    public void writeToStream(final DataOutputStream outputStream) throws IOException {
        outputStream.writeBytes(MAGIC_VALUE);

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

    @Override
    public void readFromStream(final DataInputStream inputStream) throws IOException {
        if (inputStream.available() < BASE_QCMP_HEADER_SIZE) {
            throw new IOException("Provided file is not QCMP file. The file is too small.");
        }

        final byte[] magicValueBuffer = new byte[MAGIC_VALUE.length()];
        RawDataIO.readFullBuffer(inputStream, magicValueBuffer);

        magicValue = new String(magicValueBuffer);
        if (!magicValue.equals(MAGIC_VALUE)) {
            throw new IOException("Provided file is not QCMP file. Magic value is invalid.");
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

        chunkDataSizes = new long[chunkCount];
        for (int i = 0; i < chunkCount; i++) {
            final long readValue = inputStream.readInt();
            chunkDataSizes[i] = (readValue & 0x00000000FFFFFFFFL);
        }
    }

    @Override
    public int getHeaderVersion() {
        return VERSION;
    }

    @Override
    public void report(final StringBuilder builder, final String inputFile) {
        if (!validateHeader()) {
            builder.append("Header is:\t\t invalid\n");
            return;
        }
        builder.append("HeaderVersion\t\t: ").append(getHeaderVersion()).append('\n');
        builder.append("Magic value\t\t: ").append(getMagicValue()).append('\n');
        builder.append("Quantization type\t: ").append(quantizationType).append('\n');

        builder.append("Bits per pixel\t\t: ").append(bitsPerCodebookIndex).append('\n');

        builder.append("Codebook\t\t: ").append(codebookPerPlane ? "one per plane\n" : "one for all\n");

        final int codebookSize = (int) Math.pow(2, bitsPerCodebookIndex);
        builder.append("Codebook size\t\t: ").append(codebookSize).append('\n');

        builder.append("Image stack size\t: ")
                .append(imageSizeX).append('x')
                .append(imageSizeY).append('x')
                .append(imageSizeZ).append('\n');

        builder.append("Quantization vector\t: ")
                .append(vectorSizeX).append('x')
                .append(vectorSizeY).append('x')
                .append(vectorSizeZ).append('\n');

        printFileSizeInfo(builder, inputFile);
    }

    protected void printFileSizeInfo(final StringBuilder builder, final String inputFile) {
        final long headerSize = getHeaderSize();
        final long fileSize = new File(inputFile).length();
        final long dataSize = fileSize - headerSize;

        final long expectedDataSize = getExpectedDataSize();
        final boolean correctFileSize = (dataSize == expectedDataSize);

        builder.append("File size\t\t: ");
        Utils.prettyPrintFileSize(builder, fileSize).append('\n');

        builder.append("Header size\t\t: ").append(headerSize).append(" Bytes\n");
        builder.append("Data size\t\t: ");
        Utils.prettyPrintFileSize(builder, dataSize).append(correctFileSize ? "(correct)\n" : "(INVALID)\n");

        final long pixelCount = (long) imageSizeX * imageSizeY * imageSizeZ;
        final long uncompressedSize = 2 * pixelCount; // We assert 16 bit (2 byte) pixel.
        final double compressionRatio = (double) fileSize / (double) uncompressedSize;
        builder.append(String.format("Compression ratio\t: %.4f\n", compressionRatio));

        final double BPP = ((double) fileSize * 8.0) / (double) pixelCount;
        builder.append(String.format("Bits Per Pixel (BPP)\t: %.4f\n", BPP));

        builder.append("\n=== Input file is ").append(correctFileSize ? "VALID" : "INVALID").append(" ===\n");
    }

    protected long calculateDataSizeForSq() {
        final int LONG_BYTES = 8;
        // Quantization value count.
        final int codebookSize = (int) Math.pow(2, bitsPerCodebookIndex);

        // Total codebook size in bytes. Also symbol frequencies for Huffman.
        final long codebookDataSize = ((2 * codebookSize) + (LONG_BYTES * codebookSize)) * (codebookPerPlane ? imageSizeZ : 1);

        // Indices are encoded using huffman. Plane data size is written in the header.
        long totalPlaneDataSize = 0;
        for (final long planeDataSize : chunkDataSizes) {
            totalPlaneDataSize += planeDataSize;
        }

        return (codebookDataSize + totalPlaneDataSize);
    }

    protected long calculateDataSizeForVq() {
        final int LONG_BYTES = 8;
        // Vector count in codebook
        final int codebookSize = (int) Math.pow(2, bitsPerCodebookIndex);

        // Single vector size in bytes.
        final int vectorDataSize = 2 * vectorSizeX * vectorSizeY * vectorSizeZ;

        // Total codebook size in bytes.
        final long codebookDataSize = ((codebookSize * vectorDataSize) + (codebookSize * LONG_BYTES)) * (codebookPerPlane ? imageSizeZ : 1);

        // Indices are encoded using huffman. Plane data size is written in the header.
        long totalPlaneDataSize = 0;
        for (final long planeDataSize : chunkDataSizes) {
            totalPlaneDataSize += planeDataSize;
        }
        return (codebookDataSize + totalPlaneDataSize);
    }

    @Override
    public long getExpectedDataSize() {
        switch (quantizationType) {
            case Scalar:
                return calculateDataSizeForSq();
            case Vector1D:
            case Vector2D:
            case Vector3D:
                return calculateDataSizeForVq();
        }
        return -1;
    }

    @Override
    public String getMagicValue() {
        return MAGIC_VALUE;
    }

    //endregion

    //region Cloneable implementation
    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public QCMPFileHeaderV1 copyOf() {
        try {
            return (QCMPFileHeaderV1) this.clone();
        } catch (final CloneNotSupportedException e) {
            return null;
        }
    }
    //endregion

    //region Getters and Setters
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

    public long[] getChunkDataSizes() {
        return chunkDataSizes;
    }

    public void setChunkDataSizes(final long[] sizes) {
        chunkDataSizes = sizes;
    }

    public long getHeaderSize() {
        final int chunkCount = (quantizationType != QuantizationType.Vector3D)
                ? imageSizeZ
                : VQImageCompressor.calculateVoxelLayerCount(imageSizeZ, vectorSizeZ);

        return BASE_QCMP_HEADER_SIZE + (chunkCount * 4);
    }
    //endregion
}