package cz.it4i.qcmp.cli.functions;

import cz.it4i.qcmp.cli.CompressionOptionsCLIParser;
import cz.it4i.qcmp.cli.CustomFunctionBase;
import cz.it4i.qcmp.io.RawDataIO;
import cz.it4i.qcmp.io.loader.PlaneLoaderFactory;
import cz.it4i.qcmp.utilities.TypeConverter;
import cz.it4i.qcmp.utilities.Utils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

public class CalculateDifference extends CustomFunctionBase {

    /**
     * Base constructor with parsed CLI options.
     *
     * @param options Parsed cli options.
     */
    public CalculateDifference(final CompressionOptionsCLIParser options) {
        super(options);
    }

    @Override
    public boolean run() {
        assert (options.getInputDataInfo().isPlaneIndexSet());
        final int planeIndex = options.getInputDataInfo().getPlaneIndex();
        final String directory = options.getOutputFilePath();
        final int[] referenceData;
        try {
            referenceData = PlaneLoaderFactory.getPlaneLoaderForInputFile(options.getInputDataInfo()).loadPlaneData(0, planeIndex);
        } catch (final Exception e) {
            e.printStackTrace();
            return false;
        }

        final Collection<File> rawFiles = FileUtils.listFiles(new File(directory), new String[]{"raw"}, false);
        //        final Collection<File> rawFiles = Arrays.asList(new File("gc_vq3x3_8b_670.raw"),
        //                                                        new File("mpc_vq3x3_8b_670.raw"),
        //                                                        new File("ic_vq3x3_8b_670.raw"));
        for (final File file : rawFiles) {
            final String absoluteDiffResultFile = file.getAbsolutePath().replace(".raw", "_adiff.raw");
            final String diffResultFile = file.getAbsolutePath().replace(".raw", "_diff.raw");
            final int[] fileData;
            try {
                final byte[] fileBytes = FileUtils.readFileToByteArray(file);
                fileData = TypeConverter.unsignedShortBytesToIntArray(fileBytes);
            } catch (final IOException e) {
                e.printStackTrace();
                return false;
            }

            final int[] diffData = Utils.getDifference(referenceData, fileData);
            try {
                RawDataIO.writeDataI32(diffResultFile, diffData, true);
            } catch (final IOException e) {
                e.printStackTrace();
                return false;
            }
            Utils.applyAbsFunction(diffData);

            final byte[] resultBytes = TypeConverter.unsignedShortArrayToByteArray(diffData, true);
            try {
                FileUtils.writeByteArrayToFile(new File(absoluteDiffResultFile), resultBytes);
            } catch (final IOException e) {
                e.printStackTrace();
                return false;
            }
            System.out.println("Finished: " + diffResultFile);
        }

        return true;
    }
}
