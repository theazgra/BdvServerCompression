package azgracompress;

import io.scif.SCIFIO;
import io.scif.formats.TIFFFormat;

public class ScifioWrapper {

    private static ScifioWrapper instance = null;
    private SCIFIO scifioInstance = null;

    private ScifioWrapper() {
        scifioInstance = new SCIFIO();
    }

    public static SCIFIO getScifio() {
        if (instance == null) {
            synchronized (ScifioWrapper.class) {
                if (instance == null) {
                    instance = new ScifioWrapper();
                }
            }
        }

        return instance.scifioInstance;
    }

    public synchronized static void dispose() {
        if (instance != null) {
            if (instance.scifioInstance != null) {
                instance.scifioInstance.context().dispose();
            }
        }
    }
}
