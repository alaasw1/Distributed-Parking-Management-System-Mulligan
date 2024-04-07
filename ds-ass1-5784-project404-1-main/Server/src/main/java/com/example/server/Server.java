package com.example.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;

public class Server extends Thread{
    private String serverIpAddressStr;
    private int serverPort;
    private InetAddress serverIpAddress;
    private ServerSocket serverSocket;
    private ServerListener serverListener;

    public Server(String serverIpAddressStr, int serverPort) {
        this.serverIpAddressStr = serverIpAddressStr;
        this.serverPort = serverPort;
    }

    @Override
    public void run(){
        startServer();
    }
    private void startServer(){
        while (!interrupted()) {
            try {
                serverIpAddress = InetAddress.getByName(serverIpAddressStr);
            } catch (UnknownHostException e) {
                System.out.println("Illegal ip address: " + serverIpAddressStr);
            }


            try (ServerSocket serverSocket = new ServerSocket(this.serverPort)) {
                this.serverSocket = serverSocket;
                this.serverListener = new ServerListener(this.serverSocket);
                //this.serverListener.setDaemon(true);
                System.out.println("started server listener!!!");
                this.serverListener.start();

                try {
                    this.serverListener.join();
                } catch (InterruptedException e) {
                    System.out.println("Server:interrupted will waiting for ServerListener thread!");
                    //e.printStackTrace();
                    break;
                }

            } catch (IOException e) {
                //handle any exceptions that might occur during connection
                System.out.println("Server exception: " + e.getMessage());
                e.printStackTrace();
            } finally {
                this.serverListener.interrupt();
            }

            if (isInterrupted()) {
                System.out.println("Server:interrupted ");
                break;
            }
        }
        this.serverListener.interrupt();
    }



}
