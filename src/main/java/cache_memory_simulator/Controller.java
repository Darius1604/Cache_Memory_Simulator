package cache_memory_simulator;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.TableRow;

public class Controller {
    @FXML
    private TextField cacheSizeField;
    @FXML
    private TextField blockSizeField;
    @FXML
    private TextField addressField;
    @FXML
    private TextField writeDataField;
    @FXML
    private Label hitLabel;
    @FXML
    private Label missLabel;
    @FXML
    private Label hitRatioLabel;
    @FXML
    private Slider kSlider;

    @FXML
    private Label kLabel;

    @FXML
    private TextArea logArea;

    @FXML
    private TableView<CacheLine> cacheTable;
    @FXML
    private TableView<MemoryCell> memoryTable;

    @FXML
    private RadioButton directMappedRadio;
    @FXML
    private RadioButton setAssociativeRadio;
    @FXML
    private RadioButton fullyAssociativeRadio;
    @FXML
    private ToggleGroup cacheTypeGroup;

    @FXML
    private RadioButton writeBackRadio;
    @FXML
    private RadioButton writeThroughRadio;
    @FXML
    private ToggleGroup writePoliciesGroup;

    @FXML
    private RadioButton lruRadio;
    @FXML
    private RadioButton fifoRadio;
    @FXML
    private RadioButton randomRadio;
    @FXML
    private ToggleGroup replacementPoliciesGroup;

    @FXML
    public ComboBox<Integer> memorySizeBox;

    private TableColumn<CacheLine, String> setCol;

    private Memory memory;
    private CacheMemory cache;


    @FXML
    private void initialize() {
        memorySizeBox.getItems().addAll(256, 512, 1024, 2048);
        memorySizeBox.getSelectionModel().selectFirst();

        cacheTypeGroup = new ToggleGroup();
        directMappedRadio.setToggleGroup(cacheTypeGroup);
        setAssociativeRadio.setToggleGroup(cacheTypeGroup);
        fullyAssociativeRadio.setToggleGroup(cacheTypeGroup);
        directMappedRadio.setSelected(true);

        writePoliciesGroup = new ToggleGroup();
        writeBackRadio.setToggleGroup(writePoliciesGroup);
        writeThroughRadio.setToggleGroup(writePoliciesGroup);
        writeThroughRadio.setSelected(true);

        replacementPoliciesGroup = new ToggleGroup();
        lruRadio.setToggleGroup(replacementPoliciesGroup);
        fifoRadio.setToggleGroup(replacementPoliciesGroup);
        randomRadio.setToggleGroup(replacementPoliciesGroup);
        lruRadio.setSelected(true);

        setCol = new TableColumn<>("Set");
        setCol.setCellValueFactory(cellData -> {
            int lineIndex = cellData.getValue().getLineIndex();

            // Dynamic K calculation
            int k = 1;
            if (setAssociativeRadio.isSelected()) {
                k = (int) kSlider.getValue();
            } else if (fullyAssociativeRadio.isSelected()) {
                k = getCacheSize();
            }

            return new SimpleStringProperty(String.valueOf(lineIndex / k));
        });

        kLabel.setText("k = " + (int) kSlider.getValue());

        // Add listener
        kSlider.valueProperty().addListener((obs, oldValue, newValue) -> {
            kLabel.setText("k = " + newValue.intValue());
        });


        initializeMemoryTable();
        memory = new Memory(memorySizeBox.getValue());
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

        TableColumn<CacheLine, String> dirtyCol = new TableColumn<>("Dirty");
        dirtyCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.valueOf(cellData.getValue().isDirty())));

        cacheTable.getColumns().addAll(lineCol, validCol, tagCol, dataCol, dirtyCol);
        cacheTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        setupTableColoring();
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
    private void initializeCache() {
        logArea.clear();


        int selectedMemorySize = memorySizeBox.getValue();
        memory = new Memory(selectedMemorySize);

        memoryTable.setItems(memory.getMemoryCells());
        memoryTable.refresh();

        log("System Initialized: Memory Size " + selectedMemorySize);

        int cacheSize = getCacheSize();
        int blockSize = getBlockSize();

        ReplacementPolicy policy = getReplacementPolicy();

        WritePolicy writePolicy = WritePolicy.WRITE_THROUGH; // default
        if (writeBackRadio.isSelected())
            writePolicy = WritePolicy.WRITE_BACK;

        if (directMappedRadio.isSelected()) {
            cache = new DirectMappedCache(cacheSize, blockSize, memory, writePolicy);
            log("Initialized Direct Mapped Cache (" + writePolicy + ")");
        } else if (setAssociativeRadio.isSelected()) {
            int k = (int) kSlider.getValue();
            cache = new SetAssociativeCache(cacheSize, blockSize, k, memory, policy, writePolicy);
            log("Initialized " + k + "-Way Set Associative Cache (" + policy + ") (" + writePolicy + ").");
        } else if (fullyAssociativeRadio.isSelected()) {
            // Fully Associative is just Set Associative where K = CacheSize
            // and NumSets = 1
            cache = new SetAssociativeCache(cacheSize, blockSize, cacheSize, memory, policy, writePolicy);
            log("Initialized Fully Associative Cache (" + policy + ") (" + writePolicy + ").");
        }

        boolean isSetAssociative = setAssociativeRadio.isSelected();
        boolean columnExists = cacheTable.getColumns().contains(setCol);

        if (isSetAssociative && !columnExists) {
            cacheTable.getColumns().add(1, setCol);
        } else if (!isSetAssociative && columnExists) {
            cacheTable.getColumns().remove(setCol);
        }

        ObservableList<CacheLine> cacheLines = FXCollections.observableArrayList(cache.getLines());
        cacheTable.setItems(cacheLines);
        cacheTable.refresh();

    }

    @FXML
    private void handleWrite() {
        try {
            int address = Integer.parseInt(addressField.getText());
            String data = writeDataField.getText();
            boolean isWriteBack = writeBackRadio.isSelected();

            boolean hit = cache.write(address, data);

            cacheTable.refresh();
            memoryTable.refresh();
            updateCacheStats();

            String locationDetails = cache.getLastAccessDetails();

            if (hit) {
                log("✅ HIT: Wrote '" + data + "' to " + address + ". " + locationDetails);
                if (isWriteBack) log("       -> Cache marked DIRTY. Memory NOT updated.");
                else log("       -> Cache & Memory updated (Write-Through).");
            } else {
                log("❌ MISS: Address " + address + " " + locationDetails);
                log("       -> Loaded block. " + (isWriteBack ? "Marked DIRTY." : "Updated Memory."));
            }

            String evictionMsg = cache.getLastEvictionMessage();
            if (evictionMsg != null && !evictionMsg.isEmpty()) {
                log("       [!] " + evictionMsg);
            }
            log("------------------------------------------------------");

        } catch (NumberFormatException e) {
            log("[!] Error: Please enter valid numbers for Address.");
        }
    }

    @FXML
    private void handleRead() {
        try {
            int address = Integer.parseInt(addressField.getText());
            boolean hit = cache.read(address);

            cacheTable.refresh();
            memoryTable.refresh();
            updateCacheStats();

            cacheTable.refresh();
            memoryTable.refresh();

            String locationDetails = cache.getLastAccessDetails();

            if (hit) {
                log("✅ HIT: Address " + address + " found. " + locationDetails);
            } else {
                String missType = cache.getLastMissType();
                log("❌ MISS (" + missType + "): Address " + address + " not found. " + locationDetails);
                log("       -> Loaded Block " + (address / cache.getBlockSize()) + " from memory.");
            }
            String evictionMsg = cache.getLastEvictionMessage();
            if (evictionMsg != null && !evictionMsg.isEmpty()) {
                log("       [!]  " + evictionMsg);
            }
            log("------------------------------------------------------");

        } catch (NumberFormatException e) {
            log("[!] Error: Please enter a valid number for Address.");
        }
    }

    private int getCacheSize() {
        return Integer.parseInt(cacheSizeField.getText());
    }

    private int getBlockSize() {
        return Integer.parseInt(blockSizeField.getText());
    }

    @FXML
    private void log(String message) {
        logArea.appendText(message + "\n");
    }

    private void updateCacheStats() {
        int totalAccesses = cache.getHits() + cache.getMisses();

        hitLabel.setText("HITS: " + cache.getHits());
        missLabel.setText("MISSES: " + cache.getMisses());

        hitLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
        missLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");

        double hitRatioPercent = 0;
        if (totalAccesses > 0) {
            hitRatioPercent = (double) cache.getHits() / totalAccesses * 100;
        } else hitRatioPercent = 0;
        hitRatioLabel.setText(String.format("HIT RATIO: %.2f%%", hitRatioPercent));
        hitRatioLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");

    }

    private ReplacementPolicy getReplacementPolicy() {
        if (lruRadio.isSelected()) return ReplacementPolicy.LRU;
        if (fifoRadio.isSelected()) return ReplacementPolicy.FIFO;
        return ReplacementPolicy.RANDOM;
    }

    private void setupTableColoring() {
        cacheTable.setRowFactory(tv -> {
            return new TableRow<CacheLine>() {
                @Override
                protected void updateItem(CacheLine item, boolean empty) {
                    super.updateItem(item, empty);
                    getStyleClass().removeAll("set-group-0", "set-group-1", "set-group-2", "set-group-3", "end-of-set");

                    if (item == null || empty) {
                        return;
                    }

                    if (directMappedRadio.isSelected()) {
                        return;
                    }

                    int k = 1;
                    if (setAssociativeRadio.isSelected()) {
                        k = (int) kSlider.getValue();
                    } else if (fullyAssociativeRadio.isSelected()) {
                        getStyleClass().add("set-group-0");
                        return;
                    }

                    int lineIndex = item.getLineIndex();
                    int setIndex = lineIndex / k;
                    int positionInSet = lineIndex % k;

                    int colorGroup = setIndex % 4;
                    getStyleClass().add("set-group-" + colorGroup);

                    if (positionInSet == k - 1) {
                        getStyleClass().add("end-of-set");
                    }
                }
            };
        });
    }


}

// sa adaug sa citeasca un file cu READ <adresa>, WRITE <adresa>, <valoarea>