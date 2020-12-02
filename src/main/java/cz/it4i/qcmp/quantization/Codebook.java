package cz.it4i.qcmp.quantization;

import cz.it4i.qcmp.huffman.HuffmanDecoder;
import cz.it4i.qcmp.huffman.HuffmanEncoder;
import cz.it4i.qcmp.huffman.HuffmanNode;
import cz.it4i.qcmp.huffman.HuffmanTreeBuilder;

public class Codebook {

    /**
     * Huffman tree root.
     */
    private final HuffmanNode huffmanRoot;

    // Cached encoder and decoder.
    private HuffmanEncoder huffmanEncoder;
    private HuffmanDecoder huffmanDecoder;

    /**
     * Create base codebook with huffman tree symbol coder.
     *
     * @param huffmanRoot Root of the huffman tree.
     */
    protected Codebook(final HuffmanNode huffmanRoot) {
        this.huffmanRoot = huffmanRoot;
    }


    /**
     * Get huffman decoder of this codebook.
     *
     * @return Symbol decoder.
     */
    public HuffmanDecoder getHuffmanDecoder() {
        if (huffmanDecoder == null)
            huffmanDecoder = new HuffmanDecoder(huffmanRoot);
        return huffmanDecoder;
    }

    /**
     * Get huffman encoder of this codebook.
     *
     * @return Symbol encoder.
     */
    public HuffmanEncoder getHuffmanEncoder() {
        if (huffmanEncoder == null)
            huffmanEncoder = new HuffmanEncoder(huffmanRoot, HuffmanTreeBuilder.createSymbolCodes(huffmanRoot));
        return huffmanEncoder;
    }
}
