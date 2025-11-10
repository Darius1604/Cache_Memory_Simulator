package cache_memory_simulator;

import java.util.HashSet;
import java.util.Set;

public class DirectMappedCache implements CacheMemory {
    private CacheLine[] lines;
    private Memory memory;
    private int blockSize;
    private int hits = 0;
    private int misses = 0;
    private Set<Integer> seenBlocks = new HashSet<>();
    private String lastMissType;


    public DirectMappedCache(int size, int blockSize, Memory memory){
        lines = new CacheLine[size];
        for(int i = 0; i < size; i++)
            lines[i] = new CacheLine(i, blockSize);
        this.blockSize = blockSize;
        this.memory = memory;
    }

    public boolean read(int address) {
        int blockNumber = address / blockSize;
        int lineIndex = (address / blockSize) % lines.length;
        int tag = address / blockSize;

        CacheLine line = lines[lineIndex];
        int offset = address % blockSize; // position within the block
        if(line.isValid() && line.getTag() == tag){
            hits++;
            return true;
        }

            // DirectMappedCache miss - fetch entire block from memory
        misses++;

        boolean isCompulsory = !seenBlocks.contains(blockNumber);

        String[] blockData = new String[blockSize];
        for(int i=0; i<blockSize; i++){
            int memAddress = blockNumber * blockSize + i;
            if (memAddress < memory.getSize()) {
                blockData[i] = memory.read(memAddress);
            }
            else {
                blockData[i] = ""; // empty string for out-of-bounds
            }
        }
        line.setTag(tag);
        line.setValid(true);
        line.setData(blockData);

        seenBlocks.add(blockNumber);

        // store type of miss for later use
        if(isCompulsory)
            lastMissType = "Compulsory";
        else lastMissType = "Conflict";


        return false;

    }

    public boolean write(int address, String data) {
        int blockNumber = address / blockSize;
        int lineIndex = blockNumber % lines.length;
        int tag = blockNumber;

        CacheLine line = lines[lineIndex];
        int offset = address % blockSize; // position within the block

        if(line.isValid() && line.getTag() == tag){
            hits++;
            line.getData()[offset] = data;
            memory.write(address, data); // write-through for now
            lastMissType = "Hit";
            return true;
        }

        misses++;
        boolean isCompulsory = !seenBlocks.contains(blockNumber);

        String[] blockData = new String[blockSize];
        for(int i = 0; i < blockSize; i++){
            int memAddress = blockNumber * blockSize + i;
            if(memAddress < memory.getSize()) {
                blockData[i] = memory.read(memAddress);
            } else {
                blockData[i] = "";
            }
        }

        line.setTag(tag);
        line.setValid(true);
        line.setData(blockData);


        line.getData()[offset] = data;
        memory.write(address, data); // write-through

        seenBlocks.add(blockNumber);

        if(isCompulsory)
            lastMissType = "Compulsory";
        else
            lastMissType = "Conflict";

        return false;
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

    public String getLastMissType(){
        return lastMissType;
    }
}
