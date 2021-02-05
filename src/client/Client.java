package client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.stage.Stage;
import res.Constants;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Client extends Application implements Constants {
    private DataInputStream fromServer;
    private DataOutputStream toServer;

    private char myToken;
    private char anotherToken;

    private int selectedRow;
    private int selectedCol;

    private boolean myTurn;
    private boolean gameContinue = true;
    private boolean waiting = true;

    private final Stage gameStage = new Stage();
    private final Label label = new Label();

    private final Cell[][] cells = new Cell[3][3];

    @Override
    public void start(Stage primaryStage) throws Exception {
        BorderPane connectPane = new BorderPane();
        HBox hBox = new HBox(10);
        TextField tfHost = new TextField();
        Button btSubmit = new Button("Connect");
        hBox.getChildren().addAll(tfHost, btSubmit);
        hBox.setAlignment(Pos.CENTER);
        Label note = new Label("Leave blank or input an IP");
        BorderPane.setAlignment(note, Pos.CENTER);
        connectPane.setBottom(note);
        connectPane.setCenter(hBox);

        tfHost.setOnAction(event -> btSubmit.fire());

        btSubmit.setOnAction(event -> {
            if (tfHost.getText().split("\\.").length != 4 && !tfHost.getText().isEmpty())
                return;

            btSubmit.setDisable(true);

            new Thread(() -> {
                try {
                    Socket socket = new Socket(tfHost.getText(), 14514);
                    fromServer = new DataInputStream(socket.getInputStream());
                    toServer = new DataOutputStream(socket.getOutputStream());
                } catch (IOException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
                Platform.runLater(() -> {
                    primaryStage.close();
                    makeGameStage();
                });
            }).start();
        });

        Scene scene = new Scene(connectPane, 300, 75);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.setTitle("Connect to server");
        primaryStage.show();
    }

    public void makeGameStage() {
        BorderPane borderPane = new BorderPane();
        GridPane pane = new GridPane();
        borderPane.setCenter(pane);
        borderPane.setBottom(label);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                cells[i][j] = new Cell(i, j);
                pane.add(cells[i][j], j, i);
            }
        }

        game();

        Scene scene = new Scene(borderPane, 300, 300);
        gameStage.setScene(scene);
        gameStage.setTitle("Game");
        gameStage.show();
    }

    //main game
    public void game() {
        //create a new thread
        new Thread(() -> {
            try {
                //intentionally stack to receive the notification from server
                int player = fromServer.readInt();

                //check which player I am
                if (player == PLAYER1) {
                    myToken = 'X';
                    anotherToken = 'O';
                    Platform.runLater(() -> gameStage.setTitle("PLAYER1 X"));
                    Platform.runLater(() -> label.setText("Waiting for player2 to join..."));
                    myTurn = true;

                    fromServer.readInt(); //wait to start
                    Platform.runLater(() -> label.setText("Game starts. You first."));
                } else if (player == PLAYER2) {
                    myToken = 'O';
                    anotherToken = 'X';
                    Platform.runLater(() -> gameStage.setTitle("PLAYER2 O"));
                    Platform.runLater(() -> label.setText("Game starts. Waiting for player1."));
                }

                while (gameContinue) {
                    if (player == PLAYER1) {
                        waitForOther();
                        send();
                        receive();
                    } else if (player == PLAYER2) {
                        receive();
                        waitForOther();
                        send();
                    }
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void waitForOther() throws InterruptedException {
        while (waiting) {
            Thread.sleep(50);
        }
        waiting = true;
    }

    public void send() throws IOException {
        toServer.writeInt(selectedRow);
        toServer.writeInt(selectedCol);
    }

    public void receiveMove() throws IOException {
        int row = fromServer.readInt();
        int col = fromServer.readInt();
        Platform.runLater(() -> cells[row][col].setToken(anotherToken));
    }

    public void receive() throws IOException {
        int status = fromServer.readInt();
        if (status == CONTINUE) {
            receiveMove();
            myTurn = true;
            Platform.runLater(() -> label.setText("Your turn"));
        } else if (status == DRAW) {
            waiting = false;
            gameContinue = false;
            if (myToken == 'O') receiveMove();
            Platform.runLater(() -> label.setText("Draw"));
        } else if (status == PLAYER1_WON) {
            waiting = false;
            gameContinue = false;
            switch (myToken) {
                case 'X': Platform.runLater(() -> label.setText("You win")); break;
                case 'O': receiveMove(); Platform.runLater(() -> label.setText("You lose"));
            }
        } else if (status == PLAYER2_WON) {
            waiting = false;
            gameContinue = false;
            switch (myToken) {
                case 'X': receiveMove(); Platform.runLater(() -> label.setText("You lose")); break;
                case 'O': Platform.runLater(() -> label.setText("You win"));
            }
        }
    }

    private class Cell extends Pane {
        private final int row;
        private final int col;

        private char token = ' ';

        public Cell(int row, int col) {
            this.row = row;
            this.col = col;
            setStyle("-fx-border-color: black");
            setPrefSize(1000, 1000);
            setOnMouseClicked(event -> mouseClickHandler());
        }

        public char getToken() {
            return token;
        }

        public void setToken(char token) {
            this.token = token;
            refresh();
        }

        public void refresh() {
            switch (getToken()) {
                case 'O': new O(this); break;
                case 'X': new X(this);
            }
        }

        public void mouseClickHandler() {
            if (token == ' ' && myTurn) {
                setToken(myToken);
                myTurn = false;
                selectedRow = row;
                selectedCol = col;
                waiting = false; //unlock waiting status
                label.setText("Waiting for another player to move...");
            }
        }
    }

    private static class O extends Ellipse {
        private final Pane pane;

        public O(Pane pane) {
            this.pane = pane;
            make();
            pane.getChildren().add(this);
        }

        private void make() {
            setFill(Color.WHITE);
            setStroke(Color.BLACK);
            centerXProperty().bind(pane.widthProperty().divide(2));
            centerYProperty().bind(pane.heightProperty().divide(2));
            radiusXProperty().bind(pane.widthProperty().divide(2).subtract(10));
            radiusYProperty().bind(pane.heightProperty().divide(2).subtract(10));
        }
    }

    private static class X {
        private final Pane pane;

        public X(Pane pane) {
            this.pane = pane;
            makeCross();
        }

        private void makeCross() {
            Line line1 = new Line(10, 10,
                    pane.getWidth() - 10, pane.getHeight() - 10);
            line1.endXProperty().bind(pane.widthProperty().subtract(10));
            line1.endYProperty().bind(pane.heightProperty().subtract(10));
            Line line2 = new Line(10, pane.getHeight() - 10,
                    pane.getWidth() - 10, 10);
            line2.startYProperty().bind(
                    pane.heightProperty().subtract(10));
            line2.endXProperty().bind(pane.widthProperty().subtract(10));

            pane.getChildren().addAll(line1, line2);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
