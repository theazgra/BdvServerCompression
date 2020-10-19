package cz.it4i.qcmp.cli.functions;

import cz.it4i.qcmp.cli.CompressionOptionsCLIParser;
import cz.it4i.qcmp.cli.CustomFunctionBase;
import cz.it4i.qcmp.data.V3i;
import cz.it4i.qcmp.io.FileInputData;
import cz.it4i.qcmp.io.loader.RawDataLoader;
import cz.it4i.qcmp.utilities.Stopwatch;
import cz.it4i.qcmp.utilities.Utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Arrays;

public class MeasurePlaneErrorFunction extends CustomFunctionBase {
    public MeasurePlaneErrorFunction(final CompressionOptionsCLIParser options) {
        super(options);
    }

    private final String OriginalFileForChannel0 = "D:\\biology\\tiff_data\\fused_tp_10_ch_0_16bit.raw";
    private final String OriginalFileForChannel1 = "D:\\biology\\tiff_data\\fused_tp_10_ch_1_16bit.raw";
    private final V3i ReferenceFileDimensions = new V3i(1041, 996, 946);

    private int[][] loadPlanes(final String srcFile, final V3i imageDims) throws IOException {
        final FileInputData inputDataInfo = new FileInputData(srcFile);
        inputDataInfo.setDimension(imageDims);
        final RawDataLoader loader = new RawDataLoader(inputDataInfo);

        return loader.loadAllPlanesTo2DArray();
    }

    private int[][] loadReferenceData(final int channel) throws IOException {
        assert (channel == 0 || channel == 1);
        final String referenceFilePath = (channel == 0) ? OriginalFileForChannel0 : OriginalFileForChannel1;
        return loadPlanes(referenceFilePath, ReferenceFileDimensions);
    }

    @Override
    public boolean run() {
        final int channel = 0;
        final String srcFolder = "C:\\tmp";

        final Stopwatch stopwatch = Stopwatch.startNew("Load reference plane data.");
        final int[][] referenceData;
        try {
            referenceData = loadReferenceData(channel);
        } catch (final IOException e) {
            e.printStackTrace();
            return false;
        }
        stopwatch.stop();
        System.out.println(stopwatch);

        final String[] qMethods = {"sq", "vq9x1", "vq3x3", "vq3x3x3"};

        String codebookType = "global_codebook";
        for (final String qMethod : qMethods) {
            if (!runPlaneDifferenceForAllBits(referenceData, channel, qMethod, codebookType, srcFolder))
                return false;
        }

        codebookType = "individual_codebook";
        for (final String qMethod : qMethods) {
            if (qMethod.equals("vq3x3x3"))
                continue;
            if (!runPlaneDifferenceForAllBits(referenceData, channel, qMethod, codebookType, srcFolder))
                return false;
        }

        codebookType = "middle_plane_codebook";
        for (final String qMethod : qMethods) {
            if (qMethod.equals("vq3x3x3"))
                continue;
            if (!runPlaneDifferenceForAllBits(referenceData, channel, qMethod, codebookType, srcFolder))
                return false;
        }


        return true;
    }

    public boolean runPlaneDifferenceForAllBits(final int[][] referenceData,
                                                final int channel,
                                                final String quantizationMethod,
                                                final String codebookType,
                                                final String srcFolder) {


        final String reportFilePath = String.format("D:\\biology\\benchmark\\plane_diff_ch%d_%s_%s.log",
                                                    channel, codebookType, quantizationMethod);

        final Stopwatch stopwatch = Stopwatch.startNew(String.format("Channel=%d, CodebookType=%s, QMethod=%s",
                                                                     channel, codebookType, quantizationMethod));
        try (final FileOutputStream fos = new FileOutputStream(reportFilePath, false);
             final OutputStreamWriter reportWriter = new OutputStreamWriter(fos)) {

            final String template = "%s\\%s\\fused_tp_10_ch_%d_16bit_%s_%db.raw";
            final int[] bitsPerCodebookIndices = {2, 3, 4, 5, 6, 7, 8};
            for (final int bitsPerCodebookIndex : bitsPerCodebookIndices) {
                final String srcFile = String.format(template, srcFolder, codebookType, channel, quantizationMethod, bitsPerCodebookIndex);
                System.out.printf("Running plane difference for '%s', '%s', bpci=%d\n",
                                  codebookType,
                                  quantizationMethod,
                                  bitsPerCodebookIndex);
                try {
                    reportPlaneDifference(referenceData, srcFile, reportWriter);
                } catch (final IOException e) {
                    e.printStackTrace();
                    return false;
                }
            }

        } catch (final IOException e) {
            e.printStackTrace();
        }

        stopwatch.stop();
        System.out.println(stopwatch);
        return true;
    }

    private void reportPlaneDifference(final int[][] referenceData,
                                       final String testFile,
                                       final OutputStreamWriter reportWriter) throws IOException {

        final int[][] testData = loadPlanes(testFile, ReferenceFileDimensions);

        reportWriter.write("=========================================\n");
        reportWriter.write(testFile);
        reportWriter.write('\n');
        reportWriter.write("=========================================\n");
        reportWriter.write("PlaneIndex;ErrorSum;MeanError\n");

        final int planePixelCount = ReferenceFileDimensions.toV2i().multiplyTogether();
        final int[] diffData = new int[planePixelCount];

        for (int plane = 0; plane < ReferenceFileDimensions.getZ(); plane++) {
            Utils.differenceToArray(referenceData[plane], testData[plane], diffData);
            Utils.applyAbsFunction(diffData);

            final double absDiffSum = Arrays.stream(diffData).mapToDouble(v -> v).sum();
            final double meanPixelError = absDiffSum / (double) planePixelCount;

            reportWriter.write(String.format("%d;%.4f;%.4f\n", plane, absDiffSum, meanPixelError));
        }
    }
}
