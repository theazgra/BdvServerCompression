package azgracompress.cli.functions;

import azgracompress.cli.CustomFunctionBase;
import azgracompress.cli.ParsedCliOptions;
import azgracompress.data.ImageU16;
import azgracompress.data.V3i;
import azgracompress.io.RawDataIO;
import azgracompress.utilities.Utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Arrays;

public class MeasurePlaneErrorFunction extends CustomFunctionBase {
    public MeasurePlaneErrorFunction(ParsedCliOptions options) {
        super(options);
    }

    @Override
    public boolean run() {
//        if (reportPlaneDifference(
//                "D:\\biology\\tiff_data\\quantized\\middle_frame\\fused_tp_10_ch_1_16bit_sq_cb256.raw",
//                "D:\\biology\\tiff_data\\quantized\\middle_frame\\plane_diff_ch1\\sq_cb256_plane_log.data")) {
//            return false;
//        }

//        if (reportPlaneDifference(
//                "D:\\biology\\tiff_data\\quantized\\middle_frame\\fused_tp_10_ch_1_16bit_vq3x3_cb128.raw",
//                "D:\\biology\\tiff_data\\quantized\\middle_frame\\plane_diff_ch1\\vq3x3_cb128_plane_log.data")) {
//            return false;
//        }
//
//        if (reportPlaneDifference(
//                "D:\\biology\\tiff_data\\quantized\\middle_frame\\fused_tp_10_ch_1_16bit_vq3x3_cb64.raw",
//                "D:\\biology\\tiff_data\\quantized\\middle_frame\\plane_diff_ch1\\vq3x3_cb64_plane_log.data")) {
//            return false;
//        }
//
//        if (reportPlaneDifference(
//                "D:\\biology\\tiff_data\\quantized\\middle_frame\\fused_tp_10_ch_1_16bit_vq3x3_cb32.raw",
//                "D:\\biology\\tiff_data\\quantized\\middle_frame\\plane_diff_ch1\\vq3x3_cb32_plane_log.data")) {
//            return false;
//        }
//
//        if (reportPlaneDifference(
//                "D:\\biology\\tiff_data\\quantized\\middle_frame\\fused_tp_10_ch_1_16bit_vq3x3_cb16.raw",
//                "D:\\biology\\tiff_data\\quantized\\middle_frame\\plane_diff_ch1\\vq3x3_cb16_plane_log.data")) {
//            return false;
//        }
//
//        if (reportPlaneDifference(
//                "D:\\biology\\tiff_data\\quantized\\middle_frame\\fused_tp_10_ch_1_16bit_vq3x3_cb8.raw",
//                "D:\\biology\\tiff_data\\quantized\\middle_frame\\plane_diff_ch1\\vq3x3_cb8_plane_log.data")) {
//            return false;
//        }
//
        if (reportPlaneDifference(
                "D:\\biology\\tiff_data\\quantized\\middle_frame\\fused_tp_10_ch_1_16bit_sq_cb4.raw",
                "D:\\biology\\tiff_data\\quantized\\middle_frame\\plane_diff_ch1\\sq_cb4_plane_log.data")) {
            return false;
        }


        return true;
    }

    private boolean reportPlaneDifference(final String compressedFile, final String reportFile) {
        final String referenceFile = "D:\\biology\\tiff_data\\benchmark\\fused_tp_10_ch_1_16bit.raw";

        final V3i dims = new V3i(1041, 996, 946);
        final int planePixelCount = dims.getX() * dims.getY();
        System.out.println(options.report());
        System.out.println("Run custom function.");
        ImageU16 compressedPlane = null;
        ImageU16 originalPlane = null;
        ImageU16 differencePlane = null;

        PlaneError[] planeErrors = new PlaneError[dims.getZ()];

        for (int planeIndex = 0; planeIndex < dims.getZ(); planeIndex++) {
            try {
                originalPlane = RawDataIO.loadImageU16(referenceFile, dims, planeIndex);
                compressedPlane = RawDataIO.loadImageU16(compressedFile, dims, planeIndex);
            } catch (IOException e) {
                e.printStackTrace();
                return true;
            }
            final int[] diffData = Utils.getDifference(originalPlane.getData(), compressedPlane.getData());
            Utils.applyAbsFunction(diffData);


            final double absDiffSum = Arrays.stream(diffData).mapToDouble(v -> v).sum();
            final double meanPixelError = absDiffSum / (double) planePixelCount;

            planeErrors[planeIndex] = new PlaneError(planeIndex, absDiffSum, meanPixelError);
            //            System.out.println("Finished plane: " + planeIndex);
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
        return false;
    }
}
