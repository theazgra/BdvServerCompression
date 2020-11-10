package cz.it4i.qcmp;

import cz.it4i.qcmp.data.HyperStackDimensions;

public class ScifioWrapper {

    private static final ScifioWrapper instance = null;
    //    private SCIFIO scifioInstance = null;

    private ScifioWrapper() {
        /*scifioInstance = new SCIFIO();*/
    }

    //    public static SCIFIO getScifio() {
    //        if (instance == null) {
    //            synchronized (ScifioWrapper.class) {
    //                if (instance == null) {
    //                    instance = new ScifioWrapper();
    //                }
    //            }
    //        }
    //
    //        return instance.scifioInstance;
    //    }

    public static HyperStackDimensions inspectFileWithScifio(final String filePath) throws Exception {
        final int x;
        final int y;
        final int z;
        final int t;
        final int c;
        x = y = z = t = c = 1;
        //        final Reader reader = getReader(filePath);
        //
        //        final int imageCount = reader.getImageCount();
        //        if (imageCount != 1) {
        //            assert (false) : "TODO";
        //            return null;
        //        }
        //
        //        z = (int) reader.getPlaneCount(0);
        //
        //        final Plane plane = reader.openPlane(0, 0);
        //        x = (int) plane.getLengths()[0];
        //        y = (int) plane.getLengths()[1];


        return new HyperStackDimensions(x, y, z, t, c);
    }

    //    /**
    //     * Get image file reader.
    //     *
    //     * @param path Path of image file.
    //     * @return Scifio reader.
    //     * @throws IOException
    //     * @throws FormatException
    //     */
    //    public static Reader getReader(final String path) throws IOException, FormatException {
    //        final SCIFIO scifio = getScifio();
    //        //        return scifio.initializer().initializeReader(new FileLocation(path));
    //        return scifio.initializer().initializeReader(path);
    //    }

    public synchronized static void dispose() {
        //        if (instance != null) {
        //            if (instance.scifioInstance != null) {
        //                instance.scifioInstance.context().dispose();
        //            }
        //        }
    }
}
