package cz.it4i.qcmp.huffman;

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

    private HuffmanNode(final HuffmanNode parentA, final HuffmanNode parentB) {
        subNodeA = parentA;
        subNodeB = parentB;
    }

    public static HuffmanNode constructWithSymbol(final HuffmanNode parentA, final HuffmanNode parentB, final int symbol) {
        final HuffmanNode node = new HuffmanNode(parentA, parentB);
        node.symbol = symbol;
        node.leaf = (parentA == null && parentB == null);
        return node;
    }

    public static HuffmanNode constructWithProbability(final HuffmanNode parentA, final HuffmanNode parentB, final double probability) {
        final HuffmanNode node = new HuffmanNode(parentA, parentB);
        node.probability = probability;
        return node;
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
    public int compareTo(@NotNull final HuffmanNode otherNode) {
        return Double.compare(probability, otherNode.probability);
    }

    public void setBit(final int bit) {
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

    private static boolean treeNodeEquality(final HuffmanNode A, final HuffmanNode B) {
        if (A.leaf) {
            if (!B.leaf) {
                return false;
            }
            return A.symbol == B.symbol;
        } else {
            if (B.leaf) {
                return false;
            }
            if (A.bit != B.bit)
                return false;

            if ((A.subNodeA != null && B.subNodeA == null) || (A.subNodeA == null && B.subNodeA != null))
                return false;
            if ((A.subNodeB != null && B.subNodeB == null) || (A.subNodeB == null && B.subNodeB != null))
                return false;

            final boolean subTreeAResult = treeNodeEquality(A.subNodeA, B.subNodeA);
            final boolean subTreeBResult = treeNodeEquality(A.subNodeB, B.subNodeB);
            return (subTreeAResult && subTreeBResult);
        }
    }

    public boolean treeEqual(final HuffmanNode opposite) {
        return treeNodeEquality(this, opposite);
    }
}