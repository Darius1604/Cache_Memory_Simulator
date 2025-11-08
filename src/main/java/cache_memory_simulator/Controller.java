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
    @FXML private Label hitLabel;
    @FXML private Label missLabel;

    @FXML private TableView<CacheLine> cacheTable;

    private ObservableList<CacheLine> cacheLines;

    private Cache cache;

    @FXML
    private void initialize() {
        TableColumn<CacheLine, String> lineCol = new TableColumn<>("Line");
        lineCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.valueOf(cellData.getValue().getLineIndex() + 1))
        );

        TableColumn<CacheLine, String> validCol = new TableColumn<>("Valid");
        validCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.valueOf(cellData.getValue().isValid()))
        );

        TableColumn<CacheLine, String> tagCol = new TableColumn<>("Tag");
        tagCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.valueOf(cellData.getValue().getTag()))
        );

        cacheTable.getColumns().addAll(lineCol, validCol, tagCol);
        cacheTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    @FXML
    private void initializeCache(){
        int cacheSize = getCacheSize();
        int blockSize = getBlockSize();

        if (cache == null || cache.getLines().length != cacheSize){
            cache = new Cache(cacheSize, blockSize);
            cacheLines =  FXCollections.observableArrayList(cache.getLines());
            cacheTable.setItems(cacheLines);}
        cacheTable.refresh();
    }

    @FXML
    private void handleAccess() {
        int address = Integer.parseInt(addressField.getText());
        boolean hit = cache.access(address, cache.getBlockSize());

        cacheTable.refresh();

        hitLabel.setText("Hits: " + cache.getHits());
        missLabel.setText("Misses: " + cache.getMisses());
    }

    private int getCacheSize() {
        return Integer.parseInt(cacheSizeField.getText());
    }

    private int getBlockSize() {
        return Integer.parseInt(blockSizeField.getText());
    }
}

// Add a memory block with random numbers to make the simulation more realistic
// Store copies of blocks of memory to make it more precise
// Implement Direct-Mapped, Set-Associative and Fully-Associative