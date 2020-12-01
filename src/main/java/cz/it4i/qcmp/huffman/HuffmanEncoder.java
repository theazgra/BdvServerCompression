package cz.it4i.qcmp.huffman;

import java.util.HashMap;

/**
 * Simple wrapper around root huffman symbol map to provide easy encode function.
 */
public class HuffmanEncoder {
    private final HuffmanNode root;
    private final HashMap<Integer, boolean[]> symbolCodes;

    /**
     * Create huffman encoder from symbol map.
     *
     * @param root        Huffman tree root.
     * @param symbolCodes Huffman symbol map.
     */
    public HuffmanEncoder(final HuffmanNode root, final HashMap<Integer, boolean[]> symbolCodes) {
        this.root = root;
        this.symbolCodes = symbolCodes;
    }

    /**
     * Get binary code for huffman symbol.
     *
     * @param symbol Huffman symbol.
     * @return Binary code.
     */
    public boolean[] getSymbolCode(final int symbol) {
        return symbolCodes.get(symbol);
    }

    public HuffmanNode getRoot() {
        return root;
    }
}
