package azgracompress.io.loader;

import azgracompress.io.InputDataInfo;

public final class PlaneLoaderFactory {

    /**
     * Create concrete plane loader for the input file.
     *
     * @param inputDataInfo Input file information.
     * @return Concrete plane loader.
     * @throws Exception When fails to create plane loader.
     */
    public static IPlaneLoader getPlaneLoaderForInputFile(final InputDataInfo inputDataInfo) throws Exception {
        switch (inputDataInfo.getDataLoaderType()) {
            case RawDataLoader:
                return new RawDataLoader(inputDataInfo);
            case SCIFIOLoader:
                return new SCIFIOLoader(inputDataInfo);
            case ImageJBufferLoader:
                break;
            default:
                throw new Exception("Unsupported data loader.");
        }
        return null;
    }
}
