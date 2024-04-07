package com.example.mo;

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
//import org.json.JSONArray;
//import org.json.JSONObject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MunicipalOfficerController {
    public Button loadServersButton;
    //if says "-Tamir", Do Not Touch!-Tamir
    @FXML
    private TextField vehicleNumberInput;
    @FXML
    private TextField parkingSpaceInput;
    @FXML
    private DatePicker datePicker; // Added DatePicker for date input-Tamir
    @FXML
    private TextField checkStartingHourInput; // Field for inputting the hour-Tamir
    @FXML
    private TextArea outputArea;
        @FXML
    private ComboBox<String> serverIPDropdown;
    @FXML
    private ComboBox<Integer> serverPortDropdown;
    private static final DateTimeFormatter HOUR_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private List<String> serversIPAddressAndPorts;
    private static boolean whileNotConncectedToServerFlag = true;

    @FXML
    private void initialize() {

    }

    @FXML
    private void generateParkingReport() {
        String parkingSpaceNumber = parkingSpaceInput.getText();


        String selectedIP = serverIPDropdown.getSelectionModel().getSelectedItem();
        int selectedPort = serverPortDropdown.getSelectionModel().getSelectedItem();

        MOClient moClient = new MOClient(selectedIP, selectedPort);

        String generateParkingReportResult = moClient.getSpaceReport(parkingSpaceNumber);
        if (generateParkingReportResult.equals("error")) {
            outputArea.appendText("Server communication error.\n");
        } else if (generateParkingReportResult.equals("serverNotConnected")) {
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
                            MOClient newMOClient = new MOClient(newServerIP, newServerPort);

                            String newGenerateParkingReportResult = newMOClient.getSpaceReport(parkingSpaceNumber);
                            if (newGenerateParkingReportResult.equals("invalidParkingSpaceNumber")) {
                                Platform.runLater(() -> outputArea.appendText("Invalid parkingSpaceNumber.\n"));
                                whileNotConncectedToServerFlag = false;
                            } else if (newGenerateParkingReportResult.equals("serverNotConnected")) {
                                Platform.runLater(() -> outputArea.appendText("server: " + newServerIP + ":" + newServerPort + " not connected\n"));
                            } else if (newGenerateParkingReportResult.equals("error")) {
                                Platform.runLater(() -> outputArea.appendText("Server communication error.\n"));
                                whileNotConncectedToServerFlag = false;
                            } else {
                                Platform.runLater(() -> this.openJsonViewer(generateParkingReportResult, parkingSpaceNumber));
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
        } else if (generateParkingReportResult.equals("invalidParkingSpaceNumber")) {
            outputArea.appendText("Invalid parkingSpaceNumber.\n");
        } else
            this.openJsonViewer(generateParkingReportResult, parkingSpaceNumber);
        }



    private void openJsonViewer(String jsonString, String parkingSpaceNumber) {
        // Split the jsonString into individual parking events-Tamir
        String[] parkingEvents = jsonString
                .replace("[{", "")
                .replace("}]", "")
                .replace("},{", "}\n{")
                .split("\n");

        // Use StringBuilder to create the formatted string-Tamir
        StringBuilder formattedTextBuilder = new StringBuilder();
        formattedTextBuilder.append(String.format("%-10s %-12s %-10s %-12s %-10s %-6s\n",
                "Vehicle ID", "Start Date", "Start Time", "End Date", "End Time", "Status"));
        for (String event : parkingEvents) {
            String[] attributes = event.split("\",\"");
            String vehicleId = "", startDate = "", startTime = "", endDate = "", endTime = "", status = "";
            for (String attribute : attributes) {
                attribute = attribute.replace("\"", "").replace("{", "").replace("}", "");
                String[] keyValue = attribute.split(":", 2);
                String key = keyValue[0].trim();
                String value = keyValue.length > 1 ? keyValue[1].trim() : "";

                switch (key) {
                    case "vehicleID":
                        vehicleId = value;
                        break;
                    case "startTime":
                    case "endTime":
                        String[] dateTimeSplit = value.split("T");
                        if (dateTimeSplit.length == 2) {
                            String date = dateTimeSplit[0];
                            String time = dateTimeSplit[1];
                            if (key.equals("startTime")) {
                                startDate = date;
                                startTime = time;
                            } else {
                                endDate = date;
                                endTime = time;
                            }
                        }
                        break;
                    case "closed":
                        status = value.equals("true") ? "Closed" : "Open";
                        break;
                }
            }
            // Append a formatted line to the StringBuilder
            formattedTextBuilder.append(String.format("%-10s %-12s %-10s %-12s %-10s %-6s\n",
                    vehicleId, startDate, startTime, endDate, endTime, status));
        }

        // Now display this text in the popup
        Stage jsonViewerStage = new Stage();
        jsonViewerStage.setTitle("Parking report of parkingSpaceNumber=" + parkingSpaceNumber + ":");

        TextArea textArea = new TextArea(formattedTextBuilder.toString());
        textArea.setEditable(false);
        textArea.setFont(Font.font("monospaced", FontWeight.NORMAL, FontPosture.REGULAR, 12));

        Scene scene = new Scene(textArea, 600, 400);
        jsonViewerStage.setScene(scene);

        jsonViewerStage.show();
    }


    @FXML
    private void checkPastParking() {
        String vehicleID = vehicleNumberInput.getText();
        String parkingSpaceNumber = parkingSpaceInput.getText();
        LocalDate datePickerInput = datePicker.getValue();
        String givenDate = "";
        String givenTime = checkStartingHourInput.getText();
        String dateTimeString;

        if ((datePickerInput != null) && (givenTime != null) && !(givenTime.isEmpty())) {
            //get givenDate yyyy-MM-dd format
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String formattedDate = datePickerInput.format(dateFormatter);
            //combine the givenDate and givenTime with 'T' to get yyyy-MM-dd'T'HH:mm format
            dateTimeString = formattedDate + "T" + givenTime;
        } else {
            dateTimeString = "";
        }

        System.out.println(dateTimeString);


        String selectedIP = serverIPDropdown.getSelectionModel().getSelectedItem();
        int selectedPort = serverPortDropdown.getSelectionModel().getSelectedItem();


        MOClient moClient = new MOClient(selectedIP, selectedPort);
//        MOClient moClient = new MOClient("127.0.0.1",6666);
        String checkVehiclePastParkingResult = moClient.checkPastParking(vehicleID, parkingSpaceNumber, dateTimeString);
        if(checkVehiclePastParkingResult.equals("server not connected\n")) {
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
                            MOClient newMOClient = new MOClient(newServerIP, newServerPort);

                            String newCheckVehiclePastParkingResult = newMOClient.checkPastParking(vehicleID, parkingSpaceNumber, dateTimeString);
                            if (newCheckVehiclePastParkingResult.equals("in " + dateTimeString+": vehicleID =" + vehicleID + " in parkingSpaceNumber ="+ parkingSpaceNumber + " is OK\n")) {
                                Platform.runLater(() -> outputArea.appendText("in " + dateTimeString+": vehicleID =" + vehicleID + " in parkingSpaceNumber ="+ parkingSpaceNumber + " is OK\n"));
                                whileNotConncectedToServerFlag = false;
                            } else if (newCheckVehiclePastParkingResult.equals("in " + dateTimeString+": vehicleID =" + vehicleID + " in parkingSpaceNumber ="+ parkingSpaceNumber + " is not OK\n")) {
                                Platform.runLater(() -> outputArea.appendText("in " + dateTimeString+": vehicleID =" + vehicleID + " in parkingSpaceNumber ="+ parkingSpaceNumber + " is not OK\n"));
                                whileNotConncectedToServerFlag = false;
                            } else if (newCheckVehiclePastParkingResult.equals("server not connected\n")) {
                                Platform.runLater(() -> outputArea.appendText("server: " + newServerIP + ":" + newServerPort + " not connected\n"));
                            }else if (newCheckVehiclePastParkingResult.equals("Invalid VehicleID.\n")) {
                                Platform.runLater(() -> outputArea.appendText("Invalid VehicleID.\n"));
                                whileNotConncectedToServerFlag = false;
                            } else if (newCheckVehiclePastParkingResult.equals("Invalid parkingSpaceNumber.\n")) {
                                Platform.runLater(() -> outputArea.appendText("Invalid parkingSpaceNumber.\n"));
                                whileNotConncectedToServerFlag = false;
                            }else{
                                Platform.runLater(() -> outputArea.appendText("Server communication error.\n"+newCheckVehiclePastParkingResult));
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
            outputArea.appendText(checkVehiclePastParkingResult);
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