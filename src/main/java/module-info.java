module cache_memory_simulator {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires eu.hansolo.tilesfx;

    opens cache_memory_simulator to javafx.fxml;
    exports cache_memory_simulator;
}