package cz.it4i.qcmp.io.loader;

/**
 * What to do when blocks or voxels extend outside the source image. These parameters are based on OpenGL texture wrapping options.
 */
public enum DataWrappingStrategy {
    /**
     * Pixels outside the source range are left blank, value 0.
     */
    LeaveBlank,
    /**
     * The edge value is repeated.
     */
    ClampToEdge,
    /**
     * Pixels will be repeated by mirrored strategy.
     */
    MirroredRepeat
}
