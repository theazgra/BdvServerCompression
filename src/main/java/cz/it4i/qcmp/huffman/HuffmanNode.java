package cz.it4i.qcmp.huffman;

import cz.it4i.qcmp.io.InBitStream;
import cz.it4i.qcmp.io.OutBitStream;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class HuffmanNode implements Comparable<HuffmanNode> {
    private int symbol = -1;

    private int bit = -1;
    private boolean leaf = false;
    private double probability = 0.0;

    final HuffmanNode rightChild;
    final HuffmanNode leftChild;

    public HuffmanNode(final int symbol, final double probability) {
        this.symbol = symbol;
        this.probability = probability;
        rightChild = null;
        leftChild = null;
        this.leaf = true;
    }

    private HuffmanNode(final HuffmanNode rightChild, final HuffmanNode leftChild) {
        this.rightChild = rightChild;
        this.leftChild = leftChild;
    }

    public static HuffmanNode constructWithSymbol(final HuffmanNode rightChild, final HuffmanNode leftChild, final int symbol) {
        final HuffmanNode node = new HuffmanNode(rightChild, leftChild);
        node.symbol = symbol;
        node.leaf = (rightChild == null && leftChild == null);
        return node;
    }

    public static HuffmanNode constructWithProbability(final HuffmanNode parentA, final HuffmanNode parentB, final double probability) {
        final HuffmanNode node = new HuffmanNode(parentA, parentB);
        node.probability = probability;
        return node;
    }

    public HuffmanNode traverse(final boolean queryBit) {
        if (rightChild != null && rightChild.bit == (queryBit ? 1 : 0))
            return rightChild;
        if (leftChild != null && leftChild.bit == (queryBit ? 1 : 0))
            return leftChild;

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

    public int getBit() {
        return bit;
    }

    public boolean isLeaf() {
        return leaf;
    }

    public double getProbability() {
        return probability;
    }

    public HuffmanNode getRightChild() {
        return rightChild;
    }

    public HuffmanNode getLeftChild() {
        return leftChild;
    }

    /**
     * Check if two huffman nodes are value equal and if their subtrees are also equal.
     *
     * @param nodeA First huffman node.
     * @param nodeB Second huffman code.
     * @return True if nodes and their subtrees are equal.
     */
    private static boolean treeNodeEquality(final HuffmanNode nodeA, final HuffmanNode nodeB) {
        if (nodeA.leaf) {
            if (!nodeB.leaf) {
                return false;
            }
            return nodeA.symbol == nodeB.symbol;
        } else {
            if (nodeB.leaf) {
                return false;
            }
            if (nodeA.bit != nodeB.bit)
                return false;

            if ((nodeA.rightChild != null && nodeB.rightChild == null) || (nodeA.rightChild == null && nodeB.rightChild != null))
                return false;
            if ((nodeA.leftChild != null && nodeB.leftChild == null) || (nodeA.leftChild == null && nodeB.leftChild != null))
                return false;


            assert (nodeA.rightChild != null) : "Current node is not leaf and right child must be set.";
            assert (nodeA.leftChild != null) : "Current node is not leaf and left child must be set.";

            final boolean subTreeAResult = treeNodeEquality(nodeA.rightChild, nodeB.rightChild);
            final boolean subTreeBResult = treeNodeEquality(nodeA.leftChild, nodeB.leftChild);
            return (subTreeAResult && subTreeBResult);
        }
    }

    /**
     * Check if tree starting from this node is equal to one starting from otherRoot.
     *
     * @param otherRoot Other tree root node.
     * @return True if both trees are value equal.
     */
    public boolean treeEqual(final HuffmanNode otherRoot) {
        return treeNodeEquality(this, otherRoot);
    }

    /**
     * Save current node and its children to binary stream.
     *
     * @param node      Node to write to stream.
     * @param bitStream Binary output stream.
     * @throws IOException when fails to write to stream.
     */
    private void writeToBinaryStreamImpl(final HuffmanNode node, final OutBitStream bitStream) throws IOException {
        if (node.isLeaf()) {
            bitStream.writeBit(1);
            bitStream.write(node.getSymbol());
        } else {
            bitStream.writeBit(0);
            writeToBinaryStreamImpl(node.getRightChild(), bitStream);
            writeToBinaryStreamImpl(node.getLeftChild(), bitStream);
        }
    }

    /**
     * Save huffman tree from this node to the binary stream.
     *
     * @param bitStream Binary output stream.
     * @throws IOException when fails to write to stream.
     */
    public void writeToBinaryStream(final OutBitStream bitStream) throws IOException {
        writeToBinaryStreamImpl(this, bitStream);
    }

    /**
     * Read huffman tree from the binary stream.
     *
     * @param bitStream Binary input stream.
     * @return Root of the huffman tree.
     * @throws IOException when fails to read from stream.
     */
    public static HuffmanNode readFromStream(final InBitStream bitStream) throws IOException {
        if (bitStream.readBit()) // Leaf
        {
            return HuffmanNode.constructWithSymbol(null, null, bitStream.readValue());
        } else {
            final HuffmanNode rightChild = readFromStream(bitStream);
            rightChild.setBit(1);
            final HuffmanNode leftChild = readFromStream(bitStream);
            leftChild.setBit(0);
            return HuffmanNode.constructWithSymbol(rightChild, leftChild, -1);
        }
    }
}