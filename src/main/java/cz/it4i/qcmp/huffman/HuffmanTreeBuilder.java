package cz.it4i.qcmp.huffman;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.PriorityQueue;

public class HuffmanTreeBuilder {
    private HuffmanNode root = null;
    private HashMap<Integer, boolean[]> symbolCodes;
    private final int[] symbols;
    private final long[] symbolFrequencies;

    public HuffmanTreeBuilder(final int codebookSize, final long[] symbolFrequencies) {
        assert (codebookSize == symbolFrequencies.length) : "Array lengths mismatch";
        this.symbolFrequencies = symbolFrequencies;
        this.symbols = new int[codebookSize];
        for (int i = 0; i < codebookSize; i++) {
            symbols[i] = i;
        }
    }

    public HuffmanTreeBuilder(final int[] symbols, final long[] symbolFrequencies) {
        assert (symbols.length == symbolFrequencies.length) : "Array lengths mismatch";
        this.symbols = symbols;
        this.symbolFrequencies = symbolFrequencies;
    }

    public void buildHuffmanTree() {
        final PriorityQueue<HuffmanNode> queue = buildPriorityQueue();


        while (queue.size() != 1) {
            final HuffmanNode parentA = queue.poll();
            final HuffmanNode parentB = queue.poll();
            if (!(parentA.getProbability() <= parentB.getProbability())) {
                System.err.printf("Parent A prob: %.6f\nParent B prob: %.6f%n", parentA.getProbability(), parentB.getProbability());
                assert (parentA.getProbability() <= parentB.getProbability());
            }

            parentA.setBit(1);
            parentB.setBit(0);

            final double mergedProbabilities = parentA.getProbability() + parentB.getProbability();
            final HuffmanNode mergedNode = HuffmanNode.constructWithProbability(parentA, parentB, mergedProbabilities);
            queue.add(mergedNode);
        }
        root = queue.poll();
        symbolCodes = createSymbolCodes(root);
    }

    public static HashMap<Integer, boolean[]> createSymbolCodes(final HuffmanNode node) {
        final HashMap<Integer, boolean[]> codes = new HashMap<>();
        createSymbolCodesImpl(codes, node, new ArrayList<Boolean>());
        return codes;
    }

    private static void createSymbolCodesImpl(final HashMap<Integer, boolean[]> codes,
                                              final HuffmanNode currentNode,
                                              final ArrayList<Boolean> currentCode) {
        boolean inLeaf = true;
        final int bit = currentNode.getBit();
        if (bit != -1) {
            currentCode.add(bit == 1);
        }

        if (currentNode.rightChild != null) {
            final ArrayList<Boolean> codeCopy = new ArrayList<Boolean>(currentCode);
            createSymbolCodesImpl(codes, currentNode.rightChild, codeCopy);
            inLeaf = false;
        }
        if (currentNode.leftChild != null) {
            final ArrayList<Boolean> codeCopy = new ArrayList<Boolean>(currentCode);
            createSymbolCodesImpl(codes, currentNode.leftChild, codeCopy);
            inLeaf = false;
        }

        if (inLeaf) {
            assert (currentNode.isLeaf());
            //currentNode.setIsLeaf(true);

            final boolean[] finalSymbolCode = new boolean[currentCode.size()];
            for (int i = 0; i < finalSymbolCode.length; i++) {
                finalSymbolCode[i] = currentCode.get(i);
            }
            codes.put(currentNode.getSymbol(), finalSymbolCode);
        }
    }

    private PriorityQueue<HuffmanNode> buildPriorityQueue() {
        final HashMap<Integer, Double> symbolProbabilityMap = new HashMap<>(symbols.length);
        double totalFrequency = 0.0;
        for (final long symbolFrequency : symbolFrequencies) {
            totalFrequency += symbolFrequency;
        }

        final PriorityQueue<HuffmanNode> queue = new PriorityQueue<>(symbols.length);

        for (int sIndex = 0; sIndex < symbols.length; sIndex++) {
            final double symbolProbability = (double) symbolFrequencies[sIndex] / totalFrequency;
            symbolProbabilityMap.put(symbols[sIndex], symbolProbability);
            queue.add(new HuffmanNode(symbols[sIndex], symbolProbability));
        }

        return queue;
    }

    /**
     * Create huffman encoder from symbol codes.
     *
     * @return Huffman encoder.
     */
    public HuffmanEncoder createEncoder() {
        assert (root != null && symbolCodes != null) : "Huffman tree was not build yet";
        return new HuffmanEncoder(root, symbolCodes);
    }

    public HuffmanDecoder createDecoder() {
        assert (root != null) : "Huffman tree was not build yet";
        return new HuffmanDecoder(root);
    }

    public HuffmanNode getRoot() {
        return root;
    }
}
