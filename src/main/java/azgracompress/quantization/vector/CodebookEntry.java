package azgracompress.quantization.vector;

public class CodebookEntry {

    final int[] vector;
    final int width;
    final int height;

    public CodebookEntry(final int[] codebook) {

        this.vector = codebook;
        this.width = codebook.length;
        this.height = 1;
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

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int[] getVector() {
        return vector;
    }

    public String getVectorString(StringBuilder sb) {
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i != (vector.length - 1))
                sb.append(';');
        }
        sb.append('\n');
        return sb.toString();
    }

    public String getVectorString() {
        return getVectorString(new StringBuilder());
    }
}
