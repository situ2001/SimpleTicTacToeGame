package server;

import res.Constants;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

public class Server implements Constants {
    public static void printWithDate(String msg) {
        System.out.println("[" + new Date() + "]" + " -- " + msg);
    }
    
    public static void main(String[] args) {
        try {
            //preparation
            ServerSocket serverSocket = new ServerSocket(14514);
            printWithDate("Server starts, listening at port 14514...");

            while (true) {
                //wait for player1
                Socket player1 = serverSocket.accept();
                printWithDate("Player1 joined, waiting for player2...");
                //notify
                new DataOutputStream(player1.getOutputStream()).writeInt(PLAYER1);

                //the same
                Socket player2 = serverSocket.accept();
                printWithDate("Player2 joined.");
                new DataOutputStream(player2.getOutputStream()).writeInt(PLAYER2);

                printWithDate("The game starts.");
                printWithDate("Waiting for other 2 players to start a new game...");
                
                //create a SessionHandler for player1 & player2
                new Thread(new SessionHandler(player1, player2)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
