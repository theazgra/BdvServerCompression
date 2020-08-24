package azgracompress.cli.functions;

import azgracompress.cli.CustomFunctionBase;
import azgracompress.cli.CompressionOptionsCLIParser;
import azgracompress.io.loader.IPlaneLoader;
import azgracompress.io.loader.PlaneLoaderFactory;
import azgracompress.utilities.Utils;

import java.io.IOException;

public class EntropyCalculation extends CustomFunctionBase {
    /**
     * Base constructor with parsed CLI options.
     *
     * @param options Parsed cli options.
     */
    public EntropyCalculation(CompressionOptionsCLIParser options) {
        super(options);
    }

    @Override
    public boolean run() {
        IPlaneLoader loader;
        try {
            loader = PlaneLoaderFactory.getPlaneLoaderForInputFile(options.getInputDataInfo());
        } catch (Exception ex) {
            System.err.println("EntropyCalculation::run() - Unable to get plane loader. " + ex.getMessage());
            return false;
        }

        for (int planeIndex = 0; planeIndex < options.getInputDataInfo().getDimensions().getZ(); planeIndex++) {
            final int[] planeData;
            try {
                planeData = loader.loadPlaneData(planeIndex);
            } catch (IOException e) {
                System.err.printf("Failed to load plane %d data. %s", planeIndex, e.getMessage());
                return false;
            }

            final double planeEntropy = Utils.calculateEntropy(planeData);
            System.out.printf("P: %d\tE: %.4f\n", planeIndex, planeEntropy);
        }

        return true;
    }
}
