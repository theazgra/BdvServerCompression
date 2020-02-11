package azgracompress.benchmark;

import azgracompress.cli.ParsedCliOptions;

public class CompressionBenchmark {
    public static void runBenchmark(final ParsedCliOptions options) {
        switch (options.getQuantizationType()) {
            case Scalar: {
                ScalarQuantizationBenchmark sqBenchmark = new ScalarQuantizationBenchmark(options);
                sqBenchmark.startBenchmark();
            }
            break;
            case Vector1D:
            case Vector2D: {
                VectorQuantizationBenchmark vqBenchmark = new VectorQuantizationBenchmark(options);
                vqBenchmark.startBenchmark(options.getVectorDimension());
            }
            break;
            case Vector3D:
            case Invalid: {
                System.err.println("Unsupported benchmark type.");
            }
            break;
        }
    }
}
