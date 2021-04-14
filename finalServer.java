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
import javafx.application.Platform;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.FileInputStream;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import javafx.geometry.Insets;
import javafx.geometry.Pos;

import java.io.*;
import java.net.*;
import java.util.Vector;

public class finalServer extends Application
{
    Canvas canvas;
    Scene orderbookScene, priceScene, mainScene, loginScene;
    GraphicsContext orderbookGraphics, priceGraphics;
    Socket connection;
    finalServerThread priceOrderHandler;
    static ServerSocket serverSocket = null;
    static finalServerThread thread = null;

    public void paintOrderbook()
    {

    }

    public void paintPrices()
    {

    }

    @Override
    public void start(Stage primaryStage) throws Exception
    {
        primaryStage.setTitle("Depth of Market reader - Idle...");

        //Make price and orderbook forms and set context to globals

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
                try
                {
                    int Port = Integer.parseInt(portDisplay.getText());
                    serverSocket = new ServerSocket(Port);
                    System.out.println("[DEBUG] - Listening on " + Port + "...");
                    Runnable acceptThread = () -> 
                    {
                        Socket clientSocket = null;
                        try{
                            clientSocket = serverSocket.accept();
                        }
                        catch (IOException e){
                            System.out.println("Socket not accepted");
                        }
                        System.out.println("[DEBUG] - Connection Established!");
                        thread = new finalServerThread(clientSocket);
                        thread.start();
                        priceOrderHandler = thread;
                        connection = clientSocket;
                        primaryStage.setTitle("Final Project - Connected!");
                        primaryStage.setScene(mainScene);
                    };
                    Platform.runLater(acceptThread);
                }
                catch (IOException ex)
                {
                    ex.printStackTrace();
                }
            }
        });

        primaryStage.setTitle("Final Project - Idle...");
        loginScene = new Scene(myGrid, 300, 300);

        GridPane mainGrid = new GridPane();
        mainGrid.setAlignment(Pos.CENTER);
        mainGrid.setHgap(10);
        mainGrid.setVgap(10);
        Button showPrice = new Button("View Price");
        Button showOrders = new Button("View Orderbook");
        mainGrid.add(showPrice, 0, 1);
        mainGrid.add(showOrders, 0, 2);

        showPrice.setOnAction(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent actionEvent)
            {
                primaryStage.setTitle("Final Project - Price");
                primaryStage.setScene(priceScene);
            }
        });

        showOrders.setOnAction(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent actionEvent)
            {
                primaryStage.setTitle("Final Project - Orderbook");
                primaryStage.setScene(orderbookScene);
            }
        });

        mainScene = new Scene(mainGrid, 300, 300);

        primaryStage.setScene(loginScene);
        primaryStage.show();
    }

    public static void main(String[] args)
    {
        launch(args);
    }

}
