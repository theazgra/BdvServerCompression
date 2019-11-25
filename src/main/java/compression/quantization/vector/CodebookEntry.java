package compression.quantization.vector;

public class CodebookEntry {

    int[] vector;

    public CodebookEntry(final int[] codebook) {
        this.vector = codebook;
    }

    public boolean isZeroVector() {
        for (int val : vector) {
            if (val != 0)
                return false;
        }
        return true;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof CodebookEntry) {
            final CodebookEntry ceObj = (CodebookEntry) obj;
            if (vector.length != ceObj.vector.length) {
                return false;
            }
            for (int i = 0; i < vector.length; i++) {
                if (vector[i] != ceObj.vector[i]) {
                    return false;
                }
            }
            return true;
        }
        return super.equals(obj);
    }


    public int[] getVector() {
        return vector;
    }
}
