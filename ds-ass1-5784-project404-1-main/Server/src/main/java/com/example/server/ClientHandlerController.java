package com.example.server;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import com.mongodb.client.*;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerAddress;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.FullDocument;
import org.bson.BsonDocument;
import org.bson.Document;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientHandlerController {
    public TextField serverDatabaseNameInput;
    public TextField serverIPAddressInput;
    public TextField serverPortNumberInput;
    private static Server server;
    public static String serverIPAddressInputStr;
    public static int serverPortNumberInputInt;
    private static String sourceDatabaseName;

    private static final int MAX_LENGTH = 63; // Maximum allowed length for mongodb database name//
    public Button loadServersToSyncButton;
    public ComboBox selectServerToSyncComboBox;
    public String currentSelectedServerIPAddressToSync;
    public String currentSelectedServerPortNumberToSync;


    @FXML
    private void initialize() {
    }

    public Button startServerButton;

    @FXML
    public void loadInitialData() {



        if (!serverDatabaseNameInput.getText().isEmpty()) {
                sourceDatabaseName = serverDatabaseNameInput.getText();
            if (isValidDatabaseName(sourceDatabaseName)) {
                System.out.println("The MongoDB database name is valid.");
                //open a file chooser dialog to select a JSON file
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Choose JSON File");
                fileChooser.getExtensionFilters().addAll(
                        new FileChooser.ExtensionFilter("JSON Files", "*.json"),
                        new FileChooser.ExtensionFilter("All Files", "*.*"));
                Stage stage = (Stage) startServerButton.getScene().getWindow();
                File selectedFile = fileChooser.showOpenDialog(stage);
                if (selectedFile != null) {
                    new Thread(() -> {
                        HandleDB handleDB = new HandleDB();
                        handleDB.setSourceDatabaseName(sourceDatabaseName);
                        handleDB.loadDataToMongoDB(selectedFile.getAbsolutePath());
                        StatusForSync.setIsLoadDatabaseDone(true);
                    }).start();
                }
            } else {
                System.out.println("The MongoDB database name is invalid.");
            }

        }

    }

    public void exit(ActionEvent actionEvent) {
        Platform.exit();
    }

    public ClientHandlerController() {

    }

    @FXML
    private void handleStartServerButton(ActionEvent event) {
        //start server thread
        System.out.println("start button pressed");
        //start server thread
        if (!serverIPAddressInput.getText().isEmpty() && !serverPortNumberInput.getText().isEmpty()) {
            if(ipAddressCheck(serverIPAddressInput.getText()).equals("Illegal IP Addresss")){
                System.out.println(ipAddressCheck(serverIPAddressInput.getText()));
            }else if(portNumberCheck(serverPortNumberInput.getText()) ==0){
                System.out.println("Illegal Port Number");
            }else {
                serverIPAddressInputStr = ipAddressCheck(serverIPAddressInput.getText());
                serverPortNumberInputInt = portNumberCheck(serverPortNumberInput.getText());
                server = new Server(serverIPAddressInputStr, serverPortNumberInputInt);
                //this.server.setDaemon(true);
                server.start();
                StatusForSync.setIsServerStarted(true);
            }
        }else{
            System.out.println("IP address or port number is empty!!!!");
        }
    }

    @FXML
    private void handleStopServerButton(ActionEvent event) {
        System.out.println("stop button pressed");
        server.interrupt();
    }

    @FXML
    public void syncDatabasesBetweenServers(ActionEvent actionEvent) {
        if (StatusForSync.getIsLoadDatabaseDone() && StatusForSync.getIsServerStarted()) {
            if(ipAddressCheck(this.currentSelectedServerIPAddressToSync).equals("Illegal IP Addresss")){
                System.out.println(ipAddressCheck(this.currentSelectedServerIPAddressToSync));
            }else if( portNumberCheck(this.currentSelectedServerPortNumberToSync) ==0){
                System.out.println("Illegal Port Number");
            }else {
                String targetIpAddress = this.currentSelectedServerIPAddressToSync;
                int targetPortNumber = portNumberCheck(this.currentSelectedServerPortNumberToSync);

                ServerTalker serverTalker = new ServerTalker(targetIpAddress, targetPortNumber, sourceDatabaseName, StatusForSync.getLastSyncTimeStr());
                String serverResponse = serverTalker.syncTargetedServer();
                if (serverResponse.equals("sync Failed\n") || serverResponse.equals("Server communication error.\n")) {
                    System.out.println(serverResponse);
                    return;
                }
                String[] targetedServerMessageSplit = serverResponse.split(" ");
                String targetServerDatabase = targetedServerMessageSplit[0];
                String lastSyncTargetTimeStr = targetedServerMessageSplit[1];

                int defaultMongoDBServerPort = 27017;
                // Define your MongoDB settings
                List<String> collectionsToSync = Arrays.asList("UsersDB", "ParkingHistoryDB", "RunningParkingEventsDB");
                MongoClientSettings sourceSettings = MongoClientSettings.builder()
                        .applyToClusterSettings(builder ->
                                builder.hosts(Arrays.asList(new ServerAddress(serverIPAddressInputStr, defaultMongoDBServerPort))))
                        .build();
                MongoClientSettings targetSettings = MongoClientSettings.builder()
                        .applyToClusterSettings(builder ->
                                builder.hosts(Arrays.asList(new ServerAddress(targetIpAddress, defaultMongoDBServerPort))))
                        .build();

                // ExecutorService for asynchronous operation
                ExecutorService executor = Executors.newCachedThreadPool();

                if (convertStringToLocalDateTime(lastSyncTargetTimeStr).isAfter(convertStringToLocalDateTime(StatusForSync.getLastSyncTimeStr()))) {
                    // Assuming this method should only run after a specific date, adjust as necessary
                    copyDataFromTargetToSource(ClientHandlerController.sourceDatabaseName, targetServerDatabase, collectionsToSync, sourceSettings, targetSettings);
                }

                // Loop over collections and synchronize
                for (String collectionName : collectionsToSync) {
                    executor.submit(() -> syncCollection(ClientHandlerController.sourceDatabaseName, targetServerDatabase, collectionName, sourceSettings, targetSettings));
                }
            }
        }else{
            System.out.println("you need to load database and start server first!");
        }
    }

    private static void syncCollection(String sourceDatabaseName, String targetDatabaseName, String collectionName, MongoClientSettings sourceSettings, MongoClientSettings targetSettings) {
        try (MongoClient sourceClient = MongoClients.create(sourceSettings);
             MongoClient targetClient = MongoClients.create(targetSettings)) {

            MongoDatabase sourceDatabase = sourceClient.getDatabase(sourceDatabaseName);
            MongoDatabase targetDatabase = targetClient.getDatabase(targetDatabaseName);
            MongoCollection<Document> sourceCollection = sourceDatabase.getCollection(collectionName);
            MongoCollection<Document> targetCollection = targetDatabase.getCollection(collectionName);


            try (MongoChangeStreamCursor<ChangeStreamDocument<Document>> cursor = sourceCollection.watch()
                    .fullDocument(FullDocument.UPDATE_LOOKUP)
                    .cursor()) {
                while (cursor.hasNext()) {
                    ChangeStreamDocument<Document> change = cursor.next();
                    Document document = change.getFullDocument();
                    switch (change.getOperationType()) {
                        case INSERT:
                        case REPLACE:
                        case UPDATE:
                            if (document != null) {
                                targetCollection.replaceOne(new Document("_id", document.get("_id")), document, new ReplaceOptions().upsert(true));
                            }
                            break;
                        case DELETE:
                            BsonDocument key = change.getDocumentKey();
                            if (key != null) {
                                targetCollection.deleteOne(key);
                            }
                            break;
                        default:
                            break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void copyDataFromTargetToSource(String sourceDatabaseName, String targetDatabaseName, List<String> collections, MongoClientSettings sourceSettings, MongoClientSettings targetSettings) {
        try (MongoClient sourceClient = MongoClients.create(sourceSettings);
             MongoClient targetClient = MongoClients.create(targetSettings)) {
            MongoDatabase sourceDatabase = sourceClient.getDatabase(sourceDatabaseName);
            MongoDatabase targetDatabase = targetClient.getDatabase(targetDatabaseName);

            for (String collectionName : collections) {
                MongoCollection<Document> sourceCollection = sourceDatabase.getCollection(collectionName);
                MongoCollection<Document> targetCollection = targetDatabase.getCollection(collectionName);

                sourceCollection.deleteMany(new Document()); // Clear source collection

                try (MongoCursor<Document> cursor = targetCollection.find().iterator()) {
                    while (cursor.hasNext()) {
                        sourceCollection.insertOne(cursor.next());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String getCurrentTime() {
        //get current date and time
        LocalDateTime currentDateTime = LocalDateTime.now();

        //change formatter to yyyy-MM-dd'T'HH:mm format
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

        //return yyyy-MM-dd'T'HH:mm format as string
        return currentDateTime.format(formatter);
    }

    private LocalDateTime convertStringToLocalDateTime(String dateTimeStr) {
        //change formatter to yyyy-MM-dd'T'HH:mm format
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

        //return yyyy-MM-dd'T'HH:mm format as LocalDateTime
        LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr, formatter);

        return dateTime;
    }

    private int portNumberCheck(String portNumberStr) {
        //this function check if the port number is an integer between 1024 and 65535
        //^:match the beginning of the statement
        //([0-9]{4,5}):we make group contains 0000-99999
        //$:match the end of the statement
        String REGEX_PATTERN = "^([0-9]{4,5})$";
        Pattern pattern = Pattern.compile(REGEX_PATTERN);
        Matcher matcher = pattern.matcher(portNumberStr);
        if (!matcher.matches()) {
            return 0;
        }
        //this is our group 1 :([0-9]{4,5}) we parse it to int
        int portNumberint = Integer.parseInt(matcher.group(1));
        //we check if the port number is an integer between 1024 and 65535
        if (portNumberint < 1025 || portNumberint > 65535) {
            return 0;
        }
        return portNumberint;
    }


    private String ipAddressCheck(String ipAddress) {
        //this function check if the ip address format is the standard IPv4 four byte dot separated format
        //^:match the beginning of the statement
        //25[0-5]:250-255
        //2[0-4][0-9]:200-249
        //[01]?[0-9][0-9]?:0-199
        //(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.:(0-255).
        //$:match the end of the statement
        String ipv4Pattern = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        String REGEX_PATTERN = ipv4Pattern;
        Pattern pattern = Pattern.compile(REGEX_PATTERN);
        Matcher matcher = pattern.matcher(ipAddress);
        if (!matcher.matches()) {
            return "Illegal IP Addresss";
        }
        return ipAddress;
    }

    public static boolean isValidDatabaseName(String dbName) {
        // Check if the database name is null or empty
        if (dbName == null || dbName.isEmpty()) {
            return false;
        }
        // Check if the database name exceeds the maximum length
        if (dbName.length() > MAX_LENGTH) {
            return false;
        }
        // Check for illegal characters
        if (dbName.contains(" ") || dbName.contains(".") || dbName.contains("$") ||
                dbName.contains("/") || dbName.contains("\\") || dbName.contains("\0")) {
            return false;
        }
        // Check if the name is not using reserved system database names
        if (dbName.equalsIgnoreCase("admin") || dbName.equalsIgnoreCase("local") ||
                dbName.equalsIgnoreCase("config")) {
            // You might allow these names depending on your context
            return false;
        }
        // If all checks pass, return true
        return true;
    }

    public void loadServersToSync(ActionEvent actionEvent) {
        //we open a file chooser dialog to select a txt file
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Text File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text Files", "*.txt"),  // Change this line to target .txt files
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        Stage stage = (Stage) startServerButton.getScene().getWindow();
        //we show the open dialog in the current stage
        File selectedFile = fileChooser.showOpenDialog(stage);
        //we check if a file was selected
        if (selectedFile != null) {
            try {
                //we read all lines from the selected file
                List<String> lines = Files.readAllLines(selectedFile.toPath());
                //we clear existing items and add new lines to the ComboBox from txt file
                selectServerToSyncComboBox.getItems().clear();
                selectServerToSyncComboBox.getItems().addAll(lines);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public void setCurrentSelectedServerToSync(ActionEvent actionEvent) {
        //we save the selected item
        String selectedItem = (String) selectServerToSyncComboBox.getSelectionModel().getSelectedItem();
        //we split it to ip address and port number
        if (selectedItem != null && selectedItem.contains(" ")) {
            String[] parts = selectedItem.split(" ");
            this.currentSelectedServerIPAddressToSync = parts[0];
            this.currentSelectedServerPortNumberToSync = parts.length > 1 ? parts[1] : "Port not provided";

        } else {
            System.out.println("Invalid server format, it must be in this format: 'IP Port'.");
        }
    }
}

