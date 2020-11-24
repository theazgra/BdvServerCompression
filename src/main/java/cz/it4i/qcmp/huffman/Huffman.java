package cz.it4i.qcmp.huffman;

import cz.it4i.qcmp.io.InBitStream;
import cz.it4i.qcmp.io.OutBitStream;

import java.io.IOException;
import java.util.*;

public class Huffman {
    private HuffmanNode root = null;
    private HashMap<Integer, boolean[]> symbolCodes;
    private HashMap<Integer, Double> symbolProbabilityMap;
    private final int[] symbols;
    private final long[] symbolFrequencies;

    public Huffman(final int[] symbols, final long[] symbolFrequencies) {
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

        if (currentNode.subNodeA != null) {
            final ArrayList<Boolean> codeCopy = new ArrayList<Boolean>(currentCode);
            traverseSymbolCodes(currentNode.subNodeA, codeCopy);
            inLeaf = false;
        }
        if (currentNode.subNodeB != null) {
            final ArrayList<Boolean> codeCopy = new ArrayList<Boolean>(currentCode);
            traverseSymbolCodes(currentNode.subNodeB, codeCopy);
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
            queue.add(new HuffmanNode(symbols[sIndex], symbolProbability, symbolFrequencies[sIndex]));
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
        list.sort(new Comparator<Map.Entry<Integer, Double>>() {
            @Override
            public int compare(final Map.Entry<Integer, Double> t0, final Map.Entry<Integer, Double> t1) {
                return -(t0.getValue().compareTo(t1.getValue()));
            }
        });
        //copying the sorted list in HashMap to preserve the iteration order
        final HashMap<Integer, Double> sortedHashMap = new LinkedHashMap<Integer, Double>();
        for (final Map.Entry<Integer, Double> entry : list) {
            sortedHashMap.put(entry.getKey(), entry.getValue());
        }
        return sortedHashMap;
    }

    private void encodeHuffmanNode(final HuffmanNode node, final OutBitStream bitStream) throws IOException {
        if (node.isLeaf()) {
            bitStream.writeBit(1);
            bitStream.write(node.getSymbol());
        } else {
            bitStream.writeBit(0);
            encodeHuffmanNode(node.getSubNodeA(), bitStream);
            encodeHuffmanNode(node.getSubNodeB(), bitStream);
        }
    }

    private static HuffmanNode decodeHuffmanNode(final InBitStream bitStream) throws IOException {
        if (bitStream.readBit()) // Leaf
        {
            return HuffmanNode.constructWithSymbol(null, null, bitStream.readValue());
        } else {
            final HuffmanNode nodeA = decodeHuffmanNode(bitStream);
            nodeA.setBit(1);
            final HuffmanNode nodeB = decodeHuffmanNode(bitStream);
            nodeB.setBit(0);
            return HuffmanNode.constructWithSymbol(nodeA, nodeB, -1);
        }
    }


    public void saveHuffmanTree(final OutBitStream bitStream) throws IOException {
        assert root != null : "The tree is not build.";
        encodeHuffmanNode(root, bitStream);
    }

    public static HuffmanNode readHuffmanTree(final InBitStream bitStream) throws IOException {
        return decodeHuffmanNode(bitStream);
    }
}
