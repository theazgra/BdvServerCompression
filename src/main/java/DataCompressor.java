import org.apache.commons.math3.distribution.CauchyDistribution;
import quantization.LloydMaxU16ScalarQuantization;
import quantization.Utils;
import quantization.de.DeException;
import quantization.de.jade.JadeSolver;

import java.io.FileNotFoundException;


public class DataCompressor {
    public static void main(String[] args) throws FileNotFoundException {

        final String sourceFile = "D:\\tmp\\server-dump\\small.bin";
        final int NumberOfBits = 4;
        final int Dimension = (int) Math.pow(2, NumberOfBits);
        int[] values = Utils.convertU16BytesToInt(Utils.readFileBytes(sourceFile));

//        LloydMaxU16ScalarQuantization quantization = new LloydMaxU16ScalarQuantization(values, NumberOfBits);
//        quantization.train();

        JadeSolver jadeSolver = new JadeSolver(Dimension, 10 * Dimension, 200, 0.05, 0.1);
        jadeSolver.setTrainingData(values);

        try {
            jadeSolver.train();
        } catch (DeException e) {
            e.printStackTrace();
        }

        System.out.println("Finished learning...");

    }
}
