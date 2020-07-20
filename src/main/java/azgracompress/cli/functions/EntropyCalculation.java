package azgracompress.cli.functions;

import azgracompress.cli.CustomFunctionBase;
import azgracompress.cli.ParsedCliOptions;
import azgracompress.data.ImageU16;
import azgracompress.io.RawDataIO;
import azgracompress.utilities.Utils;

import java.io.IOException;

public class EntropyCalculation extends CustomFunctionBase {
    /**
     * Base constructor with parsed CLI options.
     *
     * @param options Parsed cli options.
     */
    public EntropyCalculation(ParsedCliOptions options) {
        super(options);
    }

    @Override
    public boolean run() {
        ImageU16 plane = null;
        System.out.println(String.format("Input file: %s", options.getInputFilePath()));

        for (int planeIndex = 0; planeIndex < options.getImageDimension().getZ(); planeIndex++) {
            try {
                plane = RawDataIO.loadImageU16(options.getInputFilePath(),
                                               options.getImageDimension(),
                                               planeIndex);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            final double planeEntropy = Utils.calculateEntropy(plane.getData());

            System.out.println(String.format("%d\t%.4f", planeIndex, planeEntropy));
        }
        return true;
    }
}
