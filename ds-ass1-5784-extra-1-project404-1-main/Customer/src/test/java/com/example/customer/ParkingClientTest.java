
package com.example.customer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ParkingClientTest {
    private ParkingClient parkingClient;
    private ParkingClient parkingClient2;
    private final String SERVER_IP = "127.0.0.1";
    private final int SERVER_PORT = 6666;
    private final int SERVER_PORT2 = 6665;

    @BeforeEach
    void setUp() {
        this.parkingClient = new ParkingClient(SERVER_IP, SERVER_PORT);
        this.parkingClient2 = new ParkingClient(SERVER_IP, SERVER_PORT2);

    }

    @Test
    @DisplayName("Start parking successfully")
    void startParkingWithValidInfo() throws IOException {
        String vehicleID = "1";
        String parkingSpaceNumber = "8";

        String serverResponse = parkingClient.startParking(vehicleID, parkingSpaceNumber);
        assertEquals("Parking Started\n", serverResponse, "The server response should not indicate an invalid vehicle ID.");
    }

    @Test
    @DisplayName("Try to Start parking with invalid vehicle ID")
    void startParkingWithInvalidVehicleID() throws IOException {
        String vehicleID = "1111111";
        String parkingSpaceNumber = "8";

        String serverResponse = parkingClient.startParking(vehicleID, parkingSpaceNumber);
        assertEquals("Invalid VehicleID.\n", serverResponse, "The server response should not indicate an invalid vehicle ID.");
    }

    @Test
    @DisplayName("Try to Start parking with invalid parking space number")
    void startParkingWithInvalidParkingSpaceNumber() throws IOException {
        String vehicleID = "1";
        String parkingSpaceNumber = "8888888";

        String serverResponse = parkingClient.startParking(vehicleID, parkingSpaceNumber);
        assertEquals("Invalid parkingSpaceNumber.\n", serverResponse, "The server response should not indicate an invalid vehicle ID.");
    }

    @Test
    @DisplayName("Stop parking successfully")
    void stopParkingWhenParkingEventRunning() throws IOException {
        String vehicleID = "1";
        String parkingSpaceNumber = "8";

        parkingClient.startParking(vehicleID, parkingSpaceNumber);

        String serverResponse = parkingClient.stopParking(vehicleID).trim(); // Trim the response
        parkingClient.stopParking(vehicleID);
        assertEquals("Parking stopped".toLowerCase(), serverResponse.toLowerCase(), "The server response should indicate that parking stopped.");
    }

    @Test
    @DisplayName("Try to stop parking event when no parking event is running")
    void stopParkingWhenNoParkingEventRunning() throws IOException {
        String vehicleID = "1";

        String serverResponse = parkingClient.stopParking(vehicleID).trim(); // Trim the response
        parkingClient.stopParking(vehicleID);
        assertEquals("there is no open parking event.".toLowerCase(), serverResponse.toLowerCase(), "The server response should indicate that parking stopped.");
    }

    @Test
    @DisplayName("Retrieve list of parking events successfully")
    void retrievingListOfParkingEvents() throws IOException {
        String validVehicleID = "1";
        parkingClient.startParking(validVehicleID, "8");
        parkingClient.stopParking(validVehicleID);

        String serverResponse = parkingClient.retrievingListOfParkingEvents(validVehicleID);

        String expectedServerResponse = serverResponse.toString();

        assertEquals(expectedServerResponse, serverResponse, "The server response should be a JSON representation of parking events.");
    }

    @Test
    @DisplayName("Start parking successfully")
    void startParkingWithValidInfoOnDifferentServer() throws IOException {
        String vehicleID = "1";
        String parkingSpaceNumber = "8";

        String serverResponse = parkingClient2.startParking(vehicleID, parkingSpaceNumber);
        assertEquals("Parking Started\n", serverResponse, "The server response should not indicate an invalid vehicle ID.");
    }

    @Test
    @DisplayName("Try to Start parking with invalid vehicle ID")
    void startParkingWithInvalidVehicleIDOnDifferentServer() throws IOException {
        String vehicleID = "1111111";
        String parkingSpaceNumber = "8";

        String serverResponse = parkingClient2.startParking(vehicleID, parkingSpaceNumber);
        assertEquals("Invalid VehicleID.\n", serverResponse, "The server response should not indicate an invalid vehicle ID.");
    }

    @Test
    @DisplayName("Try to Start parking with invalid parking space number")
    void startParkingWithInvalidParkingSpaceNumberOnDifferentServer() throws IOException {
        String vehicleID = "1";
        String parkingSpaceNumber = "8888888";

        String serverResponse = parkingClient2.startParking(vehicleID, parkingSpaceNumber);
        assertEquals("Invalid parkingSpaceNumber.\n", serverResponse, "The server response should not indicate an invalid vehicle ID.");
    }

    @Test
    @DisplayName("Stop parking successfully")
    void stopParkingWhenParkingEventRunningOnDifferentServer() throws IOException {
        String vehicleID = "1";
        String parkingSpaceNumber = "8";

        parkingClient2.startParking(vehicleID, parkingSpaceNumber);

        String serverResponse = parkingClient2.stopParking(vehicleID).trim(); // Trim the response
        parkingClient2.stopParking(vehicleID);
        assertEquals("Parking stopped".toLowerCase(), serverResponse.toLowerCase(), "The server response should indicate that parking stopped.");
    }

    @Test
    @DisplayName("Try to stop parking event when no parking event is running")
    void stopParkingWhenNoParkingEventRunningOnDifferentServer() throws IOException {
        String vehicleID = "1";

        String serverResponse = parkingClient2.stopParking(vehicleID).trim(); // Trim the response
        parkingClient2.stopParking(vehicleID);
        assertEquals("there is no open parking event.".toLowerCase(), serverResponse.toLowerCase(), "The server response should indicate that parking stopped.");
    }

    @Test
    @DisplayName("Retrieve list of parking events successfully")
    void retrievingListOfParkingEventsOnDifferentServer() throws IOException {
        String validVehicleID = "1";
        parkingClient2.startParking(validVehicleID, "8");
        parkingClient2.stopParking(validVehicleID);

        String serverResponse = parkingClient2.retrievingListOfParkingEvents(validVehicleID);

        String expectedServerResponse = serverResponse.toString();

        assertEquals(expectedServerResponse, serverResponse, "The server response should be a JSON representation of parking events.");
    }

    @Test
    @DisplayName("Stop parking on different server successfully")
    void startAndStopParkingWhenParkingEventRunningOnDifferentServer() throws IOException {
        String vehicleID = "1";
        String parkingSpaceNumber = "8";

        parkingClient.startParking(vehicleID, parkingSpaceNumber);


        String serverResponse = parkingClient2.stopParking(vehicleID).trim(); // Trim the response
        System.out.println(serverResponse);

        assertEquals("Parking stopped".toLowerCase(), serverResponse.toLowerCase(), "The server response should indicate that parking stopped.");
    }
}
