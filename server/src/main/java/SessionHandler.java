import res.Constants;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class SessionHandler implements Constants, Runnable {
    private final Socket player1;
    private final Socket player2;

    private final char[][] cell = new char[3][3];

    public SessionHandler(Socket player1, Socket player2) {
        this.player1 = player1;
        this.player2 = player2;
        //init
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                cell[i][j] = ' ';
            }
        }
    }

    @Override
    public void run() {
        try {
            //IO stream
            DataInputStream fromP1 = new DataInputStream(player1.getInputStream());
            DataInputStream fromP2 = new DataInputStream(player2.getInputStream());
            DataOutputStream toP1 = new DataOutputStream(player1.getOutputStream());
            DataOutputStream toP2 = new DataOutputStream(player2.getOutputStream());

            //notify player1
            toP1.writeInt(PLAYER1);

            int row, col;

            //start gaming
            while (true) {
                //player1's turn
                row = fromP1.readInt();
                col = fromP1.readInt();
                cell[row][col] = 'X';
                //check
                if (isFull()) {
                    toP1.writeInt(DRAW);
                    toP2.writeInt(DRAW);
                    send(toP2, row, col);
                    break;
                } else if (isWin('X')) {
                    toP1.writeInt(PLAYER1_WON);
                    toP2.writeInt(PLAYER1_WON);
                    send(toP2, row, col);
                    break;
                } else {
                    toP2.writeInt(CONTINUE);
                    send(toP2, row, col);
                }

                //player2's turn
                row = fromP2.readInt();
                col = fromP2.readInt();
                cell[row][col] = 'O';
                //check
                if (isFull()) {
                    toP1.writeInt(DRAW);
                    toP2.writeInt(DRAW);
                    send(toP1, row, col);
                    break;
                } else if (isWin('O')) {
                    toP1.writeInt(PLAYER2_WON);
                    toP2.writeInt(PLAYER2_WON);
                    send(toP1, row, col);
                    break;
                } else {
                    toP1.writeInt(CONTINUE);
                    send(toP1, row, col);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //methods
    public void send(DataOutputStream out, int row, int col) throws IOException {
        out.writeInt(row);
        out.writeInt(col);
    }

    public boolean isFull() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (cell[i][j] == ' ')
                    return false;
            }
        }

        return true;
    }

    public boolean isWin(char token) {
        //check col
        for (int col = 0; col < 3; col++) {
            if (cell[0][col] == token && cell[1][col] == token && cell[2][col] == token)
                return true;
        }
        //check row
        for (int row = 0; row < 3; row++) {
            if (cell[row][0] == token && cell[row][1] == token && cell[row][2] == token)
                return true;
        }
        //check diagonal
        return (cell[0][2] == token && cell[1][1] == token && cell[2][0] == token) ||
                (cell[0][0] == token && cell[1][1] == token && cell[2][2] == token);
    }
}
