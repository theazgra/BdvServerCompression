package cz.it4i.qcmp.fileformat;

import cz.it4i.qcmp.data.V3i;

public interface IQvcHeader extends IFileHeader {
    QuantizationType getQuantizationType();

    int getBitsPerCodebookIndex();

    int getCodebookSize();

    V3i getVectorDim();
}
