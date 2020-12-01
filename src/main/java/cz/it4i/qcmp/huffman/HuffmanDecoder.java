package cz.it4i.qcmp.huffman;

import cz.it4i.qcmp.io.InBitStream;

import java.io.IOException;

/**
 * Simple wrapper around root huffman node to provide easy decode function.
 */
public class HuffmanDecoder {
    private final HuffmanNode root;

    /**
     * Create huffman decoder from the root node.
     *
     * @param root Root huffman node.
     */
    public HuffmanDecoder(final HuffmanNode root) {
        this.root = root;
    }

    /**
     * Decode huffman symbol by reading binary code from stream.
     *
     * @param inBitStream Binary input stream.
     * @return Decoded symbol.
     * @throws IOException when fails to read from input stream.
     */
    public int decodeSymbol(final InBitStream inBitStream) throws IOException {
        HuffmanNode currentNode = root;
        while (!currentNode.isLeaf()) {
            currentNode = currentNode.traverse(inBitStream.readBit());
        }
        return currentNode.getSymbol();
    }

    public HuffmanNode getRoot() {
        return root;
    }
}
