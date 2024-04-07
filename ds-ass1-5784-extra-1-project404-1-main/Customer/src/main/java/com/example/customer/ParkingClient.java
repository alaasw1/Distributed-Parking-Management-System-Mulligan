package com.example.customer;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class ParkingClient {
    private String serverIpAddressStr;
    private int serverPort;
    private InetAddress serverIpAddress;



    public ParkingClient(String serverIpAdress, int serverPort) {
        this.serverIpAddressStr = serverIpAdress;
        this.serverPort = serverPort;
    }

    public String startParking(String vehicleID, String parkingSpaceNumber){
        try {
            serverIpAddress = InetAddress.getByName(serverIpAddressStr);
        } catch (UnknownHostException e) {
            System.out.println("Illegal ip address: " + serverIpAddressStr);
        }


        try (Socket socket = new Socket(serverIpAddress, serverPort)) {
            //create output stream to send data to the server
            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);

            //create input stream to receive data from the server
            InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            //create startParking string for the server
            String startParkingStr = "startParking " + vehicleID + " " + parkingSpaceNumber + "\n";

            //send startParking string to the server
            writer.println(startParkingStr);

            //receive server response
            String serverResponse = reader.readLine();
            if (serverResponse == null) {
                System.out.println("No response from server");
                return "Server communication error.\n";
            }

            //close the resources in the end
            //for clarity no need to close socket because try-with-resources will automatically close it for us
            writer.close();
            reader.close();
            if (serverResponse.equals("parkingStarted " + vehicleID + " " + parkingSpaceNumber )){
                return "Parking Started\n";
            } else if(serverResponse.equals("invalidVehicleID " + vehicleID + " " + parkingSpaceNumber )){
                return "Invalid VehicleID.\n";
            } else if(serverResponse.equals("invalidParkingSpaceNumber " + vehicleID + " " + parkingSpaceNumber )){
                return "Invalid parkingSpaceNumber.\n";
            } else{
                System.out.println("Got wrong format from the server while doing start parking event:\n" + serverResponse);
                return "Server communication error.\n";
            }

        } catch (IOException e) {
            System.out.println("server not connected");
            return "server not connected\n";
        }
    }

    public String stopParking(String vehicleID){
        try {
            serverIpAddress = InetAddress.getByName(serverIpAddressStr);
        } catch (UnknownHostException e) {
            System.out.println("Illegal ip address: " + serverIpAddressStr);
        }


        try (Socket socket = new Socket(serverIpAddress, serverPort)) {
            //create output stream to send data to the server
            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);

            //create input stream to receive data from the server
            InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            //create startParking string for the server
            String stopParkingStr = "stopParking " + vehicleID + "\n";

            //send startParking string to the server
            writer.println(stopParkingStr);

            //receive server response
            String serverResponse = reader.readLine();
            if (serverResponse == null) {
                System.out.println("No response from server");
                return "Server communication error.\n";
            }

            //close the resources in the end
            //for clarity no need to close socket because try-with-resources will automatically close it for us
            writer.close();
            reader.close();

            if (serverResponse.equals("parkingStopped " + vehicleID )){
                return "Parking Stopped\n";
            } else if(serverResponse.equals("invalidVehicleID " + vehicleID )){
                return "Invalid VehicleID.\n";
            } else if (serverResponse.equals("thereIsNoParkingEvent " + vehicleID )){
                return "There is no open parking event.\n";
            }
            else{
                System.out.println("Got wrong format from the server while doing stop parking event:\n" + serverResponse);
                return "Server communication error.\n";
            }
        } catch (IOException e) {
            System.out.println("server not connected");
            return "server not connected\n";
        }
    }

    public String retrievingListOfParkingEvents(String vehicleID){
        try {
            serverIpAddress = InetAddress.getByName(serverIpAddressStr);
        } catch (UnknownHostException e) {
            System.out.println("Illegal ip address: " + serverIpAddressStr);
        }


        try (Socket socket = new Socket(serverIpAddress, serverPort)) {
            //create output stream to send data to the server
            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);

            //create input stream to receive data from the server
            InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            //create startParking string for the server
            String retrievingListOfParkingEventsStr = "getParkingEventsList " + vehicleID + "\n";

            //send startParking string to the server
            writer.println(retrievingListOfParkingEventsStr);

            //receive server response
            String serverResponse = reader.readLine();
            if (serverResponse == null) {
                System.out.println("No response from server");
                return "error";
            }

            //close the resources in the end
            //for clarity no need to close socket because try-with-resources will automatically close it for us
            writer.close();
            reader.close();

            if (serverResponse.startsWith("sendParkingEventsList " + vehicleID )){
                String[] serverResponseSplit = serverResponse.split(" ");
                String combinedLastTwoElements = serverResponseSplit[serverResponseSplit.length - 2] + " " + serverResponseSplit[serverResponseSplit.length - 1];
                return combinedLastTwoElements;
            } else if(serverResponse.equals("invalidVehicleID " + vehicleID )){
                return "invalidVehicleID";
            } else if (serverResponse.equals("sendParkingEventsListIsEmpty " + vehicleID )){
                return "sendParkingEventsListIsEmpty";
            }
            else{
                System.out.println("Got wrong format from the server while doing stop parking event:\n" + serverResponse);
                return "error";
            }
        } catch (IOException e) {
            System.out.println("server not connected");
            return "serverNotConnected";
        }
    }



}