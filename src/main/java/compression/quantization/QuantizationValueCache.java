package compression.quantization;

import compression.quantization.vector.CodebookEntry;

import java.io.*;

public class QuantizationValueCache {

    private final String cacheFolder;

    public QuantizationValueCache(final String cacheFolder) {
        this.cacheFolder = cacheFolder;
        new File(this.cacheFolder).mkdirs();
    }

    private File getCacheFileForScalarValues(final String trainFile, final int quantizationValueCount) {
        final File cacheFile = new File(cacheFolder, String.format("%s_%d_bits.qvc",
                trainFile, quantizationValueCount));
        return cacheFile;
    }

    private File getCacheFileForVectorValues(final String trainFile,
                                             final int codebookSize,
                                             final int entryWidth,
                                             final int entryHeight) {
        final File cacheFile = new File(cacheFolder, String.format("%s_%d_%dx%d.qvc",
                trainFile, codebookSize, entryWidth, entryHeight));
        return cacheFile;
    }

    public void saveQuantizationValue(final String trainFile, final int[] quantizationValues) {
        final int quantizationValueCount = quantizationValues.length;
        final String cacheFile = getCacheFileForScalarValues(trainFile, quantizationValueCount).getAbsolutePath();


        try {
            // NOTE(Moravec): RandomAccessFile is slow but provides binary APIs.
            RandomAccessFile rac = new RandomAccessFile(cacheFile, "rw");
            for (final int qv : quantizationValues) {
                rac.writeInt(qv);
            }
            rac.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveQuantizationValues(final String trainFile, final CodebookEntry[] entries) {
        final int codebookSize = entries.length;
        final int entryWidth = entries[0].getWidth();
        final int entryHeight = entries[0].getHeight();
        final String cacheFile = getCacheFileForVectorValues(trainFile, codebookSize, entryWidth, entryHeight).getAbsolutePath();


        try {
            // NOTE(Moravec): RandomAccessFile is slow but provides binary APIs.
            RandomAccessFile rac = new RandomAccessFile(cacheFile, "rw");
            rac.writeInt(codebookSize);
            rac.writeInt(entryWidth);
            rac.writeInt(entryHeight);

            for (final CodebookEntry entry : entries) {
                for (final int vectorValue : entry.getVector()) {
                    rac.writeInt(vectorValue);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public CodebookEntry[] readCachedValues(final String trainFile,
                                            final int codebookSize,
                                            final int entryWidth,
                                            final int entryHeight) {
        final File cacheFile = getCacheFileForVectorValues(trainFile, codebookSize, entryWidth, entryHeight);
        try {
            // NOTE(Moravec): RandomAccessFile is slow but provides binary APIs.
            RandomAccessFile rac = new RandomAccessFile(cacheFile, "r");

            final int savedCodebookSize = rac.readInt();
            final int savedEntryWidth = rac.readInt();
            final int savedEntryHeight = rac.readInt();
            assert (savedCodebookSize == codebookSize) : "Wrong codebook size";
            assert (savedEntryWidth == entryWidth) : "Wrong entry width";
            assert (savedEntryHeight == entryHeight) : "Wrong entry height";

            CodebookEntry[] codebook = new CodebookEntry[codebookSize];
            final int entrySize = entryWidth * entryHeight;
            for (int i = 0; i < codebookSize; i++) {
                int[] vector = new int[entrySize];
                for (int j = 0; j < entrySize; j++) {
                    vector[j] = rac.readInt();
                }
                codebook[i] = new CodebookEntry(vector);
            }

            return codebook;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new CodebookEntry[0];
    }

    public boolean areQuantizationValueCached(final String trainFile, final int quantizationValueCount) {
        final File cacheFile = getCacheFileForScalarValues(trainFile, quantizationValueCount);
        return cacheFile.exists();
    }

    public boolean areVectorQuantizationValueCached(final String trainFile,
                                                    final int codebookSize,
                                                    final int entryWidth,
                                                    final int entryHeight) {
        final File cacheFile = getCacheFileForVectorValues(trainFile, codebookSize, entryWidth, entryHeight);
        return cacheFile.exists();
    }

    public int[] readCachedValues(final String trainFile, final int quantizationValueCount) {
        final File cacheFile = getCacheFileForScalarValues(trainFile, quantizationValueCount);
        try {
            // NOTE(Moravec): RandomAccessFile is slow but provides binary APIs.
            RandomAccessFile rac = new RandomAccessFile(cacheFile, "r");
            int[] values = new int[quantizationValueCount];
            for (int i = 0; i < quantizationValueCount; i++) {
                values[i] = rac.readInt();
            }
            return values;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new int[0];
    }
}
