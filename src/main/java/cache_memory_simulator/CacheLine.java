package cache_memory_simulator;

public class CacheLine {
    private int tag;
    private boolean valid;
    private String data;
    private int lineIndex;

    public CacheLine(int lineIndex) {
        this.valid = false;
        this.tag = -1;
        this.data = "";
        this.lineIndex = lineIndex;
    }
    // Getters and setters
    public int getTag(){
        return tag;
    }
    public void setTag(int tag){
        this.tag = tag;
    }

    public boolean isValid(){
        return valid;
    }
    public void setValid(boolean valid){
        this.valid = valid;
    }

    public String getData(){
        return data;
    }
    public void setData(String data){
        this.data = data;
    }

    public int getLineIndex() {
        return lineIndex;
    }
}
