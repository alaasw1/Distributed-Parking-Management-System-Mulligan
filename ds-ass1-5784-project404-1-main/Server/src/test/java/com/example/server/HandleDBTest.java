package com.example.server;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HandleDBTest {

    private HandleDB handleDB;

    @BeforeEach
    void setUp() {
        handleDB = new HandleDB();

//        // Pre-load the UsersDB with test data
//        handleDB.loadDataToMongoDB("C:\\Users\\User\\Desktop\\Distributed system project\\ds-ass2-5784-project404-1\\P1\\ds-ass1-5784-project404-1-main\\Server\\src\\main\\resources\\UsersDB.json");
//
//        // Pre-load the ParkingHistoryDB with test data
//        handleDB.loadDataToMongoDB("C:\\Users\\User\\Desktop\\Distributed system project\\ds-ass2-5784-project404-1\\P1\\ds-ass1-5784-project404-1-main\\Server\\src\\main\\resources\\ParkingHistoryDB.json");
//
//        // Pre-load the RunningParkingEventsDB with test data
//        handleDB.loadDataToMongoDB("C:\\Users\\User\\Desktop\\Distributed system project\\ds-ass2-5784-project404-1\\P1\\ds-ass1-5784-project404-1-main\\Server\\src\\main\\resources\\RunningParkingEventsDB.json");
    }



    @AfterEach
    void tearDown() {

        String uri = "mongodb://localhost:27017";
        String databaseName = "MulliganDB";
        String collectionName = "RunningParkingEventsDB";

        try (MongoClient mongoClient = MongoClients.create(uri)) {
            MongoDatabase database = mongoClient.getDatabase(databaseName);
            MongoCollection<Document> collection = database.getCollection(collectionName);
            collection.deleteMany(new Document()); // Delete all documents in just this collection
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    @Test
    void isVehicleIDExists() {
        assertTrue(handleDB.isVehicleIDExists("1"), "Vehicle ID 1 should exist in the UsersDB collection.");
    }

    @Test
    void isVehicleNotExists(){
        assertFalse(handleDB.isVehicleIDExists("non_existing_vehicle_id"), "Non-existing vehicle ID should not be found in the UsersDB collection.");
    }

    @Test
    void isParkingSpaceExists() {
        assertTrue(handleDB.isParkingSpaceExists("1"), "Parking space 1 should exist in the ParkingHistoryDB collection.");
    }

    @Test
    void isParkingSpaceNotExists() {
        assertFalse(handleDB.isParkingSpaceExists("non_existing_space"), "Non-existing parking space should not be found in the ParkingHistoryDB collection.");
    }

    @Test
    void isVehicleIDExistsInRunningParkingEventsDB() {
        // Assuming vehicle ID "1" has started parking and is present in RunningParkingEventsDB
        handleDB.insertVehicleIDInRunningParkingEventsDB("1");
        assertTrue(handleDB.isVehicleIDExistsInRunningParkingEventsDB("1"), "Vehicle ID 1 should be found in the RunningParkingEventsDB collection.");
    }

    @Test
    void insertVehicleIDInRunningParkingEventsDB() {
        assertTrue(handleDB.insertVehicleIDInRunningParkingEventsDB("2"), "Inserting vehicle ID 2 should succeed.");
        assertTrue(handleDB.isVehicleIDExistsInRunningParkingEventsDB("2"), "Vehicle ID 2 should now exist in the RunningParkingEventsDB collection.");
    }

    @Test
    void deleteVehicleID() {
        handleDB.insertVehicleIDInRunningParkingEventsDB("3");
        assertTrue(handleDB.deleteVehicleID("3"), "Deletion of vehicle ID 3 should succeed.");
        assertFalse(handleDB.isVehicleIDExistsInRunningParkingEventsDB("3"), "Vehicle ID 3 should no longer exist in the RunningParkingEventsDB collection after deletion.");
    }

    @Test
    void addParkingEvent() {
        assertTrue(handleDB.isParkingSpaceExists("10"), "Parking space 10 should exist.");

        Document parkingEvent = new Document("vehicleID", "4")
                .append("startTime", "09:00")
                .append("endTime", "11:00")
                .append("date", "2024-02-10");
        assertTrue(handleDB.addParkingEvent("10", parkingEvent), "Adding a parking event should succeed.");
    }

    @Test
    void getParkingEventsList() {
        String eventsList = handleDB.getParkingEventsList("1");
        assertNotNull(eventsList, "Parking events list should not be null.");
        assertFalse(eventsList.contains("error"), "Parking events list should not contain an error.");
    }

    @Test
    void getSpaceReportEvent() {
        // Assuming parking space "10" has a report
        String spaceReport = handleDB.getSpaceReportEvent("10");
        assertNotNull(spaceReport, "Space report should not be null.");
        assertFalse(spaceReport.contains("error"), "Space report should not contain an error.");
    }
}


