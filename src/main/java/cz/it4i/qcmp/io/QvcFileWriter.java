package cz.it4i.qcmp.io;

import cz.it4i.qcmp.data.V3i;
import cz.it4i.qcmp.fileformat.QuantizationType;
import cz.it4i.qcmp.fileformat.QvcHeaderV2;
import cz.it4i.qcmp.fileformat.SqQvcFile;
import cz.it4i.qcmp.fileformat.VqQvcFile;
import cz.it4i.qcmp.quantization.scalar.SQCodebook;
import cz.it4i.qcmp.quantization.vector.VQCodebook;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class QvcFileWriter {
    /**
     * Create QVC file header for SqQvc file.
     *
     * @param trainFile Image file used for training.
     * @param codebook  Final SQ codebook.
     * @return SQ cache file header.
     */
    private static QvcHeaderV2 createQvcHeaderForSQ(final String trainFile, final SQCodebook codebook) {
        final QvcHeaderV2 header = new QvcHeaderV2();
        header.setQuantizationType(QuantizationType.Scalar);
        header.setCodebookSize(codebook.getCodebookSize());
        header.setTrainFileName(trainFile);
        header.setVectorDims(new V3i(0));
        return header;
    }

    /**
     * Find the correct quantization type based on vector dimension.
     *
     * @param vectorDims Quantization vector dimensions.
     * @return Correct QuantizationType.
     */
    private static QuantizationType getQuantizationTypeFromVectorDimensions(final V3i vectorDims) {
        if (vectorDims.getX() > 1) {
            if (vectorDims.getY() == 1 && vectorDims.getZ() == 1) {
                return QuantizationType.Vector1D;
            } else if (vectorDims.getY() > 1 && vectorDims.getZ() == 1) {
                return QuantizationType.Vector2D;
            } else {
                return QuantizationType.Vector3D;
            }
        } else if (vectorDims.getX() == 1 && vectorDims.getY() > 1 && vectorDims.getZ() == 1) {
            return QuantizationType.Vector1D;
        }
        return QuantizationType.Invalid;
    }

    /**
     * Create QVC file header for VqQvc file.
     *
     * @param trainFile Image file used for training.
     * @param codebook  Final VQ codebook.
     * @return VQ cache file header.
     */
    private static QvcHeaderV2 createQvcHeaderForVq(final String trainFile, final VQCodebook codebook) {
        final QvcHeaderV2 header = new QvcHeaderV2();
        header.setQuantizationType(getQuantizationTypeFromVectorDimensions(codebook.getVectorDims()));
        header.setCodebookSize(codebook.getCodebookSize());
        header.setTrainFileName(trainFile);
        header.setVectorDims(codebook.getVectorDims());
        return header;
    }

    /**
     * Save scalar quantization codebook as QVC file in file specified by path.
     *
     * @param path      Cache file path.
     * @param trainFile Image file used for training.
     * @param codebook  Scalar quantization codebook.
     * @throws IOException when fails to write the cache file.
     */
    public static void writeSqCacheFile(final String path, final String trainFile, final SQCodebook codebook) throws IOException {
        final QvcHeaderV2 header = createQvcHeaderForSQ(new File(trainFile).getName(), codebook);
        final SqQvcFile cacheFile = new SqQvcFile(header, codebook);

        try (final FileOutputStream fos = new FileOutputStream(path, false);
             final DataOutputStream dos = new DataOutputStream(fos)) {

            cacheFile.writeToStream(dos);
        } catch (final IOException ex) {
            throw new IOException("Failed to save SQ QVC file\n" + ex.getMessage());
        }
    }

    /**
     * Save vector quantization codebook as QVC file in file specified by path.
     *
     * @param path      Cache file path.
     * @param trainFile Image file used for training.
     * @param codebook  Vector quantization codebook.
     * @throws IOException when fails to save the cache file.
     */
    public static void writeVqCacheFile(final String path, final String trainFile, final VQCodebook codebook) throws IOException {
        final QvcHeaderV2 header = createQvcHeaderForVq(new File(trainFile).getName(), codebook);
        final VqQvcFile cacheFile = new VqQvcFile(header, codebook);

        try (final FileOutputStream fos = new FileOutputStream(path, false);
             final DataOutputStream dos = new DataOutputStream(fos)) {

            cacheFile.writeToStream(dos);
        } catch (final IOException ex) {
            throw new IOException("Failed to save VQ QVC file\n" + ex.getMessage());
        }
    }
}
