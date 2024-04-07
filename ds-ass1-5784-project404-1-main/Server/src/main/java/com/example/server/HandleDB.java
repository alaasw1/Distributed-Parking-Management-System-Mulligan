package com.example.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import com.google.gson.Gson;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import static com.mongodb.client.model.Filters.eq;
public class HandleDB {


    private static String sourceDatabaseName = "MulliganDB";
    public synchronized void loadDataToMongoDB(String usersDBjsonFilePath) {
        String uri = "mongodb://localhost:27017";
        String UsersCollectionName = "UsersDB";
        String ParkingHistoryCollectionName = "ParkingHistoryDB";
        String RunningParkingEventsCollectionName = "RunningParkingEventsDB";
        try (MongoClient mongoClient = MongoClients.create(uri)) {
            MongoDatabase database = mongoClient.getDatabase(sourceDatabaseName);

            //drop if exist and create collections
            database.getCollection(ParkingHistoryCollectionName).drop();
            database.getCollection(RunningParkingEventsCollectionName).drop();
            database.getCollection(UsersCollectionName).drop();

            MongoCollection<Document> parkingHistoryCollection = database.getCollection(ParkingHistoryCollectionName);
            MongoCollection<Document> runningParkingEventsCollection = database.getCollection(RunningParkingEventsCollectionName);
            MongoCollection<Document> usersCollection = database.getCollection(UsersCollectionName);

            //we create Jackson ObjectMapper to read JSON file
            ObjectMapper mapper = new ObjectMapper();
            TypeReference<HashMap<String, Object>> typeRef = new TypeReference<>() {};

            HashMap<String, Object> jsonMap = mapper.readValue(new File(usersDBjsonFilePath), typeRef);

            List<Map<String, Object>> parkingSpaces = (List<Map<String, Object>>) jsonMap.get("parkingSpaces");
            List<Map<String, Object>> parkingZones = (List<Map<String, Object>>) jsonMap.get("parkingZones");

            for (Map<String, Object> space : parkingSpaces) {
                for (Map<String, Object> zone : parkingZones) {
                    if (space.get("zoneId").equals(zone.get("zoneId"))) {
                        Document doc = new Document("_id", new ObjectId())
                                .append("parkingSpaceNumber", space.get("parkingSpaceId").toString())
                                .append("zoneId", space.get("zoneId").toString())
                                .append("zoneName", zone.get("zoneName").toString())
                                .append("hourlyRate", zone.get("hourlyRate").toString())
                                .append("maxParkingMinutes", zone.get("maxParkingMinutes").toString())
                                .append("parkingEvents", new ArrayList<>());
                        parkingHistoryCollection.insertOne(doc);
                        break;
                    }
                }
            }
            //now adding the events parkingEvents
            List<Map<String, Object>> parkingEvents = (List<Map<String, Object>>) jsonMap.get("parkingEvents");

            for (Map<String, Object> event : parkingEvents) {
                String vehicleID = event.get("vehicleId").toString();
                String startTime = event.get("startTime").toString();
                String endTime = event.get("endTime").toString();
                String closed = event.get("closed").toString();
                String parkingSpaceId = event.get("parkingSpaceId").toString();

                //create new event object
                Document eventDoc = new Document()
                        .append("vehicleID", vehicleID)
                        .append("startTime", startTime)
                        .append("endTime", endTime)
                        .append("closed", closed);

                //push new structure event to parkingevents to matched parkingSpaceNumber
                parkingHistoryCollection.updateOne(Filters.eq("parkingSpaceNumber", parkingSpaceId),
                        Updates.push("parkingEvents", eventDoc));
            }

            FindIterable<Document> documents = parkingHistoryCollection.find();
            for (Document doc : documents) {
                //iterating on each parkingEvents array in the collection
                List<Document> parkingEventsList = (List<Document>) doc.get("parkingEvents");
                if (parkingEventsList != null) {
                    for (Document event : parkingEventsList) {
                        String closed = event.getString("closed");
                        //we check if the event is not closed we add the vehicle id to RunningParkingEventsDB
                        if ("false".equals(closed)) {
                            String vehicleID = event.getString("vehicleID");
                            this.insertVehicleIDInRunningParkingEventsDB(vehicleID);
                        }
                    }
                }
            }

            List<Map<String, Object>> vehicles = (List<Map<String, Object>>) jsonMap.get("vehicles");

            for (Map<String, Object> vehicle : vehicles) {
                Document doc = new Document()
                        .append("vehicleID", vehicle.get("vehicleId").toString())
                        .append("username", vehicle.get("ownerName").toString())
                        .append("manufacturer", vehicle.get("manufacturer").toString())
                        .append("model", vehicle.get("model").toString())
                        .append("vin", vehicle.get("vin").toString());
                usersCollection.insertOne(doc);
            }

            System.out.println("Database and collections loading completed.");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    public synchronized boolean isVehicleIDExists(String vehicleID) {
        String uri = "mongodb://localhost:27017";
        String collectionName = "UsersDB";

        try (MongoClient mongoClient = MongoClients.create(uri)) {
            MongoDatabase database = mongoClient.getDatabase(sourceDatabaseName);

            //check if the UsersDB collection exists
            if (!database.listCollectionNames().into(new ArrayList<>()).contains(collectionName)) {
                System.out.println("Collection does not exist: " + collectionName);
                return false;
            }

            //get the UsersDB collection
            MongoCollection<Document> usersCollection = database.getCollection(collectionName);

            //build a query to find the user by username within the "UsersDB" array
            Bson eqDocumentToFindQuery = eq("vehicleID", vehicleID);
            Document eqDocumentToFind = usersCollection.find(eqDocumentToFindQuery).first();
            return (eqDocumentToFind != null ? true : false);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    public synchronized boolean isParkingSpaceExists(String parkingSpaceNumber) {
        String uri = "mongodb://localhost:27017";
        String collectionName = "ParkingHistoryDB";

        try (MongoClient mongoClient = MongoClients.create(uri)) {
            MongoDatabase database = mongoClient.getDatabase(sourceDatabaseName);

            //check if the UsersDB collection exists
            if (!database.listCollectionNames().into(new ArrayList<>()).contains(collectionName)) {
                System.out.println("Collection does not exist: " + collectionName);
                return false;
            }

            //get the UsersDB collection
            MongoCollection<Document> usersCollection = database.getCollection(collectionName);

            //build a query to find the user by username within the "UsersDB" array
            Bson eqDocumentToFindQuery = eq("parkingSpaceNumber", parkingSpaceNumber);
            Document eqDocumentToFind = usersCollection.find(eqDocumentToFindQuery).first();
            return (eqDocumentToFind != null ? true : false);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    public synchronized boolean isVehicleIDExistsInRunningParkingEventsDB(String vehicleID) {
        String uri = "mongodb://localhost:27017";
        String collectionName = "RunningParkingEventsDB";

        try (MongoClient mongoClient = MongoClients.create(uri)) {
            MongoDatabase database = mongoClient.getDatabase(sourceDatabaseName);

            //check if the UsersDB collection exists
            if (!database.listCollectionNames().into(new ArrayList<>()).contains(collectionName)) {
                System.out.println("Collection does not exist: " + collectionName);
                return false;
            }

            //get the UsersDB collection
            MongoCollection<Document> usersCollection = database.getCollection(collectionName);

            //build a query to find the user by username within the "RunningParkingEventsDB" array
            Bson eqDocumentToFindQuery = eq("vehicleID", vehicleID);
            Document eqDocumentToFind = usersCollection.find(eqDocumentToFindQuery).first();
            return (eqDocumentToFind != null ? true : false);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    public synchronized void updateParkingEventsForVehicleID(String vehicleID) {
        String uri = "mongodb://localhost:27017";
        String collectionName = "ParkingHistoryDB";

        try (MongoClient mongoClient = MongoClients.create(uri)) {
            MongoDatabase database = mongoClient.getDatabase(sourceDatabaseName);
            MongoCollection<Document> collection = database.getCollection(collectionName);

            //update to set closed to true and endTime to current time for matching elements in parkingEvents
            Bson update = Updates.combine(
                    Updates.set("parkingEvents.$[elem].closed", "true"),
                    Updates.set("parkingEvents.$[elem].endTime", this.getCurrentTime())
            );

            //we filter the collection based on combined conditions named "elem"
            UpdateOptions options = new UpdateOptions().arrayFilters(
                    java.util.Arrays.asList(
                            new Document("elem.vehicleID", vehicleID)
                                    .append("elem.closed", "false")
                    )
            );

            //update the event fields we want to stop, closed to true , endTime to current time
            collection.updateMany(new Document(), update, options);
            System.out.println("Documents updated with new endTime and closed status for vehicleID " + vehicleID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public synchronized boolean insertVehicleIDInRunningParkingEventsDB(String vehicleID) {
        String uri = "mongodb://localhost:27017";
        String usersCollectionName = "RunningParkingEventsDB";

        try (MongoClient mongoClient = MongoClients.create(uri)) {
            MongoDatabase database = mongoClient.getDatabase(sourceDatabaseName);

            //get the collection
            MongoCollection<Document> usersCollection = database.getCollection(usersCollectionName);

            //insert a new document with the specified vehicleID
            Document newDocument = new Document("vehicleID", vehicleID);
            usersCollection.insertOne(newDocument);

            //if the insertion was successful it return true
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            //if something went wrong that means the insertion not success
            return false;
        }
    }
    public synchronized boolean deleteVehicleID(String vehicleID) {
        String uri = "mongodb://localhost:27017";
        String usersCollectionName = "RunningParkingEventsDB";

        try (MongoClient mongoClient = MongoClients.create(uri)) {
            MongoDatabase database = mongoClient.getDatabase(sourceDatabaseName);

            //get the collection
            MongoCollection<Document> usersCollection = database.getCollection(usersCollectionName);

            //create a filter to find the document with the specified vehicleID
            Bson filter = Filters.eq("vehicleID", vehicleID);

            //delete the document that matches the filter
            DeleteResult deleteResult = usersCollection.deleteOne(filter);

            //check if the deletion was successful
            if (deleteResult.getDeletedCount() > 0) {
                //deletion successful
                return true;
            } else {
                //no document found with the given vehicleID, or deletion failed
                return false;
            }

        } catch (Exception e) {
            e.printStackTrace();
            //if something went wrong, we assume deletion not successful
            return false;
        }
    }
    public synchronized boolean addParkingEvent(String parkingSpaceNumber, Document parkingEvent) {
        String uri = "mongodb://localhost:27017";
        String collectionName = "ParkingHistoryDB";

        try (MongoClient mongoClient = MongoClients.create(uri)) {
            MongoDatabase database = mongoClient.getDatabase(sourceDatabaseName);

            //check if the collection exists
            if (!database.listCollectionNames().into(new ArrayList<>()).contains(collectionName)) {
                System.out.println("Collection does not exist: " + collectionName);
                return false;
            }

            //get the ParkingHistoryDB collection
            MongoCollection<Document> collection = database.getCollection(collectionName);


            //update the parking event
            //Document updateQuery = new Document("parkingSpaceNumber", parkingSpaceNumber);
            //Document updateCommand = new Document("$addToSet", new Document("parkingEvents", parkingEvent));
            Bson updateQuery = Filters.eq("parkingSpaceNumber", parkingSpaceNumber);
            Bson updateCommand = Updates.addToSet("parkingEvents", parkingEvent);

            collection.updateOne(updateQuery, updateCommand);

            System.out.println("Parking event added successfully.");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    public synchronized String getParkingEventsList(String vehicleID){
        String uri = "mongodb://localhost:27017";
        String collectionName = "ParkingHistoryDB";

        try (MongoClient mongoClient = MongoClients.create(uri)) {
            MongoDatabase database = mongoClient.getDatabase(sourceDatabaseName);

            //check if the UsersDB collection exists
            if (!database.listCollectionNames().into(new ArrayList<>()).contains(collectionName)) {
                System.out.println("Collection does not exist: " + collectionName);
                return "error";
            }

            //get the UsersDB collection
            MongoCollection<Document> collection = database.getCollection(collectionName);

            //build a query to find the user by username within the "UsersDB" array
            Document query = new Document();
            MongoCursor<Document> cursor = collection.find(query).iterator();

            List<Document> resultList = new ArrayList<>();
            double totalMoney = 0;
            while (cursor.hasNext()) {
                Document document = cursor.next();

                //extract only the specified fields
                List<Document> parkingEvents = (List<Document>) document.get("parkingEvents");
                for (Document parkingEvent : parkingEvents) {
                    if (vehicleID.equals(parkingEvent.getString("vehicleID")) && "true".equals(parkingEvent.getString("closed"))) {
                        LocalDateTime startTime = LocalDateTime.parse(parkingEvent.getString("startTime"), DateTimeFormatter.ISO_DATE_TIME);
                        LocalDateTime endTime = LocalDateTime.parse(parkingEvent.getString("endTime"), DateTimeFormatter.ISO_DATE_TIME);
                        long minutesBetween = java.time.Duration.between(startTime, endTime).toMinutes();
                        double hourlyRate = Double.parseDouble(document.getString("hourlyRate"));
                        double cost = (minutesBetween / 60.0) * hourlyRate;

                        totalMoney += cost;

                        Document extractedDocument = new Document();
                        extractedDocument.put("startTime", parkingEvent.getString("startTime"));
                        extractedDocument.put("endTime", parkingEvent.getString("endTime"));
                        extractedDocument.put("parkingSpaceNumber", document.getString("parkingSpaceNumber"));
                        resultList.add(extractedDocument);
                    }
                }
            }

            //convert the List<Document> to a JSON string using Gson
            String jsonString = new Gson().toJson(resultList);

            System.out.println("JSON String: " + jsonString);
            return (!jsonString.equals("[]") ? "sendParkingEventsList " + vehicleID + " " + jsonString + " " + totalMoney + "\n" : "sendParkingEventsListIsEmpty "+ vehicleID + "\n" );

        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }
    }
    public synchronized String getSpaceReportEvent(String parkingSpaceNumber){
        String uri = "mongodb://localhost:27017";
        String collectionName = "ParkingHistoryDB";

        try (MongoClient mongoClient = MongoClients.create(uri)) {
            MongoDatabase database = mongoClient.getDatabase(sourceDatabaseName);

            //check if the UsersDB collection exists
            if (!database.listCollectionNames().into(new ArrayList<>()).contains(collectionName)) {
                System.out.println("Collection does not exist: " + collectionName);
                return "error";
            }

            //get the UsersDB collection
            MongoCollection<Document> collection = database.getCollection(collectionName);
            // Find the document with the given parkingSpaceNumber
            Document document = collection.find(Filters.eq("parkingSpaceNumber", parkingSpaceNumber)).first();
            if (document == null) {
                System.out.println("No document found with parkingSpaceNumber: " + parkingSpaceNumber);
                return "sendEmptySpaceReport " + parkingSpaceNumber + "\n";
            }

            // Get the "parkingEvents" array as a List<Document>
            List<Document> parkingEvents = document.getList("parkingEvents", Document.class);

            // Filter for events where closed equals "false"
            List<String> vehicleIDs = parkingEvents.stream()
                    .filter(event -> "false".equals(event.getString("closed")))
                    .map(event -> event.getString("vehicleID"))
                    .distinct() //there is must not be duplicated rows just if we hard insert it
                    .collect(Collectors.toList());

            for (String vehicleID : vehicleIDs) {
                //we loop on each vehicleID that have in the start state and check if he is parking ok (same logic of PEO so we re-use it here)
                String checkVehicleMOResult = this.checkVehiclePastingMO(vehicleID,parkingSpaceNumber,getCurrentTime());
                if(checkVehicleMOResult.equals("ParkingDurationExceeded")){
                    //check if there is parking event for the customer in the Start state (vehicle is currently parked)
                    boolean isVehicleCurrentlyParked =this.isVehicleIDExistsInRunningParkingEventsDB(vehicleID);
                    if (isVehicleCurrentlyParked){
                        this.updateParkingEventsForVehicleID(vehicleID);
                        //delete vehicleID from RunningParkingEventsDB collection
                        this.deleteVehicleID(vehicleID);
                    }
                }
            }


            //build a query to find the user by username within the "RunningParkingEventsDB" array
            Bson eqDocumentToFindQuery = eq("parkingSpaceNumber", parkingSpaceNumber);
            Document eqDocumentToFind = collection.find(eqDocumentToFindQuery).first();

            //get the "updatedParkingEvents" array as a List<Document>
            List<Document> updatedParkingEvents = eqDocumentToFind.get("parkingEvents", List.class);

            //convert the List<Document> to a JSON string using Gson
            String updatedParkingEventsJson = new Gson().toJson(updatedParkingEvents);

            System.out.println("JSON String: " + updatedParkingEventsJson);
            return (!updatedParkingEventsJson.equals("[]") ? "sendSpaceReport " + parkingSpaceNumber + " " + updatedParkingEventsJson + "\n" : "sendEmptySpaceReport "+ parkingSpaceNumber + "\n" );

        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }
    }
    public synchronized String checkVehiclePEO(String vehicleID,String parkingSpaceNumber){
        String uri = "mongodb://localhost:27017";
        String collectionName = "ParkingHistoryDB";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        LocalDateTime currentTime = LocalDateTime.now();

        try (MongoClient mongoClient = MongoClients.create(uri)) {
            MongoDatabase database = mongoClient.getDatabase(sourceDatabaseName);

            //check if the UsersDB collection exists
            if (!database.listCollectionNames().into(new ArrayList<>()).contains(collectionName)) {
                System.out.println("Collection does not exist: " + collectionName);
                return "error";
            }

            //get the collection
            MongoCollection<Document> collection = database.getCollection(collectionName);

            //build a query to find the event within the "ParkingHistoryDB" collection
            Document query = new Document();
            MongoCursor<Document> cursor = collection.find(query).iterator();

            while (cursor.hasNext()) {
                Document document = cursor.next();

                if (!parkingSpaceNumber.equals(document.getString("parkingSpaceNumber"))) {
                    continue;
                }

                List<Document> parkingEvents = (List<Document>) document.get("parkingEvents");
                for (Document parkingEvent : parkingEvents) {
                    String closed = parkingEvent.getString("closed");
                    if (vehicleID.equals(parkingEvent.getString("vehicleID")) && "false".equals(closed)) {
                        LocalDateTime startTime = LocalDateTime.parse(parkingEvent.getString("startTime"), formatter);
                        LocalDateTime endTime = parkingEvent.getString("endTime").isEmpty() ? null : LocalDateTime.parse(parkingEvent.getString("endTime"), formatter);
                        int maxParkingMinutes = Integer.parseInt(document.getString("maxParkingMinutes"));
                        LocalDateTime maxEndTime = startTime.plusMinutes(maxParkingMinutes);
                        if (currentTime.isAfter(startTime) && currentTime.isBefore(maxEndTime)) {
                            System.out.println("The vehicle is currently parked within the valid parking time.");
                            return "ParkingOk";
                        } else if (currentTime.isAfter(maxEndTime)) {
                            System.out.println("The current time has passed the allowed parking duration.");
                            return "ParkingDurationExceeded";
                        } else {
                            System.out.println("The parking event is ongoing but not yet closed.");
                            return "ParkingNotOk";
                        }
                    }
                }
            }
            return "NoParkingEventFound";
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }
    }
    public synchronized String checkVehiclePastingMO(String vehicleID,String parkingSpaceNumber, String givenTimeStr){
        String uri = "mongodb://localhost:27017";
        String collectionName = "ParkingHistoryDB";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        LocalDateTime givenTime = LocalDateTime.parse(givenTimeStr, formatter);;

        try (MongoClient mongoClient = MongoClients.create(uri)) {
            MongoDatabase database = mongoClient.getDatabase(sourceDatabaseName);

            //check if the UsersDB collection exists
            if (!database.listCollectionNames().into(new ArrayList<>()).contains(collectionName)) {
                System.out.println("Collection does not exist: " + collectionName);
                return "error";
            }

            //get the collection
            MongoCollection<Document> collection = database.getCollection(collectionName);

            //build a query to find the event within the "ParkingHistoryDB" collection
            Document query = new Document();
            MongoCursor<Document> cursor = collection.find(query).iterator();

            while (cursor.hasNext()) {
                Document document = cursor.next();

                if (!parkingSpaceNumber.equals(document.getString("parkingSpaceNumber"))) {
                    continue;
                }

                List<Document> parkingEvents = (List<Document>) document.get("parkingEvents");
                for (Document parkingEvent : parkingEvents) {
                    String closed = parkingEvent.getString("closed");
                    if (vehicleID.equals(parkingEvent.getString("vehicleID")) && "false".equals(closed)) {
                        LocalDateTime startTime = LocalDateTime.parse(parkingEvent.getString("startTime"), formatter);
                        LocalDateTime endTime = parkingEvent.getString("endTime").isEmpty() ? null : LocalDateTime.parse(parkingEvent.getString("endTime"), formatter);
                        int maxParkingMinutes = Integer.parseInt(document.getString("maxParkingMinutes"));
                        LocalDateTime maxEndTime = startTime.plusMinutes(maxParkingMinutes);
                        if (givenTime.isAfter(startTime) && givenTime.isBefore(maxEndTime)) {
                            System.out.println("The vehicle is currently parked within the valid parking givenTimeStr.");
                            return "ParkingOk";
                        } else if (givenTime.isAfter(maxEndTime)) {
                            System.out.println("The current givenTimeStr has passed the allowed parking duration.");
                            return "ParkingDurationExceeded";
                        } else {
                            System.out.println("The parking event is ongoing but not yet closed.");
                            return "ParkingNotOk";
                        }
                    } else if (vehicleID.equals(parkingEvent.getString("vehicleID")) && "true".equals(closed)) {
                        LocalDateTime startTime = LocalDateTime.parse(parkingEvent.getString("startTime"), formatter);
                        LocalDateTime endTime = parkingEvent.getString("endTime").isEmpty() ? null : LocalDateTime.parse(parkingEvent.getString("endTime"), formatter);
                        int maxParkingMinutes = Integer.parseInt(document.getString("maxParkingMinutes"));
                        LocalDateTime maxEndTime = startTime.plusMinutes(maxParkingMinutes);
                        if (givenTime.isAfter(startTime) && givenTime.isBefore(endTime)) {
                            if (endTime.isBefore(maxEndTime)) {
                                System.out.println("The vehicle is currently parked within the valid parking givenTimeStr.");
                                return "ParkingOk";
                            } else if (endTime.isAfter(maxEndTime)) {
                                System.out.println("The current givenTimeStr has passed the allowed parking duration.");
                                return "ParkingNotOk";
                            }
                        }
                    }
                }
            }
            return "ParkingNotOk";
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }
    }

    public static String getSourceDatabaseName() {
        return sourceDatabaseName;
    }

    public static void setSourceDatabaseName(String sourceDatabaseName) {
        HandleDB.sourceDatabaseName = sourceDatabaseName;
    }
    private String getCurrentTime() {
        //get current date and time
        LocalDateTime currentDateTime = LocalDateTime.now();

        //change it to yyyy-MM-dd'T'HH:mm format
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

        //return yyyy-MM-dd'T'HH:mm format as string
        return currentDateTime.format(formatter);
    }

}




