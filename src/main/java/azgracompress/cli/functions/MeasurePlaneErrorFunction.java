package azgracompress.cli.functions;

import azgracompress.cli.CustomFunctionBase;
import azgracompress.cli.InputFileInfo;
import azgracompress.cli.ParsedCliOptions;
import azgracompress.data.ImageU16;
import azgracompress.data.V3i;
import azgracompress.io.RawDataLoader;
import azgracompress.utilities.Utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Arrays;

public class MeasurePlaneErrorFunction extends CustomFunctionBase {
    public MeasurePlaneErrorFunction(ParsedCliOptions options) {
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
        //        ImageU16 compressedPlane = null;
        //        ImageU16 originalPlane = null;
        //        ImageU16 differencePlane = null;

        PlaneError[] planeErrors = new PlaneError[dims.getZ()];

        InputFileInfo refFileInfo = new InputFileInfo(compFile);
        refFileInfo.setDimension(dims);
        InputFileInfo compFileInfo = new InputFileInfo(compressedFile);
        compFileInfo.setDimension(dims);

        final RawDataLoader refPlaneloader = new RawDataLoader(refFileInfo);
        final RawDataLoader compPlaneloader = new RawDataLoader(compFileInfo);

        Thread[] workers = new Thread[workerCount];
        final int workSize = dims.getZ() / workerCount;

        for (int wId = 0; wId < workerCount; wId++) {
            final int fromIndex = wId * workSize;
            final int toIndex = (wId == workerCount - 1) ? dims.getZ() : (workSize + (wId * workSize));

            workers[wId] = new Thread(() -> {

                ImageU16 originalPlane, compressedPlane, differencePlane;
                for (int planeIndex = fromIndex; planeIndex < toIndex; planeIndex++) {
                    try {
                        originalPlane = refPlaneloader.loadPlaneU16(planeIndex);
                        compressedPlane = compPlaneloader.loadPlaneU16(planeIndex);
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }


                    final int[] diffData = Utils.getDifference(originalPlane.getData(), compressedPlane.getData());
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
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try (FileOutputStream fos = new FileOutputStream(reportFile, false);
             OutputStreamWriter writer = new OutputStreamWriter(fos)) {

            writer.write("PlaneIndex\tErrorSum\tMeanError\n");

            for (final PlaneError planeError : planeErrors) {
                writer.write(String.format("%d\t%.4f\t%.4f\n",
                                           planeError.getPlaneIndex(),
                                           planeError.getAbsoluteError(),
                                           planeError.getMeanAbsoluteError()));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Finished reportPlaneDifference");
        return true;
    }
}
