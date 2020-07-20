package azgracompress.io.loader;

import azgracompress.data.ImageU16;
import azgracompress.io.BufferInputData;
import azgracompress.io.InputData;
import azgracompress.utilities.TypeConverter;

import java.awt.*;
import java.io.IOException;
import java.rmi.server.ExportException;
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
        return new int[0];
    }

    @Override
    public int[] loadAllPlanesU16Data() throws IOException {
        return new int[0];
    }
}
