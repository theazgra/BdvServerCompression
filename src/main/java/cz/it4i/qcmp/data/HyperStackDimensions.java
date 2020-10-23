package cz.it4i.qcmp.data;

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

    /**
     * Create HyperStackDimensions.
     *
     * @param width              Width of the plane.
     * @param height             Height of the plane.
     * @param planeCount         Plane count in the stack.
     * @param numberOfTimepoints Number of stack timepoints.
     */
    public HyperStackDimensions(final int width, final int height, final int planeCount, final int numberOfTimepoints) {
        this.width = width;
        this.height = height;
        this.planeCount = planeCount;
        this.numberOfTimepoints = numberOfTimepoints;
    }

    /**
     * Get number of elements in hyperstack with dimensionality = dimension.
     * When calculating the element count, overflow is checked. This is because result of this
     * function is usually used in places where we want to allocate memory.
     *
     * @param dimension Maximum dimension.
     * @return Number of elements.
     */
    @SuppressWarnings("DuplicateExpressions")
    public int getNumberOfElementsInDimension(final int dimension) {
        switch (dimension) {
            case 1:
                return width;
            case 2:
                return Math.multiplyExact(width, height);
            case 3:
                return Math.multiplyExact(planeCount, Math.multiplyExact(width, height));
            case 4:
                return Math.multiplyExact(numberOfTimepoints, Math.multiplyExact(planeCount, Math.multiplyExact(width, height)));
            default:
                assert (false) : "Wrong dimension in getNumberOfElementsInDimension";
                return -1;
        }
    }

    /**
     * Create HyperStackDimensions for single timepoint.
     *
     * @param width      Width of the plane.
     * @param height     Height of the plane.
     * @param planeCount Plane count in the stack.
     */
    public HyperStackDimensions(final int width, final int height, final int planeCount) {
        this(width, height, planeCount, 1);
    }

    /**
     * Create HyperStackDimensions for single plane and single timepoint.
     *
     * @param width  Width of the plane.
     * @param height Height of the plane.
     */
    public HyperStackDimensions(final int width, final int height) {
        this(width, height, 1, 1);
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
