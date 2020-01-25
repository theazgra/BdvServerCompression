package azgracompress.quantization;

import azgracompress.data.V2i;
import azgracompress.quantization.vector.CodebookEntry;

import java.io.*;

public class QuantizationValueCache {

    private final String cacheFolder;

    public QuantizationValueCache(final String cacheFolder) {
        this.cacheFolder = cacheFolder;
        new File(this.cacheFolder).mkdirs();
    }

    private File getCacheFileForScalarValues(final String trainFile, final int quantizationValueCount) {
        final File inputFile = new File(trainFile);
        final File cacheFile = new File(cacheFolder, String.format("%s_%d_bits.qvc",
                                                                   inputFile.getName(), quantizationValueCount));
        return cacheFile;
    }

    private File getCacheFileForVectorValues(final String trainFile,
                                             final int codebookSize,
                                             final int entryWidth,
                                             final int entryHeight) {
        final File inputFile = new File(trainFile);
        final File cacheFile = new File(cacheFolder, String.format("%s_%d_%dx%d.qvc",
                                                                   inputFile.getName(),
                                                                   codebookSize,
                                                                   entryWidth,
                                                                   entryHeight));
        return cacheFile;
    }

    public void saveQuantizationValues(final String trainFile, final int[] quantizationValues) throws IOException {
        final int quantizationValueCount = quantizationValues.length;
        final String cacheFile = getCacheFileForScalarValues(trainFile, quantizationValueCount).getAbsolutePath();

        try (FileOutputStream fos = new FileOutputStream(cacheFile, false);
             DataOutputStream dos = new DataOutputStream(fos)) {

            for (final int qv : quantizationValues) {
                dos.writeInt(qv);
            }
        } catch (IOException ex) {
            throw new IOException(String.format("Failed to write cache to file: %s.\nInner Ex:\n%s",
                                                cacheFile,
                                                ex.getMessage()));
        }
    }

    public void saveQuantizationValues(final String trainFile,
                                       final CodebookEntry[] entries,
                                       final V2i qVecDims) throws IOException {
        final int codebookSize = entries.length;
        final int entryWidth = qVecDims.getX();
        final int entryHeight = qVecDims.getY();
        final String cacheFile = getCacheFileForVectorValues(trainFile,
                                                             codebookSize,
                                                             entryWidth,
                                                             entryHeight).getAbsolutePath();


        try (FileOutputStream fos = new FileOutputStream(cacheFile, false);
             DataOutputStream dos = new DataOutputStream(fos)) {

            dos.writeInt(codebookSize);
            dos.writeInt(entryWidth);
            dos.writeInt(entryHeight);

            for (final CodebookEntry entry : entries) {
                for (final int vectorValue : entry.getVector()) {
                    dos.writeInt(vectorValue);
                }
            }
        }
    }

    public int[] readCachedValues(final String trainFile, final int quantizationValueCount) throws IOException {
        final File cacheFile = getCacheFileForScalarValues(trainFile, quantizationValueCount);

        int[] values = new int[quantizationValueCount];
        try (FileInputStream fis = new FileInputStream(cacheFile);
             DataInputStream dis = new DataInputStream(fis)) {

            for (int i = 0; i < quantizationValueCount; i++) {
                values[i] = dis.readInt();
            }
        }
        return values;
    }

    public CodebookEntry[] readCachedValues(final String trainFile,
                                            final int codebookSize,
                                            final int entryWidth,
                                            final int entryHeight) throws IOException {
        final File cacheFile = getCacheFileForVectorValues(trainFile, codebookSize, entryWidth, entryHeight);

        CodebookEntry[] codebook = new CodebookEntry[codebookSize];
        try (FileInputStream fis = new FileInputStream(cacheFile);
             DataInputStream dis = new DataInputStream(fis)) {

            final int savedCodebookSize = dis.readInt();
            final int savedEntryWidth = dis.readInt();
            final int savedEntryHeight = dis.readInt();
            assert (savedCodebookSize == codebookSize) : "Wrong codebook size";
            assert (savedEntryWidth == entryWidth) : "Wrong entry width";
            assert (savedEntryHeight == entryHeight) : "Wrong entry height";

            final int entrySize = entryWidth * entryHeight;
            for (int i = 0; i < codebookSize; i++) {
                int[] vector = new int[entrySize];
                for (int j = 0; j < entrySize; j++) {
                    vector[j] = dis.readInt();
                }
                codebook[i] = new CodebookEntry(vector);
            }
        }
        return codebook;

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


}
