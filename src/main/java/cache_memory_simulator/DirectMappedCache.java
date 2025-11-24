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
    private WritePolicy writePolicy;
    private String lastEvictionMessage = "";


    public DirectMappedCache(int size, int blockSize, Memory memory, WritePolicy writePolicy) {
        lines = new CacheLine[size];
        for (int i = 0; i < size; i++)
            lines[i] = new CacheLine(i, blockSize);
        this.blockSize = blockSize;
        this.memory = memory;
        this.writePolicy = writePolicy;
    }

    public boolean read(int address) {
        int blockNumber = address / blockSize;
        int lineIndex = (address / blockSize) % lines.length;
        int tag = blockNumber / lines.length;

        CacheLine line = lines[lineIndex];
        int offset = address % blockSize; // position within the block
        if (line.isValid() && line.getTag() == tag) {
            hits++;
            return true;
        }

        // DirectMappedCache miss - fetch entire block from memory
        misses++;
        // Check if the current occupant is dirty before kicking it out
        if (line.isValid() && line.isDirty()) {
            flushDirtyLine(line, lineIndex);
        }
        boolean isCompulsory = !seenBlocks.contains(blockNumber);

        String[] blockData = new String[blockSize];
        for (int i = 0; i < blockSize; i++) {
            int memAddress = blockNumber * blockSize + i;
            if (memAddress < memory.getSize()) {
                blockData[i] = memory.read(memAddress);
            } else {
                blockData[i] = ""; // empty string for out-of-bounds
            }
        }
        line.setTag(tag);
        line.setValid(true);
        line.setDirty(false); // New data is clean (matches memory)
        line.setData(blockData);

        seenBlocks.add(blockNumber);

        // store type of miss for later use
        if (isCompulsory)
            lastMissType = "Compulsory";
        else lastMissType = "Conflict";


        return false;

    }

    public boolean write(int address, String data) {
        this.lastEvictionMessage = "";
        int blockNumber = address / blockSize;
        int lineIndex = (address / blockSize) % lines.length;
        int tag = blockNumber / lines.length;

        CacheLine line = lines[lineIndex];
        int offset = address % blockSize;

        if (line.isValid() && line.getTag() == tag) {
            hits++;
            line.getData()[offset] = data;

            // Check policy
            if (writePolicy == WritePolicy.WRITE_THROUGH)
                memory.write(address, data); // Write immediately
            else
                line.setDirty(true);
            lastMissType = "Hit";
            return true;
        }

        misses++;

        // Evict old dirty line if necessary
        if (line.isValid() && line.isDirty())
            flushDirtyLine(line, lineIndex);

        boolean isCompulsory = !seenBlocks.contains(blockNumber);

        String[] blockData = new String[blockSize];
        for (int i = 0; i < blockSize; i++) {
            int memAddress = blockNumber * blockSize + i;
            if (memAddress < memory.getSize()) {
                blockData[i] = memory.read(memAddress);
            } else {
                blockData[i] = "";
            }
        }

        line.setTag(tag);
        line.setValid(true);
        line.setData(blockData);

        line.getData()[offset] = data;
        if (writePolicy == WritePolicy.WRITE_THROUGH)
            memory.write(address, data);
        else
            line.setDirty(true); // Modified the loaded block, but haven't sent to memory yet

        seenBlocks.add(blockNumber);
        if (isCompulsory)
            lastMissType = "Compulsory";
        else
            lastMissType = "Conflict";

        return false;
    }


    public CacheLine[] getLines() {
        return lines;
    }

    public int getHits() {
        return hits;
    }

    public int getMisses() {
        return misses;
    }

    public int getBlockSize() {
        return blockSize;
    }

    public String getLastMissType() {
        return lastMissType;
    }

    private void flushDirtyLine(CacheLine line, int lineIndex) {
        int oldBlockNumber = (line.getTag() * lines.length) + lineIndex;
        int oldBaseAddress = oldBlockNumber * blockSize;

        // Write the entire block back to memory
        String[] data = line.getData();
        for (int i = 0; i < blockSize; i++) {
            if (oldBaseAddress + i < memory.getSize()) {
                memory.write(oldBaseAddress + i, data[i]);
            }
        }
        this.lastEvictionMessage = "Write-Back: Evicted dirty  block " + oldBlockNumber + " to memory and wrote to address " + oldBaseAddress;
    }

    @Override
    public String getLastEvictionMessage() {
        return lastEvictionMessage;
    }
}

