package cache_memory_simulator;

public class MemoryCell {
    private final int address;
    private String data;
    public MemoryCell(int address, String data) {
        this.address = address;
        this.data = data;
    }
    public int getAddress() {
        return address;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
