# File formats used by QcmpCompressionLibrary


QCMP (Quantization compressed) file formats:
- [Version 1](#first-version-of-qcmp-file-header)

QVC (Quantization Value Cache) file formats:
- [Version 1](#qvc-file-format-v1)
- [Version 2](#qvc-file-format-v2)

Enumerations shared by all formats:
- [Quantization type enumeration](#quantization-type-enumeration)

## First version of QCMP file header

### Pros:
- Very compact.
- Just works for compression/decompression of stack images.
### Cons:
- No support for hyperstack images. (Images with multiple channels or timepoints)
- No support for metadata.
- No reserver bytes left for future improvements.

| Offset          | Size   | Type        | Possible values                        | Note                                                |
|-----------------|--------|-------------|----------------------------------------|-----------------------------------------------------|
|0                |8       |ASCII String |QCMPFILE                                |Magic value                                          |
|8                |1       |u8           |0 = SQ, 1 = VQ 1D, 2 = VQ 2D, 3 = VQ 3D |Quantization type                                    |
|9                |1       |u8           |1 – 255                                 |Bits Per Pixel (BPP), 2^BPP = Codebook size          |
|10               |1       |u8           |0, 1                                    |0 = One codebook for all, 1 = One Codebook per plane |
|11               |2       |u16          |0 - 65535                               |Image size X                                         |
|13               |2       |u16          |0 - 65535                               |Image size Y                                         |
|15               |2       |u16          |0 - 65535                               |Image size Z (ZSize)                                 |
|17               |2       |u16          |0 - 65535                               |Vector size X                                        |
|19               |2       |u16          |0 - 65535                               |Vector size Y                                        |
|21               |2       |u16          |0 - 65535                               |Vector size Z                                        |
|23               |4*ZSise |u32          |                                        |Plane data sizes                                     |
|23 + (4*ZSise)   |        |u8[]         |                                        |Data                                                 |

### Note on data sector:
Data sector consists of codebook data and indices data. If the file uses a single codebook then it is located at the start of the data sector. After the codebook is read, only image data remains to be decoded.
Otherwise (codebook per plane), there are always codebook data followed by the plane indices followed by another plane codebook and so on.


## QVC File Format V1
QCMP cache file (QVC) is used to store trained codebook for image file. The coder can load the cache file and encode the source image directly without needing to learn the codebook.

In the first version the Huffman tree is recontructed from the absolute frequencies of codebook indices, which is not space optimal.

| Sector    | Offset  | Size   | Type        | Note                                                |
| --------- |---------|--------|-------------|-----------------------------------------------------|
| **Header**|         |        |             |                                                     |
|           |0        |9       |ASCII String |`QCMPCACHE` Magic value                              |
|           |9        |1       |u8           |Quantization type                                    |
|           |10       |2       |u16          |Codebook size                                        |
|           |12       |2       |u16          |STFN=Size of the train file name                     |
|           |14       |STFN    |ASCII String |Train file name                                      |
|           |14+STFN  |2       |u16          |Vector size X                                        |
|           |16+STFN  |2       |u16          |Vector size Y                                        |
|           |18+STFN  |2       |u16          |Vector size Z                                        |
| **Data**  |         |                      |                                                     |
|           |         |        |u16[]        |Quantization values                                  |
|           |         |        |u64[]        |Huffman symbol frequencies                           |

## QVC File Format V2
Second version of QVC format is based on the first version and Header sector is almost the same, we just added the size of the huffman binary data.
The difference is in the data sector and in the binary representation of Huffman tree.
| Sector    | Offset  | Size   | Type        | Note                                                |
| --------- |---------|--------|-------------|-----------------------------------------------------|
| **Header**|         |        |             |                                                     |
|           |0        |9       |ASCII String |`QVCFILEV2` Magic value                              |
|           |9        |1       |u8           |Quantization type                                    |
|           |10       |2       |u16          |Codebook size                                        |
|           |12       |2       |u16          |STFN=Size of the train file name                     |
|           |14       |STFN    |ASCII String |Train file name                                      |
|           |14+STFN  |2       |u16          |Vector size X                                        |
|           |16+STFN  |2       |u16          |Vector size Y                                        |
|           |18+STFN  |2       |u16          |Vector size Z                                        |
|           |20+STFN  |2       |u16          |Huffman binary data size                             |
|           |22+STFN  |10      |u8           |Reserved bytes                                       |
| **Data**  |         |                      |                                                     |
|           |         |        |u16[]        |Quantization values                                  |
|           |         |        |u8[]         |Binary encoded Huffman tree with symbols             |

### Quantization type enumeration
Type is encoded using a single byte.
- `0` - Scalar Quantization
- `1` - Vector Quantization 1D (Row vector)
- `2` - Vector Quantization 2D (Matrix vector)
- `3` - Vector Quantization 3D (Tensor vector)