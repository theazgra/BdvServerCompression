package cz.it4i.qcmp.huffman;

import java.util.*;

public class HuffmanTreeBuilder {
    private HuffmanNode root = null;
    private HashMap<Integer, boolean[]> symbolCodes;
    private HashMap<Integer, Double> symbolProbabilityMap;
    private final int[] symbols;
    private final long[] symbolFrequencies;

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
                System.err.println(String.format("Parent A prob: %.6f\nParent B prob: %.6f",
                                                 parentA.getProbability(),
                                                 parentB.getProbability()));
                assert (parentA.getProbability() <= parentB.getProbability());
            }

            parentA.setBit(1);
            parentB.setBit(0);

            final double mergedProbabilities = parentA.getProbability() + parentB.getProbability();
            final HuffmanNode mergedNode = HuffmanNode.constructWithProbability(parentA, parentB, mergedProbabilities);
            queue.add(mergedNode);
        }
        root = queue.poll();
        buildHuffmanCodes();
    }

    private void buildHuffmanCodes() {
        symbolCodes = new HashMap<>(symbols.length);

        traverseSymbolCodes(root, new ArrayList<Boolean>());
    }

    private void traverseSymbolCodes(final HuffmanNode currentNode, final ArrayList<Boolean> currentCode) {
        boolean inLeaf = true;
        final int bit = currentNode.getBit();
        if (bit != -1) {
            currentCode.add(bit == 1);
        }

        if (currentNode.rightChild != null) {
            final ArrayList<Boolean> codeCopy = new ArrayList<Boolean>(currentCode);
            traverseSymbolCodes(currentNode.rightChild, codeCopy);
            inLeaf = false;
        }
        if (currentNode.leftChild != null) {
            final ArrayList<Boolean> codeCopy = new ArrayList<Boolean>(currentCode);
            traverseSymbolCodes(currentNode.leftChild, codeCopy);
            inLeaf = false;
        }

        if (inLeaf) {
            assert (currentNode.isLeaf());
            //currentNode.setIsLeaf(true);

            final boolean[] finalSymbolCode = new boolean[currentCode.size()];
            for (int i = 0; i < finalSymbolCode.length; i++) {
                finalSymbolCode[i] = currentCode.get(i);
            }
            symbolCodes.put(currentNode.getSymbol(), finalSymbolCode);
        }

    }

    private PriorityQueue<HuffmanNode> buildPriorityQueue() {
        symbolProbabilityMap = new HashMap<>(symbols.length);
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


    public boolean[] getCode(final int symbol) {
        return symbolCodes.get(symbol);
    }

    public HuffmanNode getRoot() {
        return root;
    }

    public HashMap<Integer, Double> getSymbolProbabilityMap() {
        return createSortedHashMap(symbolProbabilityMap);
    }

    private HashMap<Integer, Double> createSortedHashMap(final HashMap<Integer, Double> map) {
        final List<Map.Entry<Integer, Double>> list = new LinkedList<Map.Entry<Integer, Double>>(map.entrySet());
        //Custom Comparator
        list.sort((t0, t1) -> (-(t0.getValue().compareTo(t1.getValue()))));
        //copying the sorted list in HashMap to preserve the iteration order
        final HashMap<Integer, Double> sortedHashMap = new LinkedHashMap<Integer, Double>();
        for (final Map.Entry<Integer, Double> entry : list) {
            sortedHashMap.put(entry.getKey(), entry.getValue());
        }
        return sortedHashMap;
    }
}
