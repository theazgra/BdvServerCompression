package cz.it4i.qcmp.cli.functions;

import cz.it4i.qcmp.cli.CompressionOptionsCLIParser;
import cz.it4i.qcmp.cli.CustomFunctionBase;
import cz.it4i.qcmp.data.HyperStackDimensions;
import cz.it4i.qcmp.io.RawDataIO;
import cz.it4i.qcmp.io.loader.IPlaneLoader;
import cz.it4i.qcmp.io.loader.PlaneLoaderFactory;
import cz.it4i.qcmp.utilities.Utils;

import java.io.IOException;

public class DebugFunction extends CustomFunctionBase {
    /**
     * Base constructor with parsed CLI options.
     *
     * @param options Parsed cli options.
     */
    public DebugFunction(final CompressionOptionsCLIParser options) {
        super(options);
    }

    @Override
    public boolean run() {
        final IPlaneLoader loader;
        try {
            loader = PlaneLoaderFactory.getPlaneLoaderForInputFile(options.getInputDataInfo());
        } catch (final Exception e) {
            e.printStackTrace();
            return false;
        }

        final HyperStackDimensions dims = options.getInputDataInfo().getDimensions();
        final int planeSize = Utils.multiplyExact(dims.getWidth(), dims.getHeight());
        final int sliceSize = Utils.multiplyExact(planeSize, dims.getNumberOfTimepoints());
        try {

            for (int t = 12; t < 15; t++) {

                RawDataIO.write(String.format("slice_t%d.raw", t), loader.loadAllPlanesU16Data(t), false);
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }

        return true;
    }
}
