## DataCompressor usage

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

### Application methods:
- `-c`, `--compress` - Compress image planes file (Currently only RAW image files).
  - Use scalar quantization using `-sq` or `--scalar-quantization`
  - Use vector quantization using `-vq` or `--vector-quantization` and specify the row vector size (9, 9x1, etc.) or matrix dimensions (2x2, 4x6, etc.) 
  - Set the bits per pixel amount using `-b` or `--bits` and integer value from 1 to 8. Codebook size is equal to (2^bits).
  - Set the reference plane index using `-rp` or `--reference-plane`. Reference plane is used to create codebook for all planes.
- `-d`, `--decompress` - Decompress the file compressed by this application. This options doesn't require any further parameters.
- `-i`, `--inspect` - Inspect the compressed file. Read compressed file header are write out informations about that file.

### Input file
- Input file is required for compress, decompress and inspect methods
- decompress and inspect require only the input file path
- compress additionaly requires the input file dimensions in format DxDxD [D] [D-D]
  - DxDxD is image dimension. Eg. 1920x1080x1, 1041x996x946 (946 planes of 1041x996 images)
  - [D] is optional plane index. Only this plane will be compressed.
  - [D-D] is optional plane range. Only plane in this range will be compressed.
- D stands for integer values.


### Additional options:
- `-v`, `--verbose` - Make program output verbose.
