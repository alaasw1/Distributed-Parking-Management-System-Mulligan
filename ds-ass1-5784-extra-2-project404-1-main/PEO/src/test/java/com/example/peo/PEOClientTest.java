package com.example.peo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PEOClientTest {
    private PEOClient peoClient;
    private PEOClient peoClient2;
    private final String SERVER_IP = "127.0.0.1";
    private final int SERVER_PORT = 6666;
    private final int SERVER_PORT2 = 6665;

    @BeforeEach
    void setUp() {
        this.peoClient = new PEOClient(SERVER_IP, SERVER_PORT);
        this.peoClient2 = new PEOClient(SERVER_IP, SERVER_PORT2);
    }

    @Test
    void investigatingParkedVehicleWhenParkingEventIsRunning() {
        String vehicleID = "1";
        String parkingSpaceNumber = "8";

        String serverResponse = peoClient.investigatingParkedVehicle(vehicleID, parkingSpaceNumber);
        String expectedResponse = "vehicleID =1 in parkingSpaceNumber =8 is OK\n";
        assertEquals(expectedResponse, serverResponse);
    }

    @Test
    void investigatingParkedVehicleWhenNoParkingEventIsRunning() {
        String vehicleID = "2";
        String parkingSpaceNumber = "8";

        String serverResponse = peoClient.investigatingParkedVehicle(vehicleID, parkingSpaceNumber);
        String expectedResponse = "vehicleID =2 in parkingSpaceNumber =8 is not OK\n";
        assertEquals(expectedResponse, serverResponse);
    }

    @Test
    void investigatingParkedVehicleWhenVehicleIDIsInvalid() {
        String vehicleID = "11111111111";
        String parkingSpaceNumber = "8";

        String serverResponse = peoClient.investigatingParkedVehicle(vehicleID, parkingSpaceNumber);
        String expectedResponse = "Invalid VehicleID.\n";
        assertEquals(expectedResponse, serverResponse);
    }

    @Test
    void investigatingParkedVehicleWhenParkingSpaceNumberIsInvalid() {
        String vehicleID = "1";
        String parkingSpaceNumber = "8888888888";

        String serverResponse = peoClient.investigatingParkedVehicle(vehicleID, parkingSpaceNumber);
        String expectedResponse = "Invalid parkingSpaceNumber.\n";
        assertEquals(expectedResponse, serverResponse);
    }



    @Test
    void investigatingParkedVehicleWhenParkingEventIsRunningOnDifferentServer() {
        String vehicleID = "1";
        String parkingSpaceNumber = "8";

        String serverResponse = peoClient2.investigatingParkedVehicle(vehicleID, parkingSpaceNumber);
        String expectedResponse = "vehicleID =1 in parkingSpaceNumber =8 is OK\n";
        assertEquals(expectedResponse, serverResponse);
    }

    @Test
    void investigatingParkedVehicleWhenNoParkingEventIsRunningOnDifferentServer() {
        String vehicleID = "2";
        String parkingSpaceNumber = "8";

        String serverResponse = peoClient2.investigatingParkedVehicle(vehicleID, parkingSpaceNumber);
        String expectedResponse = "vehicleID =2 in parkingSpaceNumber =8 is not OK\n";
        assertEquals(expectedResponse, serverResponse);
    }

    @Test
    void investigatingParkedVehicleWhenVehicleIDIsInvalidOnDifferentServer() {
        String vehicleID = "11111111111";
        String parkingSpaceNumber = "8";

        String serverResponse = peoClient2.investigatingParkedVehicle(vehicleID, parkingSpaceNumber);
        String expectedResponse = "Invalid VehicleID.\n";
        assertEquals(expectedResponse, serverResponse);
    }

    @Test
    void investigatingParkedVehicleWhenParkingSpaceNumberIsInvalidOnDifferentServer() {
        String vehicleID = "1";
        String parkingSpaceNumber = "8888888888";

        String serverResponse = peoClient2.investigatingParkedVehicle(vehicleID, parkingSpaceNumber);
        String expectedResponse = "Invalid parkingSpaceNumber.\n";
        assertEquals(expectedResponse, serverResponse);
    }

}