import java.io.*;
import java.net.*;
import java.util.*;

public class finalServerThread extends Thread
{
    //universal variables for the thread
    protected Socket socket     = null;
    protected PrintWriter out   = null;
    protected BufferedReader in = null;
    protected finalServer callback = null;

    public finalServerThread(Socket socket, finalServer callback)
    {
        super();
        this.socket = socket;
        //callback is what we use to set the scenes, it is the form that created the thread
        this.callback = callback;
        try
        {
            //Create the in and out streams, (we didn't really use the out stream, but if we had more time we would use it for errors and stuff)
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
    }

    public void run()
    {
        boolean endOfSession = false;
        while (!endOfSession)
        {
            //Keep receiving data until the socket is closed
            endOfSession = processData();
        }
        try 
        {
            socket.close();
            System.out.println("[DEBUG] - Socket Closed! Session Ended.");
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
    }

    protected boolean processData()
    {
        String message = null;
        try
        {
            //Read and process the message
            message = in.readLine();
            String[] packetBacklog = message.split("[ ]"); //This fixes a backlog issue where multiple packets get stacked in the stream
            for (String backlogMessage : packetBacklog) //go through the backlogged packets
            {
                //System.out.println("[backlog]: " + backlogMessage);
                String[] packet = backlogMessage.split("[|]"); //This splits the message into its respective parts
                String symbol = packet[0];
                String type = packet[1];
                if (type.equals("Tick")) //if the message is a tick update then we print the new price graph
                {
                    float bid = Float.parseFloat(packet[2]);
                    float ask = Float.parseFloat(packet[3]);
                    System.out.println("New Price! Bid: " + bid + " | Ask: " + ask);
                    callback.paintPrices(bid, ask);
                }
                else if (type.equals("OrderBook"))  //If it is a orderbook update, then we print the orderbook
                {
                    String[] orderbook = packet[2].split("[!]");
                    float[] prices = new float[orderbook.length];
                    float[] volumes = new float[orderbook.length];
                    String[] orderTypes = new String[orderbook.length];
                    int counter = 0;
                    for (String pricePoint : orderbook)
                    {
                        String[] data = pricePoint.split("[-]");
                        prices[counter] = Float.parseFloat(data[0]);
                        volumes[counter] = Float.parseFloat(data[1]);
                        orderTypes[counter] = data[2];
                        counter++;
                    }
                    callback.paintOrderbook(prices, volumes, orderTypes);
                }
                else{                           //If it is a unknown type, we just print the type so we can see what it is
                    System.out.println(type);
                }
            }
          
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
            return true;
        }
        if(message == null){    //sockets return null (in bad languages, good languages can tell when a socket has been closed) when the socket is closed so we return true to end the thread
            return true;
        }
        return false;
    }
}