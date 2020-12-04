package cz.it4i.qcmp.fileformat;

import cz.it4i.qcmp.U16;
import cz.it4i.qcmp.compression.VQImageCompressor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class QCMPFileHeaderV2 extends QCMPFileHeaderV1 {
    //region Constants
    private static final int VERSION = 2;
    private static final int BASE_QCMP_HEADER_SIZE = 50;
    public static final String MAGIC_VALUE = "QCMPFLV2";
    //endregion

    //region Header fields
    private int channelCount;
    private int timepointCount;
    private int metadataSize;
    private byte[] metadata;
    //endregion

    //region IFileHeader implementation

    /**
     * Validate that all header values are in their valid range.
     *
     * @return True if this is valid QCMPFILE header.
     */
    @Override
    public boolean validateHeader() {
        if (!super.validateHeader())
            return false;

        if (!U16.isInRange(channelCount))
            return false;
        if (!U16.isInRange(timepointCount))
            return false;

        return metadataSize >= 0;
    }

    @Override
    public void writeToStream(final DataOutputStream outputStream) throws IOException {
        // TODO
    }

    @Override
    public void readFromStream(final DataInputStream inputStream) throws IOException {
        // TODO
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
                .append(imageSizeZ).append('x')
                .append(channelCount).append('x')
                .append(timepointCount).append(" [XxYxZxCxT]").append('\n');

        builder.append("Quantization vector\t: ")
                .append(vectorSizeX).append('x')
                .append(vectorSizeY).append('x')
                .append(vectorSizeZ).append('\n');

        builder.append("MetadataSize\t: ").append(metadataSize).append('\n');

        printFileSizeInfo(builder, inputFile);
    }

    @Override
    protected long calculateDataSizeForSq() {
        // TODO(Moravec): Fix this calculation. Size of the huffman tree will be added to chunk size.
        final int LONG_BYTES = 8;
        // Quantization value count.
        final int codebookSize = (int) Math.pow(2, bitsPerCodebookIndex);

        // Total codebook size in bytes. Also symbol frequencies for Huffman.
        final long codebookDataSize = ((2 * codebookSize) + (LONG_BYTES * codebookSize)) * (codebookPerPlane ? imageSizeZ : 1);

        // Indices are encoded using huffman. Plane data size is written in the header.
        long totalPlaneDataSize = 0;
        for (final long planeDataSize : planeDataSizes) {
            totalPlaneDataSize += planeDataSize;
        }

        return (codebookDataSize + totalPlaneDataSize);
    }

    @Override
    protected long calculateDataSizeForVq() {
        // TODO(Moravec): Fix this calculation. Size of the huffman tree will be added to chunk size.
        final int LONG_BYTES = 8;
        // Vector count in codebook
        final int codebookSize = (int) Math.pow(2, bitsPerCodebookIndex);

        // Single vector size in bytes.
        final int vectorDataSize = 2 * vectorSizeX * vectorSizeY * vectorSizeZ;

        // Total codebook size in bytes.
        final long codebookDataSize = ((codebookSize * vectorDataSize) + (codebookSize * LONG_BYTES)) * (codebookPerPlane ? imageSizeZ : 1);

        // Indices are encoded using huffman. Plane data size is written in the header.
        long totalPlaneDataSize = 0;
        for (final long planeDataSize : planeDataSizes) {
            totalPlaneDataSize += planeDataSize;
        }
        return (codebookDataSize + totalPlaneDataSize);
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

    public QCMPFileHeaderV2 copyOf() {
        try {
            return (QCMPFileHeaderV2) this.clone();
        } catch (final CloneNotSupportedException e) {
            return null;
        }
    }
    //endregion

    //region Getters and Setters


    public int getChannelCount() {
        return channelCount;
    }

    public void setChannelCount(final int channelCount) {
        this.channelCount = channelCount;
    }

    public int getTimepointCount() {
        return timepointCount;
    }

    public void setTimepointCount(final int timepointCount) {
        this.timepointCount = timepointCount;
    }

    public int getMetadataSize() {
        return metadataSize;
    }

    public void setMetadataSize(final int metadataSize) {
        this.metadataSize = metadataSize;
    }

    public byte[] getMetadata() {
        return metadata;
    }

    public void setMetadata(final byte[] metadata) {
        this.metadata = metadata;
    }

    public long getHeaderSize() {
        final int chunkCount = (quantizationType != QuantizationType.Vector3D)
                ? imageSizeZ
                : VQImageCompressor.calculateVoxelLayerCount(imageSizeZ, vectorSizeZ);

        return BASE_QCMP_HEADER_SIZE + metadataSize + (chunkCount * 4L);
    }
    //endregion
}