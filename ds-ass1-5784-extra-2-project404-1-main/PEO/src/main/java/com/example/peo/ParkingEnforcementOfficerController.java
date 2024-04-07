package com.example.peo;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ParkingEnforcementOfficerController {
    public Button loadServersButton;
    @FXML
    private TextField vehicleNumberInput;
    @FXML
    private TextField parkingSpaceInput;
    @FXML
    private TextArea outputArea;


    @FXML
    private ComboBox<String> serverIPDropdown;
    @FXML
    private ComboBox<Integer> serverPortDropdown;
    private List<String> serversIPAddressAndPorts;
    private static boolean whileNotConncectedToServerFlag = true;




    @FXML
    private void checkVehicle() {
        String vehicleID = vehicleNumberInput.getText();
        String parkingSpaceNumber = parkingSpaceInput.getText();

        if (vehicleID.isEmpty() || parkingSpaceNumber.isEmpty()) {
            outputArea.appendText("Please enter both vehicle number and parking space ID.\n");
            return;
        }


        String selectedIP = serverIPDropdown.getSelectionModel().getSelectedItem();
        int selectedPort = serverPortDropdown.getSelectionModel().getSelectedItem();

        PEOClient peoClient = new PEOClient(selectedIP, selectedPort);

        String checkVehicleResult = peoClient.investigatingParkedVehicle(vehicleID,parkingSpaceNumber);
        if(checkVehicleResult.equals("server not connected\n")) {
            new Thread(() -> {
                Platform.runLater(() -> outputArea.appendText("server: " + selectedIP + ":" + selectedPort + " not connected\n"));
                whileNotConncectedToServerFlag = true;
                while (whileNotConncectedToServerFlag) {
                    for (String serverIpAddressAndPort : this.serversIPAddressAndPorts) {
                        if (!whileNotConncectedToServerFlag)
                            break;
                        String[] parts = serverIpAddressAndPort.split(" ");
                        String newServerIP;
                        int newServerPort;
                        if (parts.length == 2) {
                            newServerIP = parts[0];
                            newServerPort = Integer.parseInt(parts[1]);
                            Platform.runLater(() -> {
                                serverIPDropdown.setValue(newServerIP);
                                serverPortDropdown.setValue(newServerPort);
                            });
                            PEOClient newPeoClient = new PEOClient(newServerIP, newServerPort);

                            String newCheckVehicleResult = newPeoClient.investigatingParkedVehicle(vehicleID,parkingSpaceNumber);
                            if (newCheckVehicleResult.equals("vehicleID =" + vehicleID + " in parkingSpaceNumber ="+ parkingSpaceNumber + " is not OK\n")) {
                                Platform.runLater(() -> outputArea.appendText("vehicleID =" + vehicleID + " in parkingSpaceNumber ="+ parkingSpaceNumber + " is not OK\n"));
                                whileNotConncectedToServerFlag = false;
                            } else if (newCheckVehicleResult.equals("vehicleID =" + vehicleID + " in parkingSpaceNumber ="+ parkingSpaceNumber + " is OK\n")) {
                                Platform.runLater(() -> outputArea.appendText("vehicleID =" + vehicleID + " in parkingSpaceNumber ="+ parkingSpaceNumber + " is OK\n"));
                                whileNotConncectedToServerFlag = false;
                            } else if (newCheckVehicleResult.equals("server not connected\n")) {
                                Platform.runLater(() -> outputArea.appendText("server: " + newServerIP + ":" + newServerPort + " not connected\n"));
                            }else if (newCheckVehicleResult.equals("Invalid VehicleID.\n")) {
                                Platform.runLater(() -> outputArea.appendText("Invalid VehicleID.\n"));
                                whileNotConncectedToServerFlag = false;
                            } else if (newCheckVehicleResult.equals("Invalid parkingSpaceNumber.\n")) {
                                Platform.runLater(() -> outputArea.appendText("Invalid parkingSpaceNumber.\n"));
                                whileNotConncectedToServerFlag = false;
                            }else{
                                Platform.runLater(() -> outputArea.appendText("Server communication error.\n"));
                                whileNotConncectedToServerFlag = false;
                            }
                        }
                        //we make thread sleep for 1 sec to not spam the chat with server not connected
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }


                    }
                }
            }).start();
        }
        else {
            outputArea.appendText(checkVehicleResult);
        }
    }

    public void handleLoadServers(ActionEvent actionEvent) {
        //we open a file chooser dialog to select a txt file
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Text File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text Files", "*.txt"),  // Change this line to target .txt files
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        Stage stage = (Stage) loadServersButton.getScene().getWindow();
        //we show the open dialog in the current stage
        File selectedFile = fileChooser.showOpenDialog(stage);
        //we check if a file was selected
        if (selectedFile != null) {
            try {
                //we read all lines from the selected file
                List<String> lines = Files.readAllLines(selectedFile.toPath());
                this.serversIPAddressAndPorts = lines;
                //we clear existing items and add new lines to the ComboBox from txt file
                serverIPDropdown.getItems().clear();
                serverPortDropdown.getItems().clear();
                //we use Hashset collection that does not allow duplicate elements so we make IP addresses and port numbers unique
                Set<String> uniqueIPAddresses = new HashSet<>();
                Set<Integer> uniquePortNumbers = new HashSet<>();

                for (String line : lines) {
                    String[] parts = line.split(" ");
                    if (parts.length == 2) {
                        uniqueIPAddresses.add(parts[0]);
                        uniquePortNumbers.add(Integer.parseInt(parts[1]));
                    }
                }
                serverIPDropdown.getItems().addAll(uniqueIPAddresses);
                serverPortDropdown.getItems().addAll(uniquePortNumbers);

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public void handleOnShowingPickServerIpComboBox(Event event) {
        whileNotConncectedToServerFlag = false;
    }

    public void handleOnShowingPickServerPortComboBox(Event event) {
        whileNotConncectedToServerFlag = false;

    }
}