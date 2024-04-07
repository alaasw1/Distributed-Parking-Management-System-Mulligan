package com.example.server;

import com.mongodb.MongoClientSettings;
import com.mongodb.ServerAddress;
import com.mongodb.client.*;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.FullDocument;

import org.bson.BsonDocument;
import org.bson.Document;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientHandler extends Thread {
    private Socket clientSocket;

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }
    @Override
    public void run(){
        //start to listen on the server socket
        System.out.println("handling " + clientSocket.getInetAddress().toString() + ":" + clientSocket.getPort());
        if (!interrupted()) {
            try {
                //create output stream to send data to the client
                OutputStream output = this.clientSocket.getOutputStream();
                PrintWriter writer = new PrintWriter(output, true);

                //create input stream to receive data from the client
                InputStream input = this.clientSocket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));

                //receive client message
                String clientMessage = reader.readLine();

                //handle each request
                String serverResponse;
                String[] clientMessageSplit = clientMessage.split(" ");
                switch (clientMessageSplit[0]){
                    case "startParking":
                        serverResponse = handleStartParkingEvent(clientMessageSplit[1],clientMessageSplit[2]);
                        break;
                    case "stopParking":
                        serverResponse = handleStopParkingEvent(clientMessageSplit[1]);
                        break;
                    case "getParkingEventsList":
                        serverResponse = handleSendParkingEventsList(clientMessageSplit[1]);
                        break;
                    case "checkVehicle":
                        serverResponse = handleCheckVehicleEvent(clientMessageSplit[1],clientMessageSplit[2]);
                        break;
                    case "checkPastParking":
                        serverResponse = handleCheckPastParkingEvent(clientMessageSplit[1],clientMessageSplit[2],clientMessageSplit[3]);
                        break;
                    case "getSpaceReport":
                        serverResponse = handleGetSpaceReportEvent(clientMessageSplit[1]);
                        break;
                    case "sync":
                        serverResponse = handleSyncEvent(clientMessageSplit[1],clientMessageSplit[2]);
                        break;

                    default:
                        serverResponse = "error\n";
                        break;
                }
                //send response to the client
                writer.println(serverResponse);
                //close the resources in the end
                writer.close();
                reader.close();
                clientSocket.close();
            } catch (IOException e) {
                //handle any exceptions that might occur during connection
                System.out.println("Server exception: " + e.getMessage());
                e.printStackTrace();
            }
            finally {
                if (this.clientSocket != null && !this.clientSocket.isClosed()) {
                    try {
                        this.clientSocket.close();
                    } catch (IOException e) {
                        //handle any exceptions that might occur during connection
                        System.out.println("Client handler exception: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private String handleSyncEvent(String targetServerDatabase,String lastSyncTargetTimeStr) {
        try {
            String targetIpAddress = clientSocket.getInetAddress().getHostAddress();

            if (convertStringToLocalDateTime(lastSyncTargetTimeStr).isAfter(convertStringToLocalDateTime(StatusForSync.getLastSyncTimeStr()))) {
                //source server need to copy database and then sync
                syncDatabasesBetweenServers(targetIpAddress, targetServerDatabase, lastSyncTargetTimeStr);
                //update my lastSyncTime to lastSyncTargetTime
                StatusForSync.setLastSyncTimeStr(lastSyncTargetTimeStr);

                return "syncStarted " + HandleDB.getSourceDatabaseName() + " " + StatusForSync.getLastSyncTimeStr() + "\n";
            } else if (convertStringToLocalDateTime(lastSyncTargetTimeStr).isBefore(convertStringToLocalDateTime(StatusForSync.getLastSyncTimeStr()))) {
                //target server need to copy database and then sync
                syncDatabasesBetweenServers(targetIpAddress, targetServerDatabase, lastSyncTargetTimeStr);
                return "syncAfterCopy " + HandleDB.getSourceDatabaseName() + " " + StatusForSync.getLastSyncTimeStr() + "\n";
            }
            //update my lastSyncTime to current time
            StatusForSync.setLastSyncTimeStr(getCurrentTimeWithSec());
            syncDatabasesBetweenServers(targetIpAddress, targetServerDatabase, StatusForSync.getLastSyncTimeStr());
            return "syncStarted " + HandleDB.getSourceDatabaseName() + " " + StatusForSync.getLastSyncTimeStr() + "\n";
        }catch (Exception e) {
            return "syncFailed";
        }
    }

    private String handleGetSpaceReportEvent(String parkingSpaceNumber) {
        HandleDB handleDB = new HandleDB();
        //check if parkingSpaceNumber exist
        boolean isParkingSpaceNumberExist =handleDB.isParkingSpaceExists(parkingSpaceNumber);
        if (!isParkingSpaceNumberExist)
            return "invalidParkingSpaceNumber " + parkingSpaceNumber + "\n";
        return handleDB.getSpaceReportEvent(parkingSpaceNumber);
    }

    private String handleCheckPastParkingEvent(String vehicleID, String parkingSpaceNumber, String givenTime) {
        HandleDB handleDB = new HandleDB();
        //check if user(vehicleID) exist
        boolean isUserExist =handleDB.isVehicleIDExists(vehicleID);
        if (!isUserExist)
            return "invalidVehicleID "+ vehicleID + " " + parkingSpaceNumber + " " + givenTime + "\n";
        //check if parkingSpaceNumber exist
        boolean isParkingSpaceNumberExist =handleDB.isParkingSpaceExists(parkingSpaceNumber);
        if (!isParkingSpaceNumberExist)
            return "invalidParkingSpaceNumber "+ vehicleID + " " + parkingSpaceNumber + " " + givenTime + "\n";
        //check if there is another parking event for the customer in the Start state (vehicle is currently parked)
        boolean isVehicleCurrentlyParked =handleDB.isVehicleIDExistsInRunningParkingEventsDB(vehicleID);
        if (isVehicleCurrentlyParked){
            String checkVehicleMOResult = handleDB.checkVehiclePastingMO(vehicleID,parkingSpaceNumber,givenTime);
            if(checkVehicleMOResult.equals("ParkingOk")){
                return "checkPastParkingOk " + vehicleID + " " + parkingSpaceNumber + " " + givenTime + "\n";
            } else if(checkVehicleMOResult.equals("ParkingDurationExceeded")){
                this.handleStopParkingEvent(vehicleID);
            } else if(checkVehicleMOResult.equals("ParkingNotOk")){
                //delete vehicleID from RunningParkingEventsDB collection
                if (handleDB.deleteVehicleID(vehicleID))
                    return "checkPastParkingNotOk " + vehicleID + " " + parkingSpaceNumber + " " + givenTime + "\n";
                else
                    return "error";
            } else{
                return "checkPastParkingNotOk " + vehicleID + " " + parkingSpaceNumber + " " + givenTime + "\n";
            }
        }else{
            String checkVehicleMOResult = handleDB.checkVehiclePastingMO(vehicleID,parkingSpaceNumber,givenTime);
            if(checkVehicleMOResult.equals("ParkingOk")){
                return "checkPastParkingOk " + vehicleID + " " + parkingSpaceNumber + " " + givenTime + "\n";
            } else if(checkVehicleMOResult.equals("ParkingNotOk")){
                return "checkPastParkingNotOk " + vehicleID + " " + parkingSpaceNumber + " " + givenTime + "\n";
            } else{
                return "error";
            }
        }
        return "error";
    }

    private String handleCheckVehicleEvent(String vehicleID, String parkingSpaceNumber) {
        HandleDB handleDB = new HandleDB();
        //check if user(vehicleID) exist
        boolean isUserExist =handleDB.isVehicleIDExists(vehicleID);
        if (!isUserExist)
            return "invalidVehicleID "+ vehicleID + " " + parkingSpaceNumber + "\n";
        //check if parkingSpaceNumber exist
        boolean isParkingSpaceNumberExist =handleDB.isParkingSpaceExists(parkingSpaceNumber);
        if (!isParkingSpaceNumberExist)
            return "invalidParkingSpaceNumber "+ vehicleID + " " + parkingSpaceNumber + "\n";
        //check if there is another parking event for the customer in the Start state (vehicle is currently parked)
        boolean isVehicleCurrentlyParked =handleDB.isVehicleIDExistsInRunningParkingEventsDB(vehicleID);
        if (isVehicleCurrentlyParked){
            String checkVehiclePEOResult = handleDB.checkVehiclePEO(vehicleID,parkingSpaceNumber);
            if(checkVehiclePEOResult.equals("ParkingOk")){
                return "checkVehicleOk "+ vehicleID + " " + parkingSpaceNumber + "\n";
            } else if(checkVehiclePEOResult.equals("ParkingDurationExceeded")){
                this.handleStopParkingEvent(vehicleID);
            }else if(checkVehiclePEOResult.equals("ParkingNotOk")){
                //delete vehicleID from RunningParkingEventsDB collection
                if (handleDB.deleteVehicleID(vehicleID))
                    return "checkVehicleNotOk "+ vehicleID + " " + parkingSpaceNumber + "\n";
                else
                    return "error";
            } else{
                return "checkVehicleNotOk "+ vehicleID + " " + parkingSpaceNumber + "\n";
            }
        }
        return "checkVehicleNotOk "+ vehicleID + " " + parkingSpaceNumber + "\n";
    }

    private String handleSendParkingEventsList(String vehicleID) {
        HandleDB handleDB = new HandleDB();
        //check if user(vehicleID) exist
        boolean isUserExist =handleDB.isVehicleIDExists(vehicleID);
        if (!isUserExist)
            return "invalidVehicleID "+ vehicleID + "\n";
        return handleDB.getParkingEventsList(vehicleID);
    }

    private String handleStopParkingEvent(String vehicleID) {
        HandleDB handleDB = new HandleDB();
        //check if user(vehicleID) exist
        boolean isUserExist =handleDB.isVehicleIDExists(vehicleID);
        if (!isUserExist)
            return "invalidVehicleID "+ vehicleID + "\n";
        //check if there is parking event for the customer in the Start state (vehicle is currently parked)
        boolean isVehicleCurrentlyParked =handleDB.isVehicleIDExistsInRunningParkingEventsDB(vehicleID);
        if (isVehicleCurrentlyParked){
            handleDB.updateParkingEventsForVehicleID(vehicleID);
            //delete vehicleID from RunningParkingEventsDB collection
            handleDB.deleteVehicleID(vehicleID);
            return "parkingStopped " + vehicleID + "\n";
        } else{
            return "thereIsNoParkingEvent " + vehicleID + "\n";
        }

    }

    private String handleStartParkingEvent(String vehicleID, String parkingSpaceNumber) {
        HandleDB handleDB = new HandleDB();
        //check if user(vehicleID) exist
        boolean isUserExist =handleDB.isVehicleIDExists(vehicleID);
        if (!isUserExist)
            return "invalidVehicleID "+ vehicleID + " " + parkingSpaceNumber + "\n";
        //check if parkingSpaceNumber exist
        boolean isParkingSpaceNumberExist =handleDB.isParkingSpaceExists(parkingSpaceNumber);
        if (!isParkingSpaceNumberExist)
            return "invalidParkingSpaceNumber "+ vehicleID + " " + parkingSpaceNumber + "\n";
        //check if there is another parking event for the customer in the Start state (vehicle is currently parked)
        boolean isVehicleCurrentlyParked =handleDB.isVehicleIDExistsInRunningParkingEventsDB(vehicleID);
        if (isVehicleCurrentlyParked){
            //handle stop parking event
            this.handleStopParkingEvent(vehicleID);
        }
        //create a parking event object
        Document parkingEvent = new Document("vehicleID", vehicleID)
                .append("startTime", this.getCurrentTime())
                .append("endTime", "")
                .append("closed", "false")
                ;
        //add the event to ParkingHistoryDB collection
        boolean addedParkingEventSuccessfully = handleDB.addParkingEvent(parkingSpaceNumber,parkingEvent);
        //add vehicleID to RunningParkingEventsDB collection
        handleDB.insertVehicleIDInRunningParkingEventsDB(vehicleID);
        isVehicleCurrentlyParked =handleDB.isVehicleIDExistsInRunningParkingEventsDB(vehicleID);
        if(addedParkingEventSuccessfully && isVehicleCurrentlyParked){
            return "parkingStarted "+ vehicleID + " " + parkingSpaceNumber + "\n";
        }
        else{
            return "error";
        }
    }

    private String getCurrentTime() {
        //get current date and time
        LocalDateTime currentDateTime = LocalDateTime.now();

        //change it to yyyy-MM-dd'T'HH:mm format
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

        //return yyyy-MM-dd'T'HH:mm format as string
        return currentDateTime.format(formatter);
    }

    private String getCurrentTimeWithSec() {
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
    public void syncDatabasesBetweenServers(String targetIpAddress,String targetServerDatabase,String lastSyncTargetTimeStr ) {
        if(StatusForSync.getIsLoadDatabaseDone() && StatusForSync.getIsServerStarted()) {
            int defaultMongoDBServerPort = 27017;
            // Define your MongoDB settings
            List<String> collectionsToSync = Arrays.asList("UsersDB", "ParkingHistoryDB", "RunningParkingEventsDB");
            MongoClientSettings sourceSettings = MongoClientSettings.builder()
                    .applyToClusterSettings(builder ->
                            builder.hosts(Arrays.asList(new ServerAddress(ClientHandlerController.serverIPAddressInputStr, defaultMongoDBServerPort))))
                    .build();
            MongoClientSettings targetSettings = MongoClientSettings.builder()
                    .applyToClusterSettings(builder ->
                            builder.hosts(Arrays.asList(new ServerAddress(targetIpAddress, defaultMongoDBServerPort))))
                    .build();

            // ExecutorService for asynchronous operation
            ExecutorService executor = Executors.newCachedThreadPool();

            if (convertStringToLocalDateTime(lastSyncTargetTimeStr).isAfter(convertStringToLocalDateTime(StatusForSync.getLastSyncTimeStr()))) {
                // Assuming this method should only run after a specific date, adjust as necessary
                copyDataFromTargetToSource(HandleDB.getSourceDatabaseName(), targetServerDatabase, collectionsToSync, sourceSettings, targetSettings);
            }

            // Loop over collections and synchronize
            for (String collectionName : collectionsToSync) {
                executor.submit(() -> syncCollection(HandleDB.getSourceDatabaseName(), targetServerDatabase, collectionName, sourceSettings, targetSettings));
            }
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


}
