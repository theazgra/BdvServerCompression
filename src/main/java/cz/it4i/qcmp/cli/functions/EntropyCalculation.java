package cz.it4i.qcmp.cli.functions;

import cz.it4i.qcmp.cli.CompressionOptionsCLIParser;
import cz.it4i.qcmp.cli.CustomFunctionBase;
import cz.it4i.qcmp.io.loader.IPlaneLoader;
import cz.it4i.qcmp.io.loader.PlaneLoaderFactory;
import cz.it4i.qcmp.utilities.Utils;

import java.io.IOException;

public class EntropyCalculation extends CustomFunctionBase {
    /**
     * Base constructor with parsed CLI options.
     *
     * @param options Parsed cli options.
     */
    public EntropyCalculation(final CompressionOptionsCLIParser options) {
        super(options);
    }

    @Override
    public boolean run() {
        final IPlaneLoader loader;
        try {
            loader = PlaneLoaderFactory.getPlaneLoaderForInputFile(options.getInputDataInfo());
        } catch (final Exception ex) {
            System.err.println("EntropyCalculation::run() - Unable to get plane loader. " + ex.getMessage());
            return false;
        }

        for (int planeIndex = 0; planeIndex < options.getInputDataInfo().getDimensions().getZ(); planeIndex++) {
            final int[] planeData;
            try {
                planeData = loader.loadPlaneData(planeIndex);
            } catch (final IOException e) {
                System.err.printf("Failed to load plane %d data. %s", planeIndex, e.getMessage());
                return false;
            }

            final double planeEntropy = Utils.calculateEntropy(planeData);
            System.out.printf("P: %d\tE: %.4f\n", planeIndex, planeEntropy);
        }

        return true;
    }
}
