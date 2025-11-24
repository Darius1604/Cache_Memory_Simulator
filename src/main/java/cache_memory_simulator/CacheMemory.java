package cache_memory_simulator;

public interface CacheMemory {
    boolean read(int address);

    boolean write(int address, String data);

    CacheLine[] getLines();

    int getHits();

    int getMisses();

    String getLastMissType();

    int getBlockSize();
}
