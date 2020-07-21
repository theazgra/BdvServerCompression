package azgracompress.io.loader;

import azgracompress.io.BufferInputData;
import azgracompress.io.FileInputData;
import azgracompress.io.InputData;

import java.nio.Buffer;

public final class PlaneLoaderFactory {

    /**
     * Create concrete plane loader for the input file.
     *
     * @param inputDataInfo Input file information.
     * @return Concrete plane loader.
     * @throws Exception When fails to create plane loader.
     */
    public static IPlaneLoader getPlaneLoaderForInputFile(final InputData inputDataInfo) throws Exception {
        switch (inputDataInfo.getDataLoaderType()) {
            case RawDataLoader:
                return new RawDataLoader((FileInputData) inputDataInfo);
            case SCIFIOLoader:
                return new SCIFIOLoader((FileInputData)inputDataInfo);
            case ImageJBufferLoader:
                return new ImageJBufferLoader((BufferInputData) inputDataInfo);
            default:
                throw new Exception("Unsupported data loader.");
        }
//        return null;
    }
}