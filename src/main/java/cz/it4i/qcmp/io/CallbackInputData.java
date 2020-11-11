package cz.it4i.qcmp.io;

import cz.it4i.qcmp.data.HyperStackDimensions;

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
         * @param z Plane index.
         * @param t Timepoint index.
         * @return Pixel value at specified position.
         */
        int getValueAt(final int x, final int y, final int z, final int t);
    }

    /**
     * Create very generic loader, which load data by executing callbacks with parameters.
     *
     * @param pixelLoadCallback Object with callbacks.
     * @param datasetDimensions Dataset dimensions.
     * @param cacheHint         Qcmp cache file name.
     */
    public CallbackInputData(final LoadCallback pixelLoadCallback,
                             final HyperStackDimensions datasetDimensions,
                             final String cacheHint) {
        super(datasetDimensions);
        this.pixelLoadCallback = pixelLoadCallback;
        this.cacheHint = cacheHint;
        setDataLoaderType(DataLoaderType.CallbackLoader);
        setPixelType(PixelType.Gray16);
    }

    @Override
    public String getCacheFileName() {
        return cacheHint;
    }

    /**
     * Get the pixel value callback object.
     *
     * @return Callback object.
     */
    public final LoadCallback getPixelLoadCallback() {
        return pixelLoadCallback;
    }
}
