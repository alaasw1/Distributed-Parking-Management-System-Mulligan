module com.example.server {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.bootstrapfx.core;
    requires org.mongodb.driver.sync.client;
    requires org.mongodb.bson;
    requires org.mongodb.driver.core;
    requires com.fasterxml.jackson.databind;
    requires com.google.gson;
    requires java.logging;

    opens com.example.server to javafx.fxml;
    exports com.example.server;
}