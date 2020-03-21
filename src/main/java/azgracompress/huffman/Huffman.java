package azgracompress.huffman;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.PriorityQueue;

public class Huffman {

    class Node implements Comparable<Node> {
        private int symbol = -1;
        private long symbolFrequency = -1;

        private boolean bit;
        private boolean leaf = false;
        private double probability = 0.0;

        final Node subNodeA;
        final Node subNodeB;

        public Node(final int symbol, final double probability, final long frequency) {
            this.symbol = symbol;
            this.probability = probability;
            this.symbolFrequency = frequency;
            subNodeA = null;
            subNodeB = null;
            this.leaf = true;
        }

        public Node(final double probability, Node parentA, Node parentB) {
            this.probability = probability;
            this.subNodeA = parentA;
            this.subNodeB = parentB;
        }

        Node traverse(final boolean bit) {
            if (subNodeA != null && subNodeA.bit == bit)
                return subNodeA;
            if (subNodeB != null && subNodeB.bit == bit)
                return subNodeB;

            assert (false) : "Corrupted huffman tree";
            return null;
        }

        @Override
        public int compareTo(@NotNull Huffman.Node otherNode) {
            return Double.compare(probability, otherNode.probability);
        }
    }

    Node root = null;
    HashMap<Integer, boolean[]> symbolCodes;
    final int[] symbols;
    final long[] symbolFrequencies;

    public Huffman(int[] symbols, long[] symbolFrequencies) {
        assert (symbols.length == symbolFrequencies.length) : "Array lengths mismatch";
        this.symbols = symbols;
        this.symbolFrequencies = symbolFrequencies;
    }

    public void buildHuffmanTree() {
        PriorityQueue<Node> queue = buildPriorityQueue();


        while (queue.size() != 1) {
            final Node parentA = queue.poll();
            final Node parentB = queue.poll();
            assert (parentA.probability <= parentB.probability);
            assert (parentA != null && parentB != null);

            parentA.bit = true;
            parentB.bit = false;

            final double mergedProbabilities = parentA.probability + parentB.probability;
            final Node mergedNode = new Node(mergedProbabilities, parentA, parentB);
            queue.add(mergedNode);
        }
        root = queue.poll();
        buildHuffmanCodes();
    }

    private void buildHuffmanCodes() {
        symbolCodes = new HashMap<>(symbols.length);

        traverseSymbolCodes(root, new ArrayList<Boolean>());
    }

    private void traverseSymbolCodes(Node currentNode, ArrayList<Boolean> currentCode) {
        boolean inLeaf = true;
        if (!currentNode.leaf) {
            currentCode.add(currentNode.bit);
        }

        if (currentNode.subNodeA != null) {
            ArrayList<Boolean> codeCopy = new ArrayList<Boolean>(currentCode);
            traverseSymbolCodes(currentNode.subNodeA, codeCopy);
            inLeaf = false;
        }
        if (currentNode.subNodeB != null) {
            ArrayList<Boolean> codeCopy = new ArrayList<Boolean>(currentCode);
            traverseSymbolCodes(currentNode.subNodeB, codeCopy);
            inLeaf = false;
        }

        if (inLeaf) {
            assert (currentNode.leaf);

            boolean[] finalSymbolCode = new boolean[currentCode.size()];
            for (int i = 0; i < finalSymbolCode.length; i++) {
                finalSymbolCode[i] = currentCode.get(i);
            }
            symbolCodes.put(currentNode.symbol, finalSymbolCode);
        }

    }

    private PriorityQueue<Node> buildPriorityQueue() {
        double totalFrequency = 0.0;
        for (final long symbolFrequency : symbolFrequencies) {
            totalFrequency += symbolFrequency;
        }

        PriorityQueue<Node> queue = new PriorityQueue<>(symbols.length);

        for (int sIndex = 0; sIndex < symbols.length; sIndex++) {
            final double symbolProbability = (double) symbolFrequencies[sIndex] / totalFrequency;
            queue.add(new Node(symbols[sIndex], symbolProbability, symbolFrequencies[sIndex]));
        }

        return queue;
    }


    public boolean[] getCode(final int symbol) {
        return symbolCodes.get(symbol);
    }

    public Node getRoot() {
        return root;
    }
}
