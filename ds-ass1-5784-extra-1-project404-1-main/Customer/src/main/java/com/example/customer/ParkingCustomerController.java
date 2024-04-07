package com.example.customer;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class ParkingCustomerController {
    public Button submitVehicleIdButton;
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

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    private List<String> serversIPAddressAndPorts;
    private static boolean whileNotConncectedToServerFlag = true;


    @FXML
    private void handleSubmitVehicleId() {
        if (!vehicleNumberInput.getText().isEmpty()) {
            parkingSpaceInput.setDisable(false); // Enable the parking space input
            submitVehicleIdButton.setDisable(true); // Optionally disable the ID submit button
        } else {
            outputArea.appendText("Vehicle ID is empty\n");
        }
    }

    @FXML
    private void initialize() {
        parkingSpaceInput.setDisable(true); // Disable at start


    }

    @FXML
    private void startParking() {
        // Ensure that vehicle number and parking space ID are entered
        String vehicleID = vehicleNumberInput.getText();
        String parkingSpaceNumber = parkingSpaceInput.getText();
        String selectedIP = serverIPDropdown.getSelectionModel().getSelectedItem();
        int selectedPort = serverPortDropdown.getSelectionModel().getSelectedItem();
        ParkingClient parkingClient = new ParkingClient(selectedIP, selectedPort);

        if (vehicleID.isEmpty() || parkingSpaceNumber.isEmpty()) {
            outputArea.appendText("Please enter both vehicle number and parking space ID.\n");
            return;
        }
        String startParkingResult = parkingClient.startParking(vehicleID,parkingSpaceNumber);
        if(startParkingResult.equals("server not connected\n")) {
            new Thread(() -> {
                Platform.runLater(() -> outputArea.appendText("server: " + selectedIP + ":" + selectedPort + " not connected\n"));
                whileNotConncectedToServerFlag = true;
                while (whileNotConncectedToServerFlag) {
                    for (String serverIpAddressAndPort : this.serversIPAddressAndPorts) {
                        if (whileNotConncectedToServerFlag == false)
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
                            ParkingClient newParkingClient = new ParkingClient(newServerIP, newServerPort);

                            String newStartParkingResult = newParkingClient.startParking(vehicleID,parkingSpaceNumber);
                            if (newStartParkingResult.equals("Parking Started\n")) {
                                Platform.runLater(() -> outputArea.appendText("Parking Started\n"));
                                whileNotConncectedToServerFlag = false;
                            } else if (newStartParkingResult.equals("Server communication error.\n")) {
                                Platform.runLater(() -> outputArea.appendText("Server communication error.\n"));
                                whileNotConncectedToServerFlag = false;
                            } else if (newStartParkingResult.equals("server not connected\n")) {
                                Platform.runLater(() -> outputArea.appendText("server: " + newServerIP + ":" + newServerPort + " not connected\n"));
                            }else if (newStartParkingResult.equals("Invalid VehicleID.\n")) {
                                Platform.runLater(() -> outputArea.appendText("Invalid VehicleID.\n"));
                                whileNotConncectedToServerFlag = false;
                            } else if (newStartParkingResult.equals("Invalid parkingSpaceNumber.\n")) {
                                Platform.runLater(() -> outputArea.appendText("Invalid parkingSpaceNumber.\n"));
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
            outputArea.appendText(startParkingResult);
        }

    }

    @FXML
    private void stopParking() {
        String vehicleID = vehicleNumberInput.getText();

        String selectedIP = serverIPDropdown.getSelectionModel().getSelectedItem();
        int selectedPort = serverPortDropdown.getSelectionModel().getSelectedItem();
        ParkingClient parkingClient = new ParkingClient(selectedIP, selectedPort);
//        ParkingClient parkingClient = new ParkingClient("127.0.0.1",6661);

        String stopParkingResult = parkingClient.stopParking(vehicleID);
        if(stopParkingResult.equals("server not connected\n")) {
            new Thread(() -> {
                Platform.runLater(() -> outputArea.appendText("server: " + selectedIP + ":" + selectedPort + " not connected\n"));
                whileNotConncectedToServerFlag = true;
                while (whileNotConncectedToServerFlag) {
                    for (String serverIpAddressAndPort : this.serversIPAddressAndPorts) {
                        if (whileNotConncectedToServerFlag == false)
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
                            ParkingClient newParkingClient = new ParkingClient(newServerIP, newServerPort);

                            String newStopParkingResult = newParkingClient.stopParking(vehicleID);
                            if (newStopParkingResult.equals("Parking Stopped\n")) {
                                Platform.runLater(() -> outputArea.appendText("Parking Stopped\n"));
                                whileNotConncectedToServerFlag = false;
                            } else if (newStopParkingResult.equals("Server communication error.\n")) {
                                Platform.runLater(() -> outputArea.appendText("Server communication error.\n"));
                                whileNotConncectedToServerFlag = false;
                            } else if (newStopParkingResult.equals("server not connected\n")) {
                                Platform.runLater(() -> outputArea.appendText("server: " + newServerIP + ":" + newServerPort + " not connected\n"));
                            } else if (newStopParkingResult.equals("There is no open parking event.\n")) {
                                Platform.runLater(() -> outputArea.appendText("There is no open parking event.\n"));
                            }else if (newStopParkingResult.equals("Invalid VehicleID.\n")) {
                                Platform.runLater(() -> outputArea.appendText("Invalid VehicleID.\n"));
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
            outputArea.appendText(stopParkingResult);
        }

    }

    @FXML
    private void viewParkingHistory() {
        String vehicleID = vehicleNumberInput.getText();

        String selectedIP = serverIPDropdown.getSelectionModel().getSelectedItem();
        int selectedPort = serverPortDropdown.getSelectionModel().getSelectedItem();
        ParkingClient parkingClient = new ParkingClient(selectedIP, selectedPort);

        String getParkingEventsListResult = parkingClient.retrievingListOfParkingEvents(vehicleID);
        if(getParkingEventsListResult.equals("error"))
            outputArea.appendText("Server communication error.\n");
        else if(getParkingEventsListResult.equals("serverNotConnected")) {
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
                            ParkingClient newParkingClient = new ParkingClient(newServerIP, newServerPort);

                            String getNewParkingEventsListResult = newParkingClient.retrievingListOfParkingEvents(vehicleID);
                            if (getNewParkingEventsListResult.equals("error")) {
                                Platform.runLater(() -> outputArea.appendText("Server communication error.\n"));
                                whileNotConncectedToServerFlag = false;
                            } else if (getNewParkingEventsListResult.equals("serverNotConnected")) {
                                Platform.runLater(() -> outputArea.appendText("server: " + newServerIP + ":" + newServerPort + " not connected\n"));
                            } else if (getNewParkingEventsListResult.equals("invalidVehicleID")) {
                                Platform.runLater(() -> outputArea.appendText("Invalid VehicleID.\n"));
                                whileNotConncectedToServerFlag = false;
                            } else if (getNewParkingEventsListResult.equals("sendParkingEventsListIsEmpty")) {
                                Platform.runLater(() -> {
                                    outputArea.appendText("There are no events for the customer’s vehicle(vehicleID=" + vehicleID + ").\n");
                                    outputArea.appendText("Total money:0\n");
                                });
                                whileNotConncectedToServerFlag = false;
                            } else {
                                String[] getParkingEventsListResultSplit = getNewParkingEventsListResult.split(" ");
                                String parkingEventListJsonStr = getParkingEventsListResultSplit[0];
                                String totalMoney = getParkingEventsListResultSplit[1];
                                Platform.runLater(() -> this.openJsonViewer(parkingEventListJsonStr, totalMoney, vehicleID));
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
        else if(getParkingEventsListResult.equals("invalidVehicleID"))
            outputArea.appendText("Invalid VehicleID.\n");
        else if(getParkingEventsListResult.equals("sendParkingEventsListIsEmpty")){
            outputArea.appendText("There are no events for the customer’s vehicle(vehicleID="+vehicleID+").\n");
            outputArea.appendText("Total money:0\n");
        }

        else{
            String[] getParkingEventsListResultSplit = getParkingEventsListResult.split(" ");
            String parkingEventListJsonStr = getParkingEventsListResultSplit[0];
            String totalMoney = getParkingEventsListResultSplit[1];
            this.openJsonViewer(parkingEventListJsonStr,totalMoney,vehicleID);

        }
    }


    private void openJsonViewer(String jsonString, String moneyPaid, String vehicleID) {
        // Split the jsonString into individual parking events
        String[] parkingEvents = jsonString
                .replace("[", "")
                .replace("]", "")
                .split("},\\{");

        // Use StringBuilder to create the formatted string
        StringBuilder formattedTextBuilder = new StringBuilder();
        formattedTextBuilder.append(String.format("%-20s %-20s %-20s %-15s\n",
                "Start Time", "End Time", "Parking Space", "Cost"));

        for (String event : parkingEvents) {
            event = event.replace("{", "").replace("}", ""); // Clean up the string
            String[] attributes = event.split(",");
            String startTime = "", endTime = "", parkingSpaceNumber = "", cost = "";
            for (String attribute : attributes) {
                String[] keyValue = attribute.split(":");
                String key = keyValue[0].replace("\"", "").trim();
                String value = keyValue[1].replace("\"", "").trim();

                switch (key) {
                    case "startTime":
                    case "endTime":
                        // Extract the date and time parts
                        String[] dateTime = value.split("T");
                        if (dateTime.length == 2) {
                            value = dateTime[0] + " " + dateTime[1]; // Combine date and time for display
                        }
                        if (key.equals("startTime")) {
                            startTime = value;
                        } else {
                            endTime = value;
                        }
                        break;
                    case "parkingSpaceNumber":
                        parkingSpaceNumber = value;
                        break;
                    case "cost": // Adjust this if the key in the JSON is different
                        cost = value;
                        break;
                }
            }
            // Append a formatted line to the StringBuilder
            formattedTextBuilder.append(String.format("%-20s %-20s %-20s %-15s\n",
                    startTime, endTime, parkingSpaceNumber, cost));
        }
        formattedTextBuilder.append("\nTotal money: ").append(moneyPaid); // Add total money at the end

        // Now display this text in the popup
        Stage jsonViewerStage = new Stage();
        jsonViewerStage.setTitle("View Parking History of vehicleID=" + vehicleID + ":");

        TextArea textArea = new TextArea(formattedTextBuilder.toString());
        textArea.setEditable(false);
        textArea.setFont(Font.font("monospaced", FontWeight.NORMAL, FontPosture.REGULAR, 12));

        Scene scene = new Scene(textArea, 600, 400);
        jsonViewerStage.setScene(scene);

        jsonViewerStage.show();
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