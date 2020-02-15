package azgracompress.io;

import azgracompress.cli.InputFileInfo;

public final class PlaneLoaderFactory {

    /**
     * Create concrete plane loader for the input file.
     *
     * @param inputFileInfo Input file information.
     * @return Concrete plane loader.
     * @throws Exception When fails to create plane loader.
     */
    public static IPlaneLoader getPlaneLoaderForInputFile(final InputFileInfo inputFileInfo) throws Exception {
        if (inputFileInfo.isRAW()) {
            return new RawDataLoader(inputFileInfo);
        } else {
            return new SCIFIOLoader(inputFileInfo);
        }
    }
}
