package azgracompress.io;

import azgracompress.cli.InputFileInfo;
import azgracompress.data.ImageU16;

import java.io.IOException;

public class ConcretePlaneLoader implements IPlaneLoader {

    private final IPlaneLoader loader;

    private final InputFileInfo inputFileInfo;

    /**
     * Create plane loader.
     *
     * @param inputFileInfo Information about input file.
     * @throws Exception When fails to create SCIFIO reader.
     */
    public ConcretePlaneLoader(final InputFileInfo inputFileInfo) throws Exception {
        this.inputFileInfo = inputFileInfo;

        if (inputFileInfo.isRAW()) {
            loader = new RawDataLoader(inputFileInfo);
        } else {
            loader = new SCIFIOLoader(inputFileInfo);
        }
    }


    @Override
    public ImageU16 loadPlaneU16(int plane) throws IOException {
        return loader.loadPlaneU16(plane);
    }

    @Override
    public int[] loadPlanesU16Data(int[] planes) throws IOException {
        return loader.loadPlanesU16Data(planes);
    }

    @Override
    public int[] loadAllPlanesU16Data() throws IOException {
        return loader.loadAllPlanesU16Data();
    }
}
