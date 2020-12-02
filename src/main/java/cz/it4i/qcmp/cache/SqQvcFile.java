package cz.it4i.qcmp.cache;

import cz.it4i.qcmp.fileformat.IQvcHeader;
import cz.it4i.qcmp.fileformat.QvcHeaderV2;
import cz.it4i.qcmp.huffman.HuffmanNode;
import cz.it4i.qcmp.huffman.HuffmanTreeBuilder;
import cz.it4i.qcmp.io.InBitStream;
import cz.it4i.qcmp.io.MemoryOutputStream;
import cz.it4i.qcmp.io.OutBitStream;
import cz.it4i.qcmp.quantization.scalar.SQCodebook;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class SqQvcFile implements IQvcFile {
    private IQvcHeader header;
    private SQCodebook codebook;

    public SqQvcFile() {
    }

    public SqQvcFile(final IQvcHeader header, final SQCodebook codebook) {
        this.header = header;
        this.codebook = codebook;
        assert (header.getCodebookSize() == codebook.getCodebookSize());
    }

    @Override
    public void writeToStream(final DataOutputStream outputStream) throws IOException {
        assert (header instanceof QvcHeaderV2) : "Only the latest header is supporter when writing qvc file.";

        final int huffmanTreeBinaryRepresentationSize;
        final MemoryOutputStream bufferStream = new MemoryOutputStream(256);
        try (final OutBitStream bitStream = new OutBitStream(bufferStream, header.getBitsPerCodebookIndex(), 32)) {
            codebook.getHuffmanTreeRoot().writeToBinaryStream(bitStream);
            huffmanTreeBinaryRepresentationSize = (int) bitStream.getBytesWritten();
        }
        assert (huffmanTreeBinaryRepresentationSize == bufferStream.getCurrentBufferLength());
        ((QvcHeaderV2) header).setHuffmanDataSize(huffmanTreeBinaryRepresentationSize);

        header.writeToStream(outputStream);
        
        final int[] quantizationValues = codebook.getCentroids();
        for (final int qV : quantizationValues) {
            outputStream.writeShort(qV);
        }

        outputStream.write(bufferStream.getBuffer(), 0, huffmanTreeBinaryRepresentationSize);
    }

    /**
     * Read codebook from file based on format version.
     *
     * @param inputStream Input stream.
     * @param header      File header.
     * @throws IOException when fails to read from input stream.
     */
    @Override
    public void readFromStream(final DataInputStream inputStream, final IQvcHeader header) throws IOException {
        this.header = header;

        final int headerVersion = header.getHeaderVersion();
        final int codebookSize = header.getCodebookSize();

        final int[] centroids = new int[codebookSize];
        for (int i = 0; i < codebookSize; i++) {
            centroids[i] = inputStream.readUnsignedShort();
        }
        final HuffmanNode huffmanRoot;
        if (headerVersion == 1) {           // First version of qvc file.
            final long[] frequencies = new long[codebookSize];
            for (int i = 0; i < codebookSize; i++) {
                frequencies[i] = inputStream.readLong();
            }

            final HuffmanTreeBuilder builder = new HuffmanTreeBuilder(codebookSize, frequencies);
            builder.buildHuffmanTree();
            huffmanRoot = builder.getRoot();
        } else if (headerVersion == 2) {    // Second version of qvc file.
            final InBitStream bitStream = new InBitStream(inputStream,
                                                          header.getBitsPerCodebookIndex(),
                                                          ((QvcHeaderV2) header).getHuffmanDataSize());
            bitStream.fillEntireBuffer();
            bitStream.setAllowReadFromUnderlyingStream(false);
            huffmanRoot = HuffmanNode.readFromStream(bitStream);
        } else {
            throw new IOException("Unable to read SqQvcFile of version: " + headerVersion);
        }
        codebook = new SQCodebook(centroids, huffmanRoot);
    }

    @Override
    public IQvcHeader getHeader() {
        return header;
    }

    public SQCodebook getCodebook() {
        return codebook;
    }

    @Override
    public void report(final StringBuilder builder) {

        final int[] centroids = codebook.getCentroids();
        for (int i = 0; i < centroids.length; i++) {
            if (i != centroids.length - 1) {
                builder.append(centroids[i]).append(", ");
            } else {
                builder.append(centroids[i]).append('\n');
            }
        }
    }
}
