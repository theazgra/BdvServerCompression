package azgracompress.benchmark;

import azgracompress.data.ImageU16;
import azgracompress.data.V3i;
import azgracompress.io.RawDataIO;
import azgracompress.quantization.QTrainIteration;
import azgracompress.utilities.TypeConverter;
import azgracompress.utilities.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

abstract class BenchmarkBase {
    protected final String inputFile;
    protected final String outputDirectory;
    protected final int[] planes;
    protected final V3i rawImageDims;

    protected BenchmarkBase(final String inputFile,
                            final String outputDirectory,
                            final int[] planes,
                            final V3i rawImageDims) {
        this.inputFile = inputFile;
        this.outputDirectory = outputDirectory;
        this.planes = planes;
        this.rawImageDims = rawImageDims;
    }

    /**
     * Construct filename path to the set outputDirectory.
     *
     * @param fileName File name.
     * @return File path.
     */
    protected String getFileNamePathIntoOutDir(final String fileName) {
        final File file = new File(outputDirectory, fileName);
        return file.getAbsolutePath();
    }

    /**
     * Load u16 plane from RAW file.
     *
     * @param planeIndex Zero based plane index.
     * @return u16 plane.
     */
    protected ImageU16 loadPlane(final int planeIndex) {
        try {
            return RawDataIO.loadImageU16(inputFile, rawImageDims, planeIndex);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Load U16 plane data from RAW file.
     *
     * @param planeIndex Zero based plane index.
     * @return U16 array of image plane data.
     */
    protected short[] loadPlaneData(final int planeIndex) {
        var plane = loadPlane(planeIndex);

        return (plane != null) ? plane.getData() : new short[0];
    }


    /**
     * Save quantized plane data to RAW file.
     *
     * @param data     Quantized plane data.
     * @param filename File storing quantized plane data.
     * @return True if file was saved.
     */
    protected boolean saveQuantizedPlaneData(final short[] data, final String filename) {
        ImageU16 img = new ImageU16(rawImageDims.getX(), rawImageDims.getY(), data);
        try {
            // NOTE(Moravec): Use big endian so that FIJI can read the image.
            RawDataIO.writeImageU16(getFileNamePathIntoOutDir(filename), img, false);
            System.out.println(String.format("Saved %s", filename));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Save both U16 absolute difference image and I32 difference values
     *
     * @param originalData    Original U16 plane data.
     * @param transformedData Quantized U16 plane data.
     * @param diffFile        File storing i32 difference values.
     * @param absDiffFile     File storing u16 absolute difference values.
     * @return True if both files were saved successfully.
     */
    protected boolean saveDifference(final short[] originalData,
                                     final short[] transformedData,
                                     final String diffFile,
                                     final String absDiffFile) {

        final int[] differenceData = Utils.getDifference(originalData, transformedData);
        final int[] absDifferenceData = Utils.applyAbsToValues(differenceData);
        final String diffFilePath = getFileNamePathIntoOutDir(diffFile);
        final String absDiffFilePath = getFileNamePathIntoOutDir(absDiffFile);

        ImageU16 img = new ImageU16(rawImageDims.getX(),
                                    rawImageDims.getY(),
                                    TypeConverter.intArrayToShortArray(absDifferenceData));
        try {
            // NOTE(Moravec): Use little endian so that gnuplot can read the array.
            RawDataIO.writeImageU16(absDiffFilePath, img, true);
            System.out.println("Saved absolute difference to: " + absDiffFilePath);

            RawDataIO.writeDataI32(diffFilePath, differenceData, true);
            System.out.println("Saved difference to: " + absDiffFilePath);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to save difference.");
            return false;
        }
        return true;
    }


    /**
     * Saves the QTrainLog array.
     *
     * @param filename    Log filename.
     * @param trainingLog QTrainingLog
     */
    protected void saveQTrainLog(final String filename, final QTrainIteration[] trainingLog) {
        final String CSV_HEADER = "It;AvgMSE;BestMSE;AvgPSNR;BestPSNR\n";
        try {
            FileOutputStream fileStream = new FileOutputStream(getFileNamePathIntoOutDir(filename));
            OutputStreamWriter writer = new OutputStreamWriter(fileStream);

            writer.write(CSV_HEADER);

            for (final QTrainIteration it : trainingLog) {
                writer.write(String.format("%d;%.5f;%.5f;%.5f;%.5f\n",
                                           it.getIteration(),
                                           it.getAverageMSE(),
                                           it.getBestMSE(),
                                           it.getAveragePSNR(),
                                           it.getBestPSNR()));
            }
            writer.flush();
            fileStream.flush();
            fileStream.close();
        } catch (IOException ioE) {
            ioE.printStackTrace();
            System.err.println("Failed to save QTtrain log.");
        }
    }

    public abstract void startBenchmark();

}