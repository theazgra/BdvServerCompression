package compression.quantization;

import java.io.*;

public class QuantizationValueCache {

    private final String cacheFolder;

    public QuantizationValueCache(final String cacheFolder) {
        this.cacheFolder = cacheFolder;
        new File(this.cacheFolder).mkdirs();
    }

    private File getCacheFile(final String trainFile, final int quantizationValueCount) {
        final File cacheFile = new File(cacheFolder, String.format("%s_%d_bits.qvc",
                trainFile, quantizationValueCount));
        return cacheFile;
    }

    public void saveQuantizationValue(final String trainFile, final int[] quantizationValues) {
        final int quantizationValueCount = quantizationValues.length;
        final String cacheFile = getCacheFile(trainFile, quantizationValueCount).getAbsolutePath();


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

    public boolean areQuantizationValueCached(final String trainFile, final int quantizationValueCount) {
        final File cacheFile = getCacheFile(trainFile, quantizationValueCount);
        return cacheFile.exists();
    }

    public int[] readCachedValues(final String trainFile, final int quantizationValueCount) {
        final File cacheFile = getCacheFile(trainFile, quantizationValueCount);
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
