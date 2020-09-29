# DataCompressor usage

DataCompressor allows compression of image files using scalar and vector quantization.

The application can load RAW image files and all formats supported by the [SCIFIO](https://imagej.net/SCIFIO) library.


Help output:
```
usage: azgracompress.DataCompressor [options] input
 -b,--bits <arg>                   Bit count per pixel [Default 8]
 -bench,--benchmark                Benchmark
 -c,--compress                     Compress 16 bit raw image
 -cbc,--codebook-cache <arg>       Folder of codebook caches
 -d,--decompress                   Decompress 16 bit raw image
 -h,--help                         Print help
 -i,--inspect                      Inspect the compressed file
 -mp,--middle-plane                Use middle plane for codebook creation
 -o,--output <arg>                 Custom output file
 -sq,--scalar-quantization         Use scalar quantization.
 -tcb,--train-codebook             Train codebook and save learned
                                   codebook to cache file.
 -v,--verbose                      Make program verbose
 -vq,--vector-quantization <arg>   Use vector quantization. Need to pass
                                   vector size eg. 9,9x1,3x3
 -wc,--worker-count <arg>          Number of worker threads
```

### Quantization types (QT):
- This program supports two different quantization types:
- Scalar quantization, selected by `-sq` or `--scalar-quantization`
- Vector quantization, selected by `-vq` or `--vector-quantization`
  - Vector quantization requires you to input the vector dimension after the flag
  - For one-dimensional row vectors you can the length as `9` or `9x1`
  - For two-dimensional matrix vectors the dimensions is set by `DxD` format, eg. `3x3`, `5x3`
  - For three-dimensional voxel vectors the dimensions is set by `DxDxD` format, eg. `3x3x3`, `5x3x2`

## Main program methods

### Compress
- Use with `-c` or `--compress`
- Compress the selected image planes (*Currently supporting only loading from RAW image files*).
- QT is required
- Set the bits per pixel using `-b` or `--bits` and integer value from 1 to 8. Codebook size is equal to (*2^bits*).
- Normally the codebook is created for each image plane separately, if one wants to use general codebook for all planes, these are the options:
  - Set the middle plane index using `-mp` or `--middle-plane`. Middle plane is used to create codebook for all planes.
  - Set the cache folder by `cbc` or `--codebook-cache`, quantizer will look for cached codebook of given file and codebook size.
- For input file info see Input File section

### Decompress
- Use with `-d` or `--decompress`
- Decompress the file compressed by this application.
- This method doesn't require any additional options.

### Inspect
- Use with `-i` or `--inspect` 
- Inspect the compressed image file or cached codebook. 
- Read compressed file header are write the information from that header.

### Train codebook
- Use with `-tcb` or `--train-codebook`
- QT is required
- This method load all the selected input planes and create one codebook.
- Codebook is saved to the cache folder configured by the `-o` option.
- Codebook is trained from planes configured by the input file, see Input File section

### Benchmark
- Use with `-bench` or `--benchmark `
- QT is required.
- Run benchmarking code on input planes with selected quantization type,

### Input file
- Input file is required for all methods.
- Decompress and inspect require only the input file path, while other also require its dimensions
- If the input file is RAW data file, then image dimensions must be provided as follows:
  - Input file dimensions are inputed in format of DxDxD [D] [D-D]
    - DxDxD is image dimension. Eg. 1920x1080x1, 1041x996x946 (946 planes of 1041x996 images)
    - [D] is optional plane index. Only this plane will be compressed.
    - [D-D] is optional plane range. Only plane in this range will be compressed.
  - D stands for integer values.
- Planes selected by the index or plane range are used for:
  - Compression
  - Training of codebook
  - Running benchmark


### Additional options:
- `-v`, `--verbose` - Make program output verbose.
- `-o`, `--output` - Set the ouput of compression, decompression, codebook training, benchmark.
- `-wc`, `--worker-count` - Set the number of worker threads.


[GitHub mirror link](https://github.com/theazgra/BdvServerCompression)
