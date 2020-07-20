package azgracompress.io.loader;

import azgracompress.data.ImageU16;
import azgracompress.data.V3i;
import azgracompress.io.BufferInputData;
import azgracompress.io.InputData;
import azgracompress.utilities.TypeConverter;
import java.io.IOException;
import java.util.Arrays;

public class ImageJBufferLoader implements IPlaneLoader {
    private final BufferInputData bufferInputData;

    public ImageJBufferLoader(BufferInputData bufferDataInfo) {
        this.bufferInputData = bufferDataInfo;
        assert (this.bufferInputData.getPixelType() == InputData.PixelType.Gray16);
    }

    private int[] copyShortArray(short[] srcArray, int srcOffset, int copyLen) {
        int[] destArray = new int[copyLen];
        for (int i = 0; i < copyLen; i++) {
            destArray[i] = TypeConverter.shortToInt(srcArray[srcOffset + i]);
        }
        return destArray;
    }

    private void copyShortArray(short[] srcArray, int srcOffset, int[] destArray, int destOffset, int copyLen) {
        for (int i = 0; i < copyLen; i++) {
            destArray[destOffset + i] = TypeConverter.shortToInt(srcArray[srcOffset + i]);
        }
    }

    @Override
    public ImageU16 loadPlaneU16(int plane) throws IOException {
        switch (bufferInputData.getPixelType()) {
            case Gray16: {
                final int planePixelCount = bufferInputData.getDimensions().getX() * bufferInputData.getDimensions().getY();
                final int offset = plane * planePixelCount;
                final short[] srcBuffer = (short[]) bufferInputData.getBuffer();
                return new ImageU16(bufferInputData.getDimensions().getX(),
                        bufferInputData.getDimensions().getY(),
                        copyShortArray(srcBuffer, offset, planePixelCount));
            }
            default:
                throw new IOException("Unable to load unsupported pixel type.");
        }
    }

    @Override
    public int[] loadPlanesU16Data(int[] planes) throws IOException {
        if (planes.length < 1) {
            return new int[0];
        } else if (planes.length == 1) {
            return loadPlaneU16(planes[0]).getData();
        } else if (planes.length == bufferInputData.getDimensions().getZ()) { // Maybe?
            return loadAllPlanesU16Data();
        }
        switch (bufferInputData.getPixelType()) {
            case Gray16: {
                final int planePixelCount = bufferInputData.getDimensions().getX() * bufferInputData.getDimensions().getY();

                final long totalValueCount = (long) planePixelCount * (long) planes.length;

                if (totalValueCount > (long) Integer.MAX_VALUE) {
                    throw new IOException("Unable to load image data for planes, file size is too big.");
                }

                Arrays.sort(planes);
                final short[] srcBuffer = (short[]) bufferInputData.getBuffer();
                int[] destBuffer = new int[(int) totalValueCount];

                for (int index = 0; index < planes.length; index++) {
                    final int srcOffset = planes[index] * planePixelCount;
                    final int destOffset = index * planePixelCount;

                    copyShortArray(srcBuffer, srcOffset, destBuffer, destOffset, planePixelCount);
                }
                return destBuffer;
            }
            default:
                throw new IOException("Unable to load unsupported pixel type.");
        }
    }

    @Override
    public int[] loadAllPlanesU16Data() throws IOException {
        switch (bufferInputData.getPixelType()) {
            case Gray16: {
                final V3i imageDims = bufferInputData.getDimensions();
                final long dataSize = (long) imageDims.getX() * (long) imageDims.getY() * (long) imageDims.getZ();

                if (dataSize > (long) Integer.MAX_VALUE) {
                    throw new IOException("Unable to load all image data, file size is too big.");
                }

                final short[] srcBuffer = (short[]) bufferInputData.getBuffer();
                return copyShortArray(srcBuffer, 0, (int) dataSize);
            }
            default:
                throw new IOException("Unable to load unsupported pixel type.");
        }
    }
}
