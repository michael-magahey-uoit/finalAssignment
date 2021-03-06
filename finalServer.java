import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.paint.Color;
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
import javafx.scene.layout.VBox;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import javafx.geometry.Insets;
import javafx.geometry.Pos;

import java.io.*;
import java.net.*;
import java.util.Vector;

public class finalServer extends Application
{
    //Declare static variables that will be modified outside of the main start method
    Canvas orderCanvas, pricesCanvas;
    Scene orderbookScene, pricesScene, mainScene, loginScene;
    GraphicsContext orderbookGraphics, priceGraphics;
    Socket connection;
    finalServerThread priceOrderHandler;
    static ServerSocket serverSocket = null;
    static finalServerThread thread = null;
    static float[] bidHistory = new float[200];
    static float[] askHistory = new float[200];
    static int historyIndex = 0;


    //These functions will be called by the socket thread to update the scene
    public void paintOrderbook(float[] price, float[] volume, String[] orderType)
    {
        int lastX = 0;

        //Get orderbook max orders, we will use this for the size of the rectangle
        float yMax = Float.MAX_VALUE;

        for (int i = 0; i < volume.length; i++)
        {
            yMax = Float.max(yMax, volume[i]);
        }
        //Ensure that the number of order locations matches the number of price points
        pricePositions = price.length;
        if (pricePositions != volume.length)
        {
            System.out.println("Error! Prices dont match volume count.");
            return;
        }

        //This finds the thickness of each rectangle with a 10px buffer between the rectangles
        int xBase = (int)(800 / price.length);
        xBase -= (price.length * 10);

        //Go through the volumes and print each rectangle respective to the order size. We change color based on the orders type (BUY/SELL)
        for (int i = 0; i < price.length; i++)
        {
            if (orderType[i] == "BOOK_TYPE_BUY")
            {
                orderbookGraphics.setStroke(Color.BLUE);
            }
            else if (orderType[i] == "BOOK_TYPE_SELL")
            {
                orderbookGraphics.setStroke(Color.RED);
            }
            //volume / max_vol = percentageHeight * height of window; gave it a 50px buffer on the top and bottom
            int height = (int)(volume[i] / yMax) * 700;
            orderbookGraphics.fillRect(lastX, 750, xBase, height);
            lastX = lastX + xBase + 10;
        }
        
    }

    public void paintPrices(float bid, float ask)
    {
        //History shift. This moves all the values to the right so that index 0 is the newest price point (this deletes the oldest value)
        for (int i = historyIndex; i > 0; i--)
        { 
            bidHistory[i] = bidHistory[i-1];
            askHistory[i] = askHistory[i-1];
        }
        //Set the new history
        bidHistory[0] = bid;
        askHistory[0] = ask;

        historyIndex++;

        //Get the min and max of the price points
        float yMax = Float.MIN_VALUE;
        float yMin = Float.MAX_VAULE;

        for (int i = 0; i < historyIndex; i++)
        {
            yMax = Float.max(yMax, bidHistory[i]);
            yMax = Float.max(yMax, askHistory[i]);

            yMin = Float.min(yMin, bidHistory[i]);
            yMin = Float.min(yMin, askHistory[i]);
        }

        //Print the bid price line
        priceGraphics.setStroke(Color.BLUE);
        //Much the same as the stock lab, we normalize the price then apply it to the height of the window
        //we set the x increment to the length of the window divided by the number of points in the history
        int prevX = 50;
        float norm = (bidHistory[0] - yMin) / (yMax - yMin);
        float prevY = 500 - (norm * 400);
        float incX = 450 / bidHistory.length;
        for (int i = 1; i < historyIndex; i++)
        {
            float num = (bidHistory[i] - yMin) / (yMax - yMin);
            float yPos = 500 - (num * 400);
            priceGraphics.strokeLine(prevX, prevY, (int)(prevX + incX), (int)yPos);
            prevX = prevX + (int)incX;
            prevY = (int)yPos;
        }

        //Print the ask price line
        priceGrahics.setStroke(Color.RED);
        //Same as the bid line, pretty self explainatory
        int prevX = 50;
        float norm = (askHistory[0] - yMin) / (yMax - yMin);
        float prevY = 500 - (norm * 400);
        float incX = 450 / askHistory.length;
        for (int i = 1; i < historyIndex; i++)
        {
            float num = (askHistory[i] - yMin) / (yMax - yMin);
            float yPos = 500 - (num * 400);
            priceGraphics.strokeLine(prevX, prevY, (int)(prevX + incX), (int)yPos);
            prevX = prevX + (int)incX;
            prevY = (int)yPos;
        }
    }


    //Below isn't commented much because its just the form creation which we did 1000 times in the labs. It's self explainatory
    @Override
    public void start(Stage primaryStage) throws Exception
    {
        primaryStage.setTitle("Depth of Market reader - Idle...");

        //Make price and orderbook forms and set context to globals
        //Return Button
        Button orderReturn = new Button("Back to Main");
        Button priceReturn = new Button("Back to Main");

        //Order Book
        VBox orderLayout = new VBox(40); 
        orderbookScene = new Scene(orderLayout, 1000, 800);
        primaryStage.setTitle("Order Book");

        Group orderRoot = new Group();
        orderCanvas = new Canvas();

        orderCanvas.widthProperty().bind(primaryStage.widthProperty());
        orderCanvas.heightProperty().bind(primaryStage.heightProperty());
        orderRoot.getChildren().add(orderCanvas);

        primaryStage.setScene(orderbookScene);

        orderLayout.getChildren().add(orderReturn);
        orderLayout.getChildren().add(orderCanvas);

        orderbookGraphics = orderCanvas.getGraphicsContext2D();

        //Prices
        VBox priceLayout = new VBox(40); 
        pricesScene = new Scene(priceLayout, 1000, 800);
        primaryStage.setTitle("Prices");

        Group priceRoot = new Group();
        pricesCanvas = new Canvas();

        pricesCanvas.widthProperty().bind(primaryStage.widthProperty());
        pricesCanvas.heightProperty().bind(primaryStage.heightProperty());
        priceRoot.getChildren().add(pricesCanvas);

        primaryStage.setScene(pricesScene);

        priceLayout.getChildren().add(priceReturn);
        priceLayout.getChildren().add(pricesCanvas);


        priceGraphics = pricesCanvas.getGraphicsContext2D();

        //Return Buttons

        orderReturn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                primaryStage.setScene(mainScene);
                System.out.println("Clicked on Back to Main");
            }
        });

        priceReturn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                primaryStage.setScene(mainScene);
                System.out.println("Clicked on Back to Main");
            }
        });

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
                    //We need to set this to a runnable so it doesn't freeze the UI while awaiting a connection (C# is so much better at this)
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
                    Platform.runLater(acceptThread); //This runs the thread but still allows it to interact with the primaryStage
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
                primaryStage.setScene(pricesScene);
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
