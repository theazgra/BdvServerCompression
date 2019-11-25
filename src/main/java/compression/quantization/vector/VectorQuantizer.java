package compression.quantization.vector;

import compression.utilities.Utils;

public class VectorQuantizer {

    public VectorQuantizer(final CodebookEntry[] codebook) {

    }

//    public double getMse(final int[] data) {
//        double mse = 0.0;
//        for (int i = 0; i < data.length; i++) {
//            int quantizedValue = quantize(data[i]);
//            mse += Math.pow(((double) data[i] - (double) quantizedValue), 2);
//        }
//        mse /= (double) data.length;
//        return mse;
//    }
//
//    public short[] quantize(short[] data) {
//        short[] result = new short[data.length];
//        for (int i = 0; i < data.length; i++) {
//            final int intRepresentationOfValue = Utils.shortBitsToInt(data[i]);
//            final int quantizedValue = quantize(intRepresentationOfValue);
//            final short shortRepresentation = Utils.u16BitsToShort(quantizedValue);
//            result[i] = shortRepresentation;
//        }
//        return result;
//    }

}
