package cz.it4i.qcmp.cli.functions;

import cz.it4i.qcmp.cache.QuantizationCacheManager;
import cz.it4i.qcmp.cache.VQCacheFile;
import cz.it4i.qcmp.cli.CompressionOptionsCLIParser;
import cz.it4i.qcmp.cli.CustomFunctionBase;
import cz.it4i.qcmp.huffman.HuffmanNode;
import cz.it4i.qcmp.huffman.HuffmanTreeBuilder;
import cz.it4i.qcmp.io.InBitStream;
import cz.it4i.qcmp.io.OutBitStream;
import cz.it4i.qcmp.quantization.vector.VQCodebook;
import cz.it4i.qcmp.utilities.Utils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

@SuppressWarnings("ConstantConditions")
public class DebugFunction extends CustomFunctionBase {
    /**
     * Base constructor with parsed CLI options.
     *
     * @param options Parsed cli options.
     */
    public DebugFunction(final CompressionOptionsCLIParser options) {
        super(options);
    }

    @Override
    public boolean run() {

        final VQCodebook codebook = ((VQCacheFile) QuantizationCacheManager.readCacheFile("D:\\tmp\\codebook.qvc")).getCodebook();

        final int[] symbols = new int[codebook.getCodebookSize()];
        for (int i = 0; i < codebook.getCodebookSize(); i++) {
            symbols[i] = i;
        }

        final HuffmanTreeBuilder huffmanBuilder = new HuffmanTreeBuilder(symbols, codebook.getVectorFrequencies());
        huffmanBuilder.buildHuffmanTree();

        final int bitsPerSymbol = (int) Utils.log2(codebook.getCodebookSize());
        try (final OutBitStream bitStream = new OutBitStream(new FileOutputStream("D:\\tmp\\huffman_tree.data", false),
                                                             bitsPerSymbol,
                                                             64)) {
            huffmanBuilder.createEncoder().getRoot().writeToBinaryStream(bitStream);
        } catch (final IOException e) {
            e.printStackTrace();
        }


        HuffmanNode readRoot = null;
        try (final InBitStream inBitStream = new InBitStream(new FileInputStream("D:\\tmp\\huffman_tree.data"), bitsPerSymbol, 256)) {
            readRoot = HuffmanNode.readFromStream(inBitStream);
        } catch (final IOException ex) {
            ex.printStackTrace();
        }

        final boolean equal = huffmanBuilder.createEncoder().getRoot().treeEqual(readRoot);

        System.out.println(readRoot != null);

        return true;
    }
}
