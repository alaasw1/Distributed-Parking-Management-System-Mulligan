package com.example.peo;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class ParkingEnforcementOfficer extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        //replace "Parking_Enforcement_Officer.fxml" with the correct FXML file name if different
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("Parking_Enforcement_Officer.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 600, 400);
        stage.setTitle("Parking Enforcement Officer Application");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}