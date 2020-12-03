package cz.it4i.qcmp.cache;

import cz.it4i.qcmp.fileformat.IQvcHeader;
import cz.it4i.qcmp.fileformat.QvcHeaderV2;
import cz.it4i.qcmp.huffman.HuffmanNode;
import cz.it4i.qcmp.huffman.HuffmanTreeBuilder;
import cz.it4i.qcmp.io.InBitStream;
import cz.it4i.qcmp.io.MemoryOutputStream;
import cz.it4i.qcmp.io.OutBitStream;
import cz.it4i.qcmp.quantization.vector.VQCodebook;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class VqQvcFile implements IQvcFile {
    private IQvcHeader header;
    private VQCodebook codebook;

    public VqQvcFile() {
    }

    public VqQvcFile(final IQvcHeader header, final VQCodebook codebook) {
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

        final int[][] entries = codebook.getVectors();
        for (final int[] entry : entries) {
            for (final int vectorValue : entry) {
                outputStream.writeShort(vectorValue);
            }
        }

        outputStream.write(bufferStream.getBuffer(), 0, huffmanTreeBinaryRepresentationSize);
    }

    @Override
    public void readFromStream(final DataInputStream inputStream, final IQvcHeader header) throws IOException {
        this.header = header;
        final int codebookSize = header.getCodebookSize();

        final int entrySize = header.getVectorDim().multiplyTogether();
        final int[][] vectors = new int[codebookSize][entrySize];

        for (int i = 0; i < codebookSize; i++) {
            for (int j = 0; j < entrySize; j++) {
                vectors[i][j] = inputStream.readUnsignedShort();
            }
        }

        final HuffmanNode huffmanRoot;
        final int headerVersion = header.getHeaderVersion();
        if (headerVersion == 1) {
            final long[] frequencies = new long[codebookSize];
            for (int i = 0; i < codebookSize; i++) {
                frequencies[i] = inputStream.readLong();
            }
            final HuffmanTreeBuilder builder = new HuffmanTreeBuilder(codebookSize, frequencies);
            builder.buildHuffmanTree();
            huffmanRoot = builder.getRoot();
        } else if (headerVersion == 2) {
            final InBitStream bitStream = new InBitStream(inputStream,
                                                          header.getBitsPerCodebookIndex(),
                                                          ((QvcHeaderV2) header).getHuffmanDataSize());
            bitStream.fillEntireBuffer();
            bitStream.setAllowReadFromUnderlyingStream(false);
            huffmanRoot = HuffmanNode.readFromStream(bitStream);
        } else {
            throw new IOException("Unable to read VqQvcFile of version: " + headerVersion);
        }

        codebook = new VQCodebook(header.getVectorDim(), vectors, huffmanRoot);
    }

    @Override
    public IQvcHeader getHeader() {
        return header;
    }

    public VQCodebook getCodebook() {
        return codebook;
    }

    @Override
    public void convertToNewerVersion(final boolean inPlace, final String inputPath, final String outputPath) {
        assert false : "NOT IMPLEMENTED YET";
    }

    @Override
    public void report(final StringBuilder builder) {
        final int[][] vectors = codebook.getVectors();
        builder.append("\n- - - - - - - - - - - - - - - - - - - - - - - - -\n");
        for (final int[] vector : vectors) {
            for (final int x : vector) {
                builder.append(x).append(';');
            }
            builder.append("\n- - - - - - - - - - - - - - - - - - - - - - - - -\n");
        }


    }
}
