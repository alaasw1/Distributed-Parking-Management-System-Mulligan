package com.example.server;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
public class ServerTalker  {
    private static String targetServerIpAddressStr;
    private static int targetServerPort;
    private static InetAddress targetServerIpAddress;
    private String sourceDatabaseName;
    private String lastSyncDate;

    public ServerTalker(String targetServerIpAddressStr, int targetServerPort, String sourceDatabaseName, String lastSyncDate) {
        ServerTalker.targetServerIpAddressStr = targetServerIpAddressStr;
        ServerTalker.targetServerPort = targetServerPort;
        this.sourceDatabaseName = sourceDatabaseName;
        this.lastSyncDate = lastSyncDate;
    }
    public String syncTargetedServer(){
        try {
            targetServerIpAddress = InetAddress.getByName(targetServerIpAddressStr);
        } catch (UnknownHostException e) {
            System.out.println("Illegal ip address: " + targetServerIpAddressStr);
        }


        try (Socket socket = new Socket(targetServerIpAddress, targetServerPort)) {
            //create output stream to send data to the server
            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);

            //create input stream to receive data from the server
            InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            //create syncTargetedServerStr string for the server
            String syncTargetedServerStr = "sync " + sourceDatabaseName + " "+ lastSyncDate + "\n";

            //send syncTargetedServerStr string to the server
            writer.println(syncTargetedServerStr);

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
            String[] targetedServerMessageSplit = serverResponse.split(" ");
            if (targetedServerMessageSplit[0].equals("syncStarted")){
                return targetedServerMessageSplit[1] + " " + targetedServerMessageSplit[2];
            }if (targetedServerMessageSplit[0].equals("syncAfterCopy")){
                return targetedServerMessageSplit[1] + " " + targetedServerMessageSplit[2];
            }else if (targetedServerMessageSplit[0].equals("syncFailed")){
                return "sync Failed\n";
            }else{
                System.out.println("something went wrong while doing sync\n" + serverResponse);
                return "Server communication error.\n";
            }

        } catch (IOException e) {
            System.out.println("server not connected");
            return "server not connected\n";
        }
    }




}
