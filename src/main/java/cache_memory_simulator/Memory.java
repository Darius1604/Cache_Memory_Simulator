package cache_memory_simulator;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.Random;

public class Memory {
    private String[] memory;
    private ObservableList<MemoryCell> memoryCells;

    public Memory(int size) {
        memory = new String[size];
        memoryCells = FXCollections.observableArrayList();
        Random rand = new Random();
        for (int i=0; i<size; i++) {
            int value = rand.nextInt(100); // random number 0-99
            memory[i] = String.valueOf(value);
            memoryCells.add(new MemoryCell(i, memory[i]));
        }
    }

    public String read(int address){
        return memory[address];
    }
    public void write(int address, String data){
        memory[address] = data;
        memoryCells.get(address).setData(data); // update table view
    }

    public ObservableList<MemoryCell> getMemoryCells() {
        return memoryCells;
    }

    public int getSize() {
        return memory.length;
    }
}
