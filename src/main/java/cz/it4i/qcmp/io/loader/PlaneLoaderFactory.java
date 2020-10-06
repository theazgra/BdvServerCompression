package cz.it4i.qcmp.io.loader;

import cz.it4i.qcmp.io.BufferInputData;
import cz.it4i.qcmp.io.FileInputData;
import cz.it4i.qcmp.io.FlatBufferInputData;
import cz.it4i.qcmp.io.InputData;

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
                return new SCIFIOLoader((FileInputData) inputDataInfo);
            case ImageJBufferLoader:
                return new ImageJBufferLoader((BufferInputData) inputDataInfo);
            case FlatBufferLoader:
                return new FlatBufferLoader((FlatBufferInputData) inputDataInfo);
            default:
                throw new Exception("Unsupported data loader.");
        }
    }
}
