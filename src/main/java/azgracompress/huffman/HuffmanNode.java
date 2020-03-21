package azgracompress.huffman;

import org.jetbrains.annotations.NotNull;

public class HuffmanNode implements Comparable<HuffmanNode> {
    private int symbol = -1;
    private long symbolFrequency = -1;

    private int bit = -1;
    private boolean leaf = false;
    private double probability = 0.0;

    final HuffmanNode subNodeA;
    final HuffmanNode subNodeB;

    public HuffmanNode(final int symbol, final double probability, final long frequency) {
        this.symbol = symbol;
        this.probability = probability;
        this.symbolFrequency = frequency;
        subNodeA = null;
        subNodeB = null;
        this.leaf = true;
    }

    public HuffmanNode(final double probability, HuffmanNode parentA, HuffmanNode parentB) {
        this.probability = probability;
        this.subNodeA = parentA;
        this.subNodeB = parentB;
    }

    public HuffmanNode traverse(final boolean queryBit) {
        if (subNodeA != null && subNodeA.bit == (queryBit ? 1 : 0))
            return subNodeA;
        if (subNodeB != null && subNodeB.bit == (queryBit ? 1 : 0))
            return subNodeB;

        assert (false) : "Corrupted huffman tree";
        return null;
    }

    @Override
    public int compareTo(@NotNull HuffmanNode otherNode) {
        return Double.compare(probability, otherNode.probability);
    }

    public void setBit(int bit) {
        this.bit = bit;
    }

    public int getSymbol() {
        return symbol;
    }

    public long getSymbolFrequency() {
        return symbolFrequency;
    }

    public int getBit() {
        return bit;
    }

    public boolean isLeaf() {
        return leaf;
    }

    public double getProbability() {
        return probability;
    }

    public HuffmanNode getSubNodeA() {
        return subNodeA;
    }

    public HuffmanNode getSubNodeB() {
        return subNodeB;
    }
}