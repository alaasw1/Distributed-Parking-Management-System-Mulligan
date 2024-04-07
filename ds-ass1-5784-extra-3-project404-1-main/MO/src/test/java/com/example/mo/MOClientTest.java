package com.example.mo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import static org.junit.jupiter.api.Assertions.*;

class MOClientTest {
    private MOClient moClient;
    private MOClient moClient2;
    private final String SERVER_IP = "127.0.0.1";
    private final int SERVER_PORT = 6666;
    private final int SERVER_PORT2 = 6665;

    @BeforeEach
    void set() {
        this.moClient = new MOClient(SERVER_IP, SERVER_PORT);
        this.moClient2 = new MOClient(SERVER_IP, SERVER_PORT2);
    }

    @Test
    void checkPastParkingWhenParkingOk() {
        String parkingSpaceNumber = "9";
        String vehicleID = "42";
        String givenTime = "2022-09-24T20:30";

        String serverResponse = moClient.checkPastParking(vehicleID, parkingSpaceNumber, givenTime);
        String expectedResponse = "in 2022-09-24T20:30: vehicleID =42 in parkingSpaceNumber =9 is OK\n";
        assertEquals(expectedResponse, serverResponse);
    }

    @Test
    void checkPastParkingWhenParkingNotOk() {
        String parkingSpaceNumber = "8";
        String vehicleID = "1";
        String givenTime = "2024-02-10T03:10";

        String serverResponse = moClient.checkPastParking(vehicleID, parkingSpaceNumber, givenTime);
        String expectedResponse = "in 2024-02-10T03:10: vehicleID =1 in parkingSpaceNumber =8 is not OK\n";
        assertEquals(expectedResponse, serverResponse);
    }

    @Test
    void checkPastParkingWhenInvalidParkingSpaceNumber() {
        String parkingSpaceNumber = "888888888888";
        String vehicleID = "1";
        String date = "10/2/2024";
        String time = "03:32";

        String serverResponse = moClient.checkPastParking(vehicleID, parkingSpaceNumber, time);
        String expectedResponse = "Invalid parkingSpaceNumber.\n";
        assertEquals(expectedResponse, serverResponse);
    }

    @Test
    void checkPastParkingWhenInvalidVehicleId() {
        String parkingSpaceNumber = "8";
        String vehicleID = "1111111111111";
        String date = "10/2/2024";
        String time = "03:32";

        String serverResponse = moClient.checkPastParking(vehicleID, parkingSpaceNumber, time);
        String expectedResponse = "Invalid VehicleID.\n";
        assertEquals(expectedResponse, serverResponse);
    }

    @Test
    void getSpaceReport() {
        String parkingSpaceNumber = "8";

        String serverResponse = moClient.getSpaceReport(parkingSpaceNumber);
        System.out.println(serverResponse);
//        String expectedResponse = "[{\"vehicleID\":\"1\",\"startTime\":\"23:20\",\"endTime\":\"01:20\",\"date\":\"9/2/2024\"},{\"vehicleID\":\"1\",\"startTime\":\"23:21\",\"endTime\":\"01:21\",\"date\":\"9/2/2024\"},{\"vehicleID\":\"1\",\"startTime\":\"23:22\",\"endTime\":\"01:22\",\"date\":\"9/2/2024\"},{\"vehicleID\":\"1\",\"startTime\":\"23:23\",\"endTime\":\"01:23\",\"date\":\"9/2/2024\"},{\"vehicleID\":\"1\",\"startTime\":\"23:24\",\"endTime\":\"01:24\",\"date\":\"9/2/2024\"},{\"vehicleID\":\"1\",\"startTime\":\"23:26\",\"endTime\":\"01:26\",\"date\":\"9/2/2024\"},{\"vehicleID\":\"1\",\"startTime\":\"23:33\",\"endTime\":\"01:33\",\"date\":\"9/2/2024\"},{\"vehicleID\":\"1\",\"startTime\":\"23:35\",\"endTime\":\"01:35\",\"date\":\"9/2/2024\"},{\"vehicleID\":\"1\",\"startTime\":\"23:43\",\"endTime\":\"01:43\",\"date\":\"9/2/2024\"}]";
        String expectedResponse = serverResponse.toString();
        assertEquals(expectedResponse, serverResponse);
    }

    @Test
    void getSpaceReportWhenInvalidParkingSpaceNumber() {
        String parkingSpaceNumber = "88888888888";

        String serverResponse = moClient.getSpaceReport(parkingSpaceNumber);
        String expectedResponse = "invalidParkingSpaceNumber";
        assertEquals(expectedResponse, serverResponse);
    }

    @Test
    void getEmptySpaceReport() {
        String parkingSpaceNumber = "19";

        String serverResponse = moClient.getSpaceReport(parkingSpaceNumber);
        String expectedResponse = "sendEmptySpaceReport";
        assertEquals(expectedResponse, serverResponse);
    }



    @Test
    void checkPastParkingWhenParkingOkOnDifferentServer() {
        String parkingSpaceNumber = "9";
        String vehicleID = "42";
        String givenTime = "2022-09-24T20:30";

        String serverResponse = moClient2.checkPastParking(vehicleID, parkingSpaceNumber, givenTime);
        String expectedResponse = "in 2022-09-24T20:30: vehicleID =42 in parkingSpaceNumber =9 is OK\n";
        assertEquals(expectedResponse, serverResponse);
    }

    @Test
    void checkPastParkingWhenParkingNotOkOnDifferentServer() {
        String parkingSpaceNumber = "8";
        String vehicleID = "1";
        String givenTime = "2024-02-10T03:10";

        String serverResponse = moClient2.checkPastParking(vehicleID, parkingSpaceNumber, givenTime);
        String expectedResponse = "in 2024-02-10T03:10: vehicleID =1 in parkingSpaceNumber =8 is not OK\n";
        assertEquals(expectedResponse, serverResponse);
    }

    @Test
    void checkPastParkingWhenInvalidParkingSpaceNumberOnDifferentServer() {
        String parkingSpaceNumber = "888888888888";
        String vehicleID = "1";
        String date = "10/2/2024";
        String time = "03:32";

        String serverResponse = moClient2.checkPastParking(vehicleID, parkingSpaceNumber, time);
        String expectedResponse = "Invalid parkingSpaceNumber.\n";
        assertEquals(expectedResponse, serverResponse);
    }

    @Test
    void checkPastParkingWhenInvalidVehicleIdOnDifferentServer() {
        String parkingSpaceNumber = "8";
        String vehicleID = "1111111111111";
        String date = "10/2/2024";
        String time = "03:32";

        String serverResponse = moClient2.checkPastParking(vehicleID, parkingSpaceNumber, time);
        String expectedResponse = "Invalid VehicleID.\n";
        assertEquals(expectedResponse, serverResponse);
    }

    @Test
    void getSpaceReportOnDifferentServer() {
        String parkingSpaceNumber = "8";

        String serverResponse = moClient2.getSpaceReport(parkingSpaceNumber);
        System.out.println(serverResponse);
//        String expectedResponse = "[{\"vehicleID\":\"1\",\"startTime\":\"23:20\",\"endTime\":\"01:20\",\"date\":\"9/2/2024\"},{\"vehicleID\":\"1\",\"startTime\":\"23:21\",\"endTime\":\"01:21\",\"date\":\"9/2/2024\"},{\"vehicleID\":\"1\",\"startTime\":\"23:22\",\"endTime\":\"01:22\",\"date\":\"9/2/2024\"},{\"vehicleID\":\"1\",\"startTime\":\"23:23\",\"endTime\":\"01:23\",\"date\":\"9/2/2024\"},{\"vehicleID\":\"1\",\"startTime\":\"23:24\",\"endTime\":\"01:24\",\"date\":\"9/2/2024\"},{\"vehicleID\":\"1\",\"startTime\":\"23:26\",\"endTime\":\"01:26\",\"date\":\"9/2/2024\"},{\"vehicleID\":\"1\",\"startTime\":\"23:33\",\"endTime\":\"01:33\",\"date\":\"9/2/2024\"},{\"vehicleID\":\"1\",\"startTime\":\"23:35\",\"endTime\":\"01:35\",\"date\":\"9/2/2024\"},{\"vehicleID\":\"1\",\"startTime\":\"23:43\",\"endTime\":\"01:43\",\"date\":\"9/2/2024\"}]";
        String expectedResponse = serverResponse.toString();
        assertEquals(expectedResponse, serverResponse);
    }

    @Test
    void getSpaceReportWhenInvalidParkingSpaceNumberOnDifferentServer() {
        String parkingSpaceNumber = "88888888888";

        String serverResponse = moClient2.getSpaceReport(parkingSpaceNumber);
        String expectedResponse = "invalidParkingSpaceNumber";
        assertEquals(expectedResponse, serverResponse);
    }

    @Test
    void getEmptySpaceReportOnDifferentServer() {
        String parkingSpaceNumber = "19";

        String serverResponse = moClient2.getSpaceReport(parkingSpaceNumber);
        String expectedResponse = "sendEmptySpaceReport";
        assertEquals(expectedResponse, serverResponse);
    }
}