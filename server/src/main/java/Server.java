import res.Constants;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;

public class Server implements Constants {
    public static void main(String[] args) {
        Logger logger = Logger.getGlobal();
        
        try {
            //preparation
            ServerSocket serverSocket = new ServerSocket(14514);
            logger.info("Server starts, listening at port 14514...");

            while (true) {
                //wait for player1
                Socket player1 = serverSocket.accept();
                logger.info("Player1 joined, waiting for player2...");
                //notify
                new DataOutputStream(player1.getOutputStream()).writeInt(PLAYER1);

                //the same
                Socket player2 = serverSocket.accept();
                logger.info("Player2 joined.");
                new DataOutputStream(player2.getOutputStream()).writeInt(PLAYER2);

                logger.info("The game starts.");
                logger.info("Waiting for other 2 players to start a new game...");
                
                //create a SessionHandler for player1 & player2
                new Thread(new SessionHandler(player1, player2)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
