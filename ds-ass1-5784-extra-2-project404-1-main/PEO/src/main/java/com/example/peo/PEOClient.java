package com.example.peo;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
public class PEOClient {
    private String serverIpAddressStr;
    private int serverPort;

    private InetAddress serverIpAddress;

    public PEOClient(String serverIpAddressStr, int serverPort) {
        this.serverIpAddressStr = serverIpAddressStr;
        this.serverPort = serverPort;
    }



    public String investigatingParkedVehicle(String vehicleID, String parkingSpaceNumber){
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
            String startParkingStr = "checkVehicle " + vehicleID + " " + parkingSpaceNumber + "\n";

            //send startParking string to the server
            writer.println(startParkingStr);

            //receive server response
            String serverResponse = reader.readLine();

            //close the resources in the end
            //for clarity no need to close socket because try-with-resources will automatically close it for us
            writer.close();
            reader.close();

            if (serverResponse.equals("checkVehicleOk " + vehicleID + " " + parkingSpaceNumber )){
                return "vehicleID =" + vehicleID + " in parkingSpaceNumber ="+ parkingSpaceNumber + " is OK\n";
            }
            else if(serverResponse.equals("checkVehicleNotOk " + vehicleID + " " + parkingSpaceNumber )){
                return "vehicleID =" + vehicleID + " in parkingSpaceNumber ="+ parkingSpaceNumber + " is not OK\n";
            }else if(serverResponse.equals("invalidVehicleID " + vehicleID + " " + parkingSpaceNumber )){
                return "Invalid VehicleID.\n";
            }
            else if(serverResponse.equals("invalidParkingSpaceNumber " + vehicleID + " " + parkingSpaceNumber )){
                return "Invalid parkingSpaceNumber.\n";
            }else{
                System.out.println("Got wrong format from the server while doing start parking event:\n" + serverResponse);
                return "Server communication error.\n";
            }

        } catch (IOException e) {
            System.out.println("server not connected");
            return "server not connected\n";
        }
    }

}
