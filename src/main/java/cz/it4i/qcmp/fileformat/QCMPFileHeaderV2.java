package cz.it4i.qcmp.fileformat;

import cz.it4i.qcmp.U16;
import cz.it4i.qcmp.io.RawDataIO;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class QCMPFileHeaderV2 extends QCMPFileHeaderV1 {
    //region Constants
    private static final int VERSION = 2;
    private static final int BASE_QCMP_HEADER_SIZE = 50;
    public static final String MAGIC_VALUE = "QCMPFLV2";
    private static final int RESERVED_BYTES_SIZE = 19;
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
        outputStream.writeBytes(MAGIC_VALUE);

        outputStream.writeByte(quantizationType.getValue());
        outputStream.writeByte(bitsPerCodebookIndex);
        outputStream.writeBoolean(codebookPerPlane);

        outputStream.writeShort(imageSizeX);
        outputStream.writeShort(imageSizeY);
        outputStream.writeShort(imageSizeZ);
        outputStream.writeShort(channelCount);
        outputStream.writeShort(timepointCount);

        outputStream.writeShort(vectorSizeX);
        outputStream.writeShort(vectorSizeY);
        outputStream.writeShort(vectorSizeZ);

        outputStream.writeInt(metadataSize);

        // Reserved bytes.
        for (int i = 0; i < RESERVED_BYTES_SIZE; i++) {
            outputStream.writeByte(0x0);
        }
        assert (metadata.length == metadataSize) : "Metadata size doesn't match with metadata.length";
        outputStream.write(metadata);

        // NOTE(Moravec): Allocate space for plane/voxel layers data sizes. Offset: 23.
        allocateSpaceForChunkSizes(outputStream);
    }

    @Override
    public void readFromStream(final DataInputStream inputStream) throws IOException {
        if (inputStream.available() < BASE_QCMP_HEADER_SIZE) {
            throw new IOException("Provided file is not QCMP v2 file. The file is too small.");
        }

        final byte[] magicValueBuffer = new byte[MAGIC_VALUE.length()];
        RawDataIO.readFullBuffer(inputStream, magicValueBuffer);

        magicValue = new String(magicValueBuffer);
        if (!magicValue.equals(MAGIC_VALUE)) {
            throw new IOException("Provided file is not QCMP v2 file. Magic value is invalid.");
        }

        quantizationType = QuantizationType.fromByte(inputStream.readByte());
        bitsPerCodebookIndex = inputStream.readByte();
        codebookPerPlane = inputStream.readBoolean();

        imageSizeX = inputStream.readUnsignedShort();
        imageSizeY = inputStream.readUnsignedShort();
        imageSizeZ = inputStream.readUnsignedShort();
        channelCount = inputStream.readUnsignedShort();
        timepointCount = inputStream.readUnsignedShort();

        vectorSizeX = inputStream.readUnsignedShort();
        vectorSizeY = inputStream.readUnsignedShort();
        vectorSizeZ = inputStream.readUnsignedShort();

        metadataSize = inputStream.readInt();
        if (metadataSize < 0)
            throw new IOException("Negative metadata size was read from stream.");

        final int skipped = inputStream.skipBytes(RESERVED_BYTES_SIZE);
        if (skipped != RESERVED_BYTES_SIZE)
            throw new IOException("Unable to read QCMPFileHeaderV2. Unable to skip reserved bytes.");

        // TODO(Moravec): Should we always read the full metadata string?
        if (metadataSize > 0) {
            metadata = new byte[metadataSize];
            RawDataIO.readFullBuffer(inputStream, metadata);
        }

        readChunkDataSizes(inputStream);
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


    private long calculateDataSize() {
        long dataSize = 0;
        for (final long chunkSize : chunkDataSizes) {
            dataSize += chunkSize;
        }
        return dataSize;
    }

    @Override
    protected long calculateDataSizeForSq() {
        return calculateDataSize();
    }

    @Override
    protected long calculateDataSizeForVq() {
        return calculateDataSize();
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
        return BASE_QCMP_HEADER_SIZE + metadataSize + (getChunkCount() * 4L);
    }
    //endregion
}