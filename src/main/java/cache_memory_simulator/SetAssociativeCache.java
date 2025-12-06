package cache_memory_simulator;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class SetAssociativeCache implements CacheMemory {
    private CacheLine[] lines;
    private Memory memory;
    private int blockSize;
    private int associativity; // K
    private int numSets;
    private ReplacementPolicy replacementPolicy;
    private WritePolicy writePolicy;
    private String lastEvictionMessage = "";

    private int hits = 0;
    private int misses = 0;
    private String lastMissType;

    private Set<Integer> seenBlocks = new HashSet<>();

    // To implement LRU and FIFO, we need to track usage/insertion
    private long[] usageTimestamps;
    private long operationCounter = 0;

    private int lastBlockNumber;
    private int lastSetIndex;
    private int lastTag;

    public SetAssociativeCache(int size, int blockSize, int associativity, Memory memory, ReplacementPolicy replacementPolicy, WritePolicy writePolicy) {
        this.blockSize = blockSize;
        this.memory = memory;
        this.associativity = associativity;
        this.replacementPolicy = replacementPolicy;
        this.writePolicy = writePolicy;

        // Calculate number of sets
        this.lines = new CacheLine[size];
        this.usageTimestamps = new long[size];

        // Example: 8 lines, 2-way => 4 sets.
        this.numSets = size / associativity;

        for (int i = 0; i < size; i++) {
            lines[i] = new CacheLine(i, blockSize);
            usageTimestamps[i] = 0;
        }
    }

    @Override
    public boolean read(int address) {
        this.lastBlockNumber = address / blockSize;
        this.lastSetIndex = lastBlockNumber % numSets;
        this.lastTag = lastBlockNumber / numSets;

        // The lines for a set range from [setIndex * K] to [setIndex * K + K - 1]
        int startIndex = lastSetIndex * associativity;
        int endIndex = startIndex + associativity;
        for (int i = startIndex; i < endIndex; i++) {
            CacheLine line = lines[i];
            if (line.isValid() && line.getTag() == lastTag) {
                hits++;
                // Update usage for LRU (Touched now)
                if (replacementPolicy == ReplacementPolicy.LRU)
                    usageTimestamps[i] = ++operationCounter;
                return true;
            }
        }
        misses++;
        handleMiss(address, lastBlockNumber, lastTag, startIndex, endIndex);
        return false;
    }

    @Override
    public boolean write(int address, String data) {
        this.lastEvictionMessage = "";
        this.lastBlockNumber = address / blockSize;
        this.lastSetIndex = lastBlockNumber % numSets;
        this.lastTag = lastBlockNumber / numSets;
        int offset = address % blockSize;

        int startIndex = lastSetIndex * associativity;
        int endIndex = startIndex + associativity;

        for (int i = startIndex; i < endIndex; i++) {
            CacheLine line = lines[i];
            if (line.isValid() && line.getTag() == lastTag) {
                hits++;
                // Write Through policy (update cache and memory)
                line.getData()[offset] = data;
                if (writePolicy == WritePolicy.WRITE_THROUGH)
                    memory.write(address, data);
                else
                    line.setDirty(true);

                lastMissType = "Hit";
                if (replacementPolicy == ReplacementPolicy.LRU)
                    usageTimestamps[i] = ++operationCounter;
                return true;

            }
        }

        misses++;
        // Fetch block first
        int lineIndex = handleMiss(address, lastBlockNumber, lastTag, startIndex, endIndex);

        // Perform the write on the newly loaded line
        lines[lineIndex].getData()[offset] = data;
        return false;
    }

    // Helper function to handle fetching from memory and eviction logic
    private int handleMiss(int address, int blockNumber, int tag, int startIndex, int endIndex) {
        boolean isCompulsory = !seenBlocks.contains(blockNumber);
        seenBlocks.add(blockNumber);

        if (isCompulsory)
            lastMissType = "Compulsory";
        else
            lastMissType = "Conflict";

        int victimIndex = findVictimIndex(startIndex, endIndex);
        CacheLine line = lines[victimIndex];

        if (line.isValid() && line.isDirty())
            flushDirtyLineToMemory(line, victimIndex);


        String[] blockData = new String[blockSize];
        for (int i = 0; i < blockSize; i++) {
            int memAddress = blockNumber * blockSize + i;
            if (memAddress < memory.getSize())
                blockData[i] = memory.read(memAddress);
            else
                blockData[i] = "";
        }

        line.setTag(tag);
        line.setValid(true);
        line.setDirty(false);
        line.setData(blockData);

        // Update timestamps for LRU or FIFO
        // For LRU: Used now. For FIFO: Inserted now.
        usageTimestamps[victimIndex] = ++operationCounter;
        return victimIndex;
    }

    private int findVictimIndex(int startIndex, int endIndex) {
        // First, look for an empty (invalid) line
        for (int i = startIndex; i < endIndex; i++) {
            if (!lines[i].isValid())
                return i;
        }

        // If set is full, use Replacement Policy
        if (replacementPolicy == ReplacementPolicy.RANDOM) {
            Random rand = new Random();
            return startIndex + rand.nextInt(associativity);
        }

        // Logic for LRU and FIFO is identical here: find the minimum timestamp
        // Differences are handled when we UPDATE the timestamp ( Hit vs Miss)
        int victimIndex = startIndex;
        long minTime = usageTimestamps[victimIndex];

        for (int i = startIndex + 1; i < endIndex; i++) {
            if (usageTimestamps[i] < minTime) {
                minTime = usageTimestamps[i];
                victimIndex = i;
            }
        }
        return victimIndex;
    }

    private void flushDirtyLineToMemory(CacheLine line, int index) {
        int setIndex = index / associativity;
        int oldBlockNumber = (line.getTag() * numSets) + setIndex;
        int oldBaseAddress = oldBlockNumber * blockSize;

        String[] data = line.getData();
        for (int i = 0; i < data.length; i++) {
            memory.write(oldBaseAddress + i, data[i]);
        }
        this.lastEvictionMessage = "Evicted dirty block " + oldBlockNumber + " to memory and" + "wrote to address " + oldBaseAddress;
    }

    @Override
    public String getLastAccessDetails() {
        return String.format("[Block: %d | Set: %d | Tag: %d]", lastBlockNumber, lastSetIndex, lastTag);
    }


    @Override
    public CacheLine[] getLines() {
        return lines;
    }

    @Override
    public int getHits() {
        return hits;
    }

    @Override
    public int getMisses() {
        return misses;
    }

    public String getLastMissType() {
        return lastMissType;
    }

    @Override
    public int getBlockSize() {
        return blockSize;
    }

    @Override
    public String getLastEvictionMessage() {
        return lastEvictionMessage;
    }
}
