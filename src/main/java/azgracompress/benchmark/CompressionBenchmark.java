package azgracompress.benchmark;

import azgracompress.cli.CompressionOptionsCLIParser;

public class CompressionBenchmark {
    public static void runBenchmark(final CompressionOptionsCLIParser options) {
        Benchmark benchmark = new Benchmark(options);
        benchmark.startBenchmark();
//        switch (options.getQuantizationType()) {
//            case Scalar: {
//                SQBenchmark sqBenchmark = new SQBenchmark(options);
//                sqBenchmark.startBenchmark();
//            }
//            break;
//            case Vector1D:
//            case Vector2D: {
//                VQBenchmark vqBenchmark = new VQBenchmark(options);
//                vqBenchmark.startBenchmark(options.getVectorDimension());
//            }
//            break;
//            case Vector3D:
//            case Invalid: {
//                System.err.println("Unsupported benchmark type.");
//            }
//            break;
//        }
    }
}
