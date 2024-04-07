package com.example.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerListener extends Thread{
    private ServerSocket serverSocket;
    private Socket clientSocket;

    public ServerListener(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }
    @Override
    public void run(){
        //start to listen on the server socket
        System.out.println("Listening on " + serverSocket.getInetAddress().toString() + ":" + serverSocket.getLocalPort());
        while (!interrupted()) {
            try {
                //accepting new connctions
                this.clientSocket = this.serverSocket.accept();
                //start a thread to handle each client
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                System.out.println("started client handler!!!");
                clientHandler.start();
                try {
                    clientHandler.join();
                } catch (InterruptedException e) {
                    System.out.println("ServerListener:interrupted will waiting for handleClient thread!");
                    //e.printStackTrace();
                    break;
                }
            } catch (IOException e) {
                //handle any exceptions that might occur during connection
                System.out.println("Server exception: " + e.getMessage());
                //e.printStackTrace();
            }
            finally {
                if (clientSocket != null && !clientSocket.isClosed()) {
                    try {
                        this.clientSocket.close();
                    } catch (IOException e) {
                        //handle any exceptions that might occur during connection
                        System.out.println("Server exception: " + e.getMessage());
                        e.printStackTrace();
                    }
                }

            }

            if (isInterrupted()) {
                System.out.println("ServerListener:interrupted");
                break;
            }
        }

        if (this.serverSocket != null && !this.serverSocket.isClosed()) {
            try {
                System.out.println("closing server socket");
                this.serverSocket.close();

            } catch (IOException e) {
                //handle any exceptions that might occur during connection
                System.out.println("Server listener exception: " + e.getMessage());
                e.printStackTrace();
            }
        }

    }

}
