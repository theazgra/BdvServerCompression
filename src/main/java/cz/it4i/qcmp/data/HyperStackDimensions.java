package cz.it4i.qcmp.data;

import cz.it4i.qcmp.utilities.Utils;

import java.util.Objects;

/**
 * Class representing dimensions of the Stack or Hyperstack.
 * This terminology is taken from the ImageJ.
 */
public class HyperStackDimensions {
    private final int width;
    private final int height;
    private final int planeCount;
    private final int numberOfTimepoints;
    private final int numberOfChannels;

    /**
     * Create HyperStackDimensions.
     *
     * @param width              Width of the plane.
     * @param height             Height of the plane.
     * @param planeCount         Plane count in the stack.
     * @param numberOfTimepoints Number of stack timepoints.
     * @param channelCount       Number of image channels.
     */
    public HyperStackDimensions(final int width,
                                final int height,
                                final int planeCount,
                                final int numberOfTimepoints,
                                final int channelCount) {
        assert (channelCount == 1) : "QcmpCompressionLibrary support only single channel datasets.";
        this.width = width;
        this.height = height;
        this.planeCount = planeCount;
        this.numberOfTimepoints = numberOfTimepoints;
        this.numberOfChannels = channelCount;
    }

    /**
     * Create HyperStackDimensions.
     *
     * @param width              Width of the plane.
     * @param height             Height of the plane.
     * @param planeCount         Plane count in the stack.
     * @param numberOfTimepoints Number of stack timepoints.
     */
    public HyperStackDimensions(final int width,
                                final int height,
                                final int planeCount,
                                final int numberOfTimepoints) {
        this(width, height, planeCount, numberOfTimepoints, 1);
    }

    /**
     * Create HyperStackDimensions for single timepoint.
     *
     * @param width      Width of the plane.
     * @param height     Height of the plane.
     * @param planeCount Plane count in the stack.
     */
    public HyperStackDimensions(final int width, final int height, final int planeCount) {
        this(width, height, planeCount, 1, 1);
    }

    /**
     * Create HyperStackDimensions for single plane and single timepoint.
     *
     * @param width  Width of the plane.
     * @param height Height of the plane.
     */
    public HyperStackDimensions(final int width, final int height) {
        this(width, height, 1, 1, 1);
    }

    /**
     * Create HyperStackDimensions from ij.ImagePlus dimensions array.
     *
     * @param imagePlusDimensions ImagePlus dimensions.
     * @return Correct HyperStackDimensions.
     */
    public static HyperStackDimensions createFromImagePlusDimensions(final int[] imagePlusDimensions) {
        // NOTE(Moravec):  ij.ImagePlus dimensions array = (width, height, nChannels, nSlices, nFrames)
        return new HyperStackDimensions(imagePlusDimensions[0],
                                        imagePlusDimensions[1],
                                        imagePlusDimensions[3],
                                        imagePlusDimensions[4],
                                        imagePlusDimensions[2]);
    }

    /**
     * Get number of elements in hyperstack with dimensionality = dimension.
     * When calculating the element count, overflow is checked. This is because result of this
     * function is usually used in places where we want to allocate memory.
     *
     * @param dimension Maximum dimension.
     * @return Number of elements.
     */
    public int getNumberOfElementsInDimension(final int dimension) {
        switch (dimension) {
            case 1:
                return width;
            case 2:
                return Math.multiplyExact(width, height);
            case 3:
                return Utils.multiplyExact(width, height, planeCount);
            case 4:
                return Utils.multiplyExact(width, height, planeCount, numberOfChannels);
            default:
                assert (false) : "Wrong dimension in getNumberOfElementsInDimension";
                return -1;
        }
    }

    /**
     * Get data size of U16 dataset with this dimensions.
     *
     * @return Data size.
     */
    public long getDataSize() {
        return (long) 2 * width * height * planeCount * numberOfTimepoints * numberOfChannels;
    }

    /**
     * Get single plane width. (X)
     *
     * @return Plane width.
     */
    public final int getWidth() {
        return width;
    }

    /**
     * Get single plane height. (Y)
     *
     * @return Plane height.
     */
    public final int getHeight() {
        return height;
    }

    /**
     * Get plane count. (Z)
     *
     * @return Plane count.
     */
    public final int getPlaneCount() {
        return planeCount;
    }

    /**
     * Get dimensions of the single plane.
     *
     * @return Plane dimensions.
     */
    public V2i getPlaneDimensions() {
        return new V2i(width, height);
    }

    /**
     * Get number of channels in the dataset.
     *
     * @return Channel count.
     */
    public int getNumberOfChannels() {
        return numberOfChannels;
    }

    /**
     * Get number of timepoints of the stack.
     *
     * @return Timepoint count.
     */
    public final int getNumberOfTimepoints() {
        return numberOfTimepoints;
    }

    @Override
    public String toString() {
        return String.format("X=%d;Y=%d;Z=%d;T=%d", width, height, planeCount, numberOfTimepoints);
    }

    @Override
    public int hashCode() {
        return Objects.hash(width, height, planeCount, numberOfTimepoints);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof HyperStackDimensions) {
            final HyperStackDimensions other = (HyperStackDimensions) obj;
            return (width == other.width && height == other.height &&
                    planeCount == other.planeCount && numberOfTimepoints == other.numberOfTimepoints);
        }
        return super.equals(obj);
    }
}
