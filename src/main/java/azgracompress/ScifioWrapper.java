package azgracompress;

import io.scif.FormatException;
import io.scif.Reader;
import io.scif.SCIFIO;

import java.io.IOException;

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

    /**
     * Get image file reader.
     *
     * @param path Path of image file.
     * @return Scifio reader.
     * @throws IOException
     * @throws FormatException
     */
    public static Reader getReader(final String path) throws IOException, FormatException {
        SCIFIO scifio = getScifio();
        return scifio.initializer().initializeReader(path);
    }

    public synchronized static void dispose() {
        if (instance != null) {
            if (instance.scifioInstance != null) {
                instance.scifioInstance.context().dispose();
            }
        }
    }
}
