## azgracompress.DataCompressor usage

Help output:
```
usage: azgracompress.DataCompressor [options] input
-b,--bits <arg>                   Bit count per pixel [Default 8]
-c,--compress                     Compress 16 bit raw image
-d,--decompress                   Decompress 16 bit raw image
-h,--help                         Print help
-i,--inspect                      Inspect the compressed file
-o,--output <arg>                 Custom output file
-rp,--reference-plane <arg>       Reference plane index
-sq,--scalar-quantization         Use scalar quantization.
-v,--verbose                      Make program verbose
-vq,--vector-quantization <arg>   Use vector quantization. Need to pass
                                  vector size eg. 9,9x1,3x3
```