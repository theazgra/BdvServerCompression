package cz.it4i.qcmp.data;

/**
 * Class representing dimensions of the Stack or Hyperstack.
 * This terminology is taken from the ImageJ.
 */
public class HyperStackDimensions {
    private final int width;
    private final int height;
    private final int sliceCount;
    private final int numberOfTimepoints;


    /**
     * Create HyperStackDimensions.
     *
     * @param width              Width of the slice.
     * @param height             Height of the slice.
     * @param sliceCount         Slice count in the stack.
     * @param numberOfTimepoints Number of stack timepoints.
     */
    public HyperStackDimensions(final int width, final int height, final int sliceCount, final int numberOfTimepoints) {
        this.width = width;
        this.height = height;
        this.sliceCount = sliceCount;
        this.numberOfTimepoints = numberOfTimepoints;
    }

    /**
     * Create HyperStackDimensions for single timepoint.
     *
     * @param width      Width of the slice.
     * @param height     Height of the slice.
     * @param sliceCount Slice count in the stack.
     */
    public HyperStackDimensions(final int width, final int height, final int sliceCount) {
        this(width, height, sliceCount, 1);
    }

    /**
     * Create HyperStackDimensions for single slice and single timepoint.
     *
     * @param width  Width of the slice.
     * @param height Height of the slice.
     */
    public HyperStackDimensions(final int width, final int height) {
        this(width, height, 1, 1);
    }

    /**
     * Get single slice width. (X)
     *
     * @return Slice width.
     */
    public final int getWidth() {
        return width;
    }

    /**
     * Get single slice height. (Y)
     *
     * @return Slice height.
     */
    public final int getHeight() {
        return height;
    }

    /**
     * Get slice count. (Z, Plane Count)
     *
     * @return Slice count.
     */
    public final int getSliceCount() {
        return sliceCount;
    }

    /**
     * Get number of timepoints of the stack.
     *
     * @return Timepoint count.
     */
    public final int getNumberOfTimepoints() {
        return numberOfTimepoints;
    }
}
