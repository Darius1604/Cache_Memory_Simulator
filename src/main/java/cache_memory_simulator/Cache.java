package cache_memory_simulator;

public class Cache {
    private CacheLine[] lines;
    private int blockSize;
    private int hits = 0;
    private int misses = 0;

    public Cache(int size, int blockSize){
        lines = new CacheLine[size];
        for(int i = 0; i < size; i++)
            lines[i] = new CacheLine(i);
        this.blockSize = blockSize;
    }

    public boolean access(int address, int blockSize){
        int lineIndex = (address / blockSize) % lines.length;
        int tag = address / blockSize;

        CacheLine line = lines[lineIndex];

        if(line.isValid() && line.getTag() == tag){
            hits++;
            return true; // Cache hit
        }
        else{
            misses++;
            line.setTag(tag);
            line.setValid(true);
            line.setData("Data@" + address);
            return false;
        }
    }

    public CacheLine[] getLines(){
        return lines;
    }

    public int getHits(){
        return hits;
    }

    public int getMisses(){
        return misses;
    }

    public int getBlockSize() {
        return blockSize;
    }
}
