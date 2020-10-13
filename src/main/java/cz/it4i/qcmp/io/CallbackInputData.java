package cz.it4i.qcmp.io;

import cz.it4i.qcmp.data.V3i;

public class CallbackInputData extends InputData {

    private final String cacheHint;
    private final LoadCallback pixelLoadCallback;

    /**
     * Interface defining method for pixel data loading.
     */
    public interface LoadCallback {
        /**
         * Callback to load pixel value at specified position.
         *
         * @param x X position.
         * @param y Y position.
         * @param z Z position.
         * @return Pixel value at specified position.
         */
        int getValueAt(final int x, final int y, final int z);
    }

    public CallbackInputData(final LoadCallback pixelLoadCallback,
                             final V3i dimensions,
                             final String cacheHint) {
        this.pixelLoadCallback = pixelLoadCallback;
        this.cacheHint = cacheHint;
        setDataLoaderType(DataLoaderType.CallbackLoader);
        setPixelType(PixelType.Gray16);
        setDimension(dimensions);
    }

    @Override
    public String getCacheFileName() {
        return cacheHint;
    }

    public final LoadCallback getPixelLoadCallback() {
        return pixelLoadCallback;
    }
}
