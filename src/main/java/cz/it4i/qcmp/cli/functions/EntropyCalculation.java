package cz.it4i.qcmp.cli.functions;

import cz.it4i.qcmp.cli.CompressionOptionsCLIParser;
import cz.it4i.qcmp.cli.CustomFunctionBase;
import cz.it4i.qcmp.io.loader.IPlaneLoader;
import cz.it4i.qcmp.io.loader.PlaneLoaderFactory;
import cz.it4i.qcmp.utilities.Utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

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
        System.out.println("Running EntropyCalculation for " + options.getInputDataInfo().getCacheFileName());
        final IPlaneLoader loader;
        try {
            loader = PlaneLoaderFactory.getPlaneLoaderForInputFile(options.getInputDataInfo());
        } catch (final Exception ex) {
            System.err.println("EntropyCalculation::run() - Unable to get plane loader. " + ex.getMessage());
            return false;
        }

        try (final FileOutputStream fos = new FileOutputStream(options.getOutputFilePath(), false);
             final OutputStreamWriter reportWriter = new OutputStreamWriter(fos)) {

            reportWriter.write("Plane;Entropy\n");

            for (int planeIndex = 0; planeIndex < options.getInputDataInfo().getDimensions().getPlaneCount(); planeIndex++) {

                final int[] planeData = loader.loadPlaneData(0, planeIndex);
                final double planeEntropy = Utils.calculateEntropy(planeData);
                reportWriter.write(String.format("%d;%.4f\n", planeIndex, planeEntropy));
            }

        } catch (final IOException e) {
            e.printStackTrace();
            return false;
        }


        return true;
    }
}
