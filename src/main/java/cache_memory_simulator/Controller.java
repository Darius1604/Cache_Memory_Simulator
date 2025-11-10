package cache_memory_simulator;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class Controller {

    @FXML private TextField cacheSizeField;
    @FXML private TextField blockSizeField;
    @FXML private TextField addressField;
    @FXML private TextField writeDataField;
    @FXML private Label hitLabel;
    @FXML private Label missLabel;
    @FXML private Label hitRatioLabel;

    @FXML private TextArea logArea;

    @FXML private TableView<CacheLine> cacheTable;
    @FXML private TableView<MemoryCell> memoryTable;

    @FXML private RadioButton directMappedRadio;
    @FXML private RadioButton setAssociativeRadio;
    @FXML private RadioButton fullyAssociativeRadio;
    private ToggleGroup cacheTypeGroup;

    @FXML private RadioButton writeBackRadio;
    @FXML private RadioButton writeThroughRadio;
    private ToggleGroup writePoliciesGroup;

    @FXML private RadioButton lruRadio;
    @FXML private RadioButton fifoRadio;
    @FXML private RadioButton randomRadio;
    private ToggleGroup replacementPoliciesGroup;

    private Memory memory;
    private DirectMappedCache cache;

    @FXML
    private void initialize() {
        cacheTypeGroup = new ToggleGroup();
        directMappedRadio.setToggleGroup(cacheTypeGroup);
        setAssociativeRadio.setToggleGroup(cacheTypeGroup);
        fullyAssociativeRadio.setToggleGroup(cacheTypeGroup);
        directMappedRadio.setSelected(true);

        writePoliciesGroup = new ToggleGroup();
        writeBackRadio.setToggleGroup(writePoliciesGroup);
        writeThroughRadio.setToggleGroup(writePoliciesGroup);
        writeBackRadio.setSelected(true);

        replacementPoliciesGroup = new ToggleGroup();
        lruRadio.setToggleGroup(replacementPoliciesGroup);
        fifoRadio.setToggleGroup(replacementPoliciesGroup);
        randomRadio.setToggleGroup(replacementPoliciesGroup);
        lruRadio.setSelected(true);


        initializeMemoryTable();
        memory = new Memory(256);
        memoryTable.setItems(memory.getMemoryCells());

        TableColumn<CacheLine, String> lineCol = new TableColumn<>("Line");
        lineCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.valueOf(cellData.getValue().getLineIndex()))
        );

        TableColumn<CacheLine, String> validCol = new TableColumn<>("Valid");
        validCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.valueOf(cellData.getValue().isValid()))
        );

        TableColumn<CacheLine, String> tagCol = new TableColumn<>("Tag");
        tagCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.valueOf(cellData.getValue().getTag()))
        );

        TableColumn<CacheLine, String> dataCol = new TableColumn<>("Data");
        dataCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.valueOf(cellData.getValue().getDataAsString())));

        cacheTable.getColumns().addAll(lineCol, validCol, tagCol, dataCol);
        cacheTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void initializeMemoryTable() {
        TableColumn<MemoryCell, String> addressCol = new TableColumn<>("Address");
        addressCol.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().getAddress())));
        TableColumn<MemoryCell, String> dataCol = new TableColumn<>("Data");
        dataCol.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().getData())));
        memoryTable.getColumns().addAll(addressCol, dataCol);
        memoryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    @FXML
    private void initializeCache(){
        logArea.clear();
        int cacheSize = getCacheSize();
        int blockSize = getBlockSize();

        if (cache == null || cache.getLines().length != cacheSize || cache.getBlockSize() != blockSize) {
            cache = new DirectMappedCache(cacheSize, blockSize, memory);
            ObservableList<CacheLine> cacheLines = FXCollections.observableArrayList(cache.getLines());
            cacheTable.setItems(cacheLines);}
        cacheTable.refresh();

    }

    @FXML
    private void handleWrite(){
        int address = Integer.parseInt(addressField.getText());
        String data = writeDataField.getText();
        boolean hit = cache.write(address, data);

        cacheTable.refresh();
        memoryTable.refresh(); // update memory table
        updateCacheStats();
        cacheTable.refresh();
        memoryTable.refresh();

        if(hit)
            log("✅ HIT: Wrote data '" + data + "' to address " + address + " in cache (and memory).");
        else
            log("❌ MISS: Address " + address + " not in cache. Loaded block and wrote data '" + data + "'.");
    }

    @FXML
    private void handleRead(){
        int address = Integer.parseInt(addressField.getText());
        boolean hit = cache.read(address);
        cacheTable.refresh();
        memoryTable.refresh();
        updateCacheStats();
        cacheTable.refresh();
        memoryTable.refresh();

        if(hit){
            log("✅ HIT: Read from address " + address);
        }
        else {
            String missType = cache.getLastMissType();
            log("❌ MISS ("+ missType + "): Loaded block for address " + address + " from memory(block " +
                    (address / cache.getBlockSize()) + ").");
        }
    }

    private int getCacheSize() {
        return Integer.parseInt(cacheSizeField.getText());
    }

    private int getBlockSize() {
        return Integer.parseInt(blockSizeField.getText());
    }

    @FXML
    private void log(String message){
        logArea.appendText(message + "\n");
    }

    private void updateCacheStats(){
        int totalAccesses = cache.getHits() + cache.getMisses();

        hitLabel.setText("HITS: " + cache.getHits());
        missLabel.setText("MISSES: " + cache.getMisses());

        hitLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
        missLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");

        double hitRatioPercent = 0;
        if (totalAccesses > 0) {
            hitRatioPercent = (double) cache.getHits() / totalAccesses * 100;
        }
        else hitRatioPercent = 0;
        hitRatioLabel.setText(String.format("HIT RATIO: %.2f%%", hitRatioPercent));
        hitRatioLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");

    }
}



// Add a memory block with random numbers to make the simulation more realistic
// Store copies of blocks of memory to make it more precise
// Implement Direct-Mapped, Set-Associative and Fully-Associative