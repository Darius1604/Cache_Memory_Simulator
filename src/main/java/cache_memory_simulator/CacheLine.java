package cache_memory_simulator;

public class CacheLine {
    private int tag;
    private boolean valid;
    private String[] data;
    private int lineIndex;
    private boolean dirty;

    public CacheLine(int lineIndex, int blockSize) {
        this.valid = false;
        this.tag = -1;
        this.data = new String[blockSize];
        this.lineIndex = lineIndex;
        for (int i = 0; i < blockSize; i++) {
            this.data[i] = "";
        }
        this.dirty = false;
    }

    // Getters and setters
    public int getTag() {
        return tag;
    }

    public void setTag(int tag) {
        this.tag = tag;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String[] getData() {
        return data;
    }

    public void setData(String[] data) {
        this.data = data;
    }

    public int getLineIndex() {
        return lineIndex;
    }

    public String getDataAsString() {
        if (data == null) return "";
        return String.join(", ", data); // join array elements as a comma-separated string
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }
}
