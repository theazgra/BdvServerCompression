package cz.it4i.qcmp.cli.functions;

import cz.it4i.qcmp.cli.CompressionOptionsCLIParser;
import cz.it4i.qcmp.cli.CustomFunctionBase;
import cz.it4i.qcmp.data.V3i;
import cz.it4i.qcmp.io.FileInputData;
import cz.it4i.qcmp.io.loader.RawDataLoader;
import cz.it4i.qcmp.utilities.Utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Arrays;

public class MeasurePlaneErrorFunction extends CustomFunctionBase {
    public MeasurePlaneErrorFunction(final CompressionOptionsCLIParser options) {
        super(options);
    }

    private final String COMP_FILE_ch0 = "D:\\biology\\tiff_data\\benchmark\\fused_tp_10_ch_0_16bit.raw";
    private final String COMP_FILE_ch1 = "D:\\biology\\tiff_data\\benchmark\\fused_tp_10_ch_1_16bit.raw";

    @Override
    public boolean run() {


        boolean result = true;

        result &= runPlaneDifferenceForAllBits(0, "sq", "file_codebook", "D:\\biology\\tiff_data\\quantized");
        result &= runPlaneDifferenceForAllBits(0, "vq3x3", "file_codebook", "D:\\biology\\tiff_data\\quantized");
        result &= runPlaneDifferenceForAllBits(0, "vq9x1", "file_codebook", "D:\\biology\\tiff_data\\quantized");

        result &= runPlaneDifferenceForAllBits(1, "sq", "file_codebook", "D:\\biology\\tiff_data\\quantized");
        result &= runPlaneDifferenceForAllBits(1, "vq3x3", "file_codebook", "D:\\biology\\tiff_data\\quantized");
        result &= runPlaneDifferenceForAllBits(1, "vq9x1", "file_codebook", "D:\\biology\\tiff_data\\quantized");

        //        result &= reportPlaneDifference(
        //                String.format("%s\\%s\\fused_tp_10_ch_%d_16bit_%s_cb4.raw",
        //                              "D:\\biology\\tiff_data\\quantized",
        //                              "middle_frame",
        //                              1,
        //                              "sq"),
        //                String.format("%s\\%s\\plane_diff_ch%d\\%s_cb4_plane_log.data",
        //                              "D:\\biology\\tiff_data\\quantized",
        //                              "middle_frame",
        //                              1,
        //                              "sq"),
        //                COMP_FILE_ch1);

        return result;
    }

    public boolean runPlaneDifferenceForAllBits(final int channel,
                                                final String method,
                                                final String type,
                                                final String folder) {
        System.out.println(
                String.format("runPlaneDifferenceForAllBits\n\tChannel: %d\n\tMethod: %s\n\tType: %s",
                              channel, type, folder));
        //        final int channel = 0;
        assert (channel == 0 || channel == 1);
        final String comp_file = channel == 0 ? COMP_FILE_ch0 : COMP_FILE_ch1;
        //        final String method = "sq";
        //        final String type = "plane_codebook";
        //        final String folder = "D:\\biology\\tiff_data\\quantized";

        if (!reportPlaneDifference(
                String.format("%s\\%s\\fused_tp_10_ch_%d_16bit_%s_cb256.raw", folder, type, channel, method),
                String.format("%s\\%s\\plane_diff_ch%d\\%s_cb256_plane_log.data", folder, type, channel, method),
                comp_file)) {
            return false;
        }

        if (!reportPlaneDifference(
                String.format("%s\\%s\\fused_tp_10_ch_%d_16bit_%s_cb128.raw", folder, type, channel, method),
                String.format("%s\\%s\\plane_diff_ch%d\\%s_cb128_plane_log.data", folder, type, channel, method),
                comp_file)) {
            return false;
        }

        if (!reportPlaneDifference(
                String.format("%s\\%s\\fused_tp_10_ch_%d_16bit_%s_cb64.raw", folder, type, channel, method),
                String.format("%s\\%s\\plane_diff_ch%d\\%s_cb64_plane_log.data", folder, type, channel, method),
                comp_file)) {
            return false;
        }

        if (!reportPlaneDifference(
                String.format("%s\\%s\\fused_tp_10_ch_%d_16bit_%s_cb32.raw", folder, type, channel, method),
                String.format("%s\\%s\\plane_diff_ch%d\\%s_cb32_plane_log.data", folder, type, channel, method),
                comp_file)) {
            return false;
        }

        if (!reportPlaneDifference(
                String.format("%s\\%s\\fused_tp_10_ch_%d_16bit_%s_cb16.raw", folder, type, channel, method),
                String.format("%s\\%s\\plane_diff_ch%d\\%s_cb16_plane_log.data", folder, type, channel, method),
                comp_file)) {
            return false;
        }

        if (!reportPlaneDifference(
                String.format("%s\\%s\\fused_tp_10_ch_%d_16bit_%s_cb8.raw", folder, type, channel, method),
                String.format("%s\\%s\\plane_diff_ch%d\\%s_cb8_plane_log.data", folder, type, channel, method),
                comp_file)) {
            return false;
        }

        if (!reportPlaneDifference(
                String.format("%s\\%s\\fused_tp_10_ch_%d_16bit_%s_cb4.raw", folder, type, channel, method),
                String.format("%s\\%s\\plane_diff_ch%d\\%s_cb4_plane_log.data", folder, type, channel, method),
                comp_file)) {
            return false;
        }


        return true;
    }

    private boolean reportPlaneDifference(final String compressedFile, final String reportFile, final String compFile) {
        final int workerCount = 8;
        final V3i dims = new V3i(1041, 996, 946);
        final int planePixelCount = dims.getX() * dims.getY();

        final PlaneError[] planeErrors = new PlaneError[dims.getZ()];

        final FileInputData refFileInfo = new FileInputData(compFile);
        refFileInfo.setDimension(dims);
        final FileInputData compFileInfo = new FileInputData(compressedFile);
        compFileInfo.setDimension(dims);

        final RawDataLoader refPlaneloader = new RawDataLoader(refFileInfo);
        final RawDataLoader compPlaneloader = new RawDataLoader(compFileInfo);

        final Thread[] workers = new Thread[workerCount];
        final int workSize = dims.getZ() / workerCount;

        for (int wId = 0; wId < workerCount; wId++) {
            final int fromIndex = wId * workSize;
            final int toIndex = (wId == workerCount - 1) ? dims.getZ() : (workSize + (wId * workSize));

            workers[wId] = new Thread(() -> {


                int[] originalPlaneData, compressedPlaneData;
                for (int planeIndex = fromIndex; planeIndex < toIndex; planeIndex++) {
                    try {
                        originalPlaneData = refPlaneloader.loadPlaneData(planeIndex);
                        compressedPlaneData = compPlaneloader.loadPlaneData(planeIndex);
                    } catch (final IOException e) {
                        e.printStackTrace();
                        break;
                    }


                    final int[] diffData = Utils.getDifference(originalPlaneData, compressedPlaneData);
                    Utils.applyAbsFunction(diffData);

                    final double absDiffSum = Arrays.stream(diffData).mapToDouble(v -> v).sum();
                    final double meanPixelError = absDiffSum / (double) planePixelCount;

                    planeErrors[planeIndex] = new PlaneError(planeIndex, absDiffSum, meanPixelError);
                }
            });

            workers[wId].start();
        }
        try {
            for (int wId = 0; wId < workerCount; wId++) {
                workers[wId].join();
            }
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }

        try (final FileOutputStream fos = new FileOutputStream(reportFile, false);
             final OutputStreamWriter writer = new OutputStreamWriter(fos)) {

            writer.write("PlaneIndex\tErrorSum\tMeanError\n");

            for (final PlaneError planeError : planeErrors) {
                writer.write(String.format("%d\t%.4f\t%.4f\n",
                                           planeError.getPlaneIndex(),
                                           planeError.getAbsoluteError(),
                                           planeError.getMeanAbsoluteError()));
            }

        } catch (final IOException e) {
            e.printStackTrace();
        }
        System.out.println("Finished reportPlaneDifference");
        return true;
    }

    /*
    final String[] templates = new String[]{
                "D:\\biology\\benchmark\\jpeg_comp\\jpeg2000\\ch0_400_cr%d.%s",
                "D:\\biology\\benchmark\\jpeg_comp\\jpeg2000\\ch1_683_cr%d.%s"
        };
        final String[] referenceFiles = new String[]{
                "D:\\biology\\benchmark\\jpeg_comp\\ch0_400.raw",
                "D:\\biology\\benchmark\\jpeg_comp\\ch1_683.raw"
        };
        final int[] CRS = new int[]{1, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30};
        try {

            for (int i = 0; i < templates.length; i++) {
                final String refFile = referenceFiles[i];
                final int[] refData = loadImageData(refFile);

                for (final int cr : CRS) {
                    final String inputFile = String.format(templates[i], cr, "raw");

                    final int[] imageData = loadImageData(inputFile);

                    final int[] diff = Utils.getDifference(refData, imageData);
                    final double mse = Utils.calculateMse(diff);

                    final double psnr = Utils.calculatePsnr(mse, U16.Max);

                    final String channel = new File(inputFile).getName().substring(0, 3);
                    DecimalFormat df = new DecimalFormat("#.####");

                    System.out.println(String.format("%s CR: %d\n\tMSE: %s\n\tPSNR: %s\n",
                                                     channel, cr, df.format(mse), df.format(psnr)));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ScifioWrapper.dispose();
        }
    * */


}