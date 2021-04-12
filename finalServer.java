import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.Group;
import javafx.stage.Stage;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import java.io.*;
import java.net.*;
import java.util.Vector;

public class finalServer extends Application
{
    Canvas canvas;
    Scene orderbookScene, priceScene, mainScene, loginScene;
    Socket connection;
    finalServerThread priceOrderHandler;
    @Override
    public void start(Stage primaryStage) throws Exception
    {
        primaryStage.setTitle("Depth of Market reader - Idle...");

        GridPane myGrid = new GridPane();
        myGrid.setAlignment(Pos.CENTER);
        myGrid.setHgap(10);
        myGrid.setVgap(10);
        myGrid.setPadding(new Insets(25, 25, 25, 25));

        Label portLabel = new Label("Port: ");
        TextField portDisplay = new TextField();
        Button btListen = new Button("Listen");
        myGrid.add(portLabel, 0, 1);
        myGrid.add(portDisplay, 1, 1);
        myGrid.add(btListen, 0, 2);
        btListen.setOnAction(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent actionEvent)
            {
                Socket serverSocket = null;
                finalServerThread thread = null;
                try
                {
                    int Port = 6800;
                    serverSocket = new ServerSocket(Port);
                    System.out.println("[DEBUG] - Listening on " + Port + "...");
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("[DEBUG] - Connection Established!");
                    thread = new finalServerThread(clientSocket, this);
                    thread.start();
                    priceOrderHandler = thread;
                    connection = clientSocket;
                }
                catch (IOException ex)
                {
                    ex.printStackTrace();
                }
                primaryStage.setTitle("Final Project - Connected!");
                primaryStage.setScene(mainScene);
            }
        });

        primaryStage.setTitle("Final Project - Idle...");
        loginScene = new Scene(myGrid, 300, 300);
        primaryStage.setScene(loginScene);

        GridPane mainGrid = new GridPane();
        mainGrid.setAlignment(Pos.CENTER);
        mainGrid.setHgap(10);
        mainGrid.setVgap(10);
        Button showPrice = new Button("View Price");
        Button showOrders = new Button("View Orderbook");
        mainGrid.add(showPrice, 0, 1);
        mainGrid.add(showOrders, 0, 2);

        mainScene = new Scene(mainGrid, 300, 300);



        primaryStage.setScene(loginScene);
        primaryStage.show();
    }

    public static voide main(String[] args)
    {
        launch(args);
    }

}