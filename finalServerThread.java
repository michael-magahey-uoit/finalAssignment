import java.io.*;
import java.net.*;
import java.util.*;

public class finalServerThread extends Thread
{
    protected Socket socket     = null;
    protected PrintWriter out   = null;
    protected BufferedReader in = null;
    protected finalServer callback = null;

    public finalServerThread(Socket socket, finalServer callback)
    {
        super();
        this.socket = socket;
        this.callback = callback;
        try
        {
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
            message = in.readLine();
            String[] packetBacklog = message.split("[ ]");
            for (String backlogMessage : packetBacklog)
            {
                System.out.println("[backlog]: " + backlogMessage);
                String[] packet = backlogMessage.split("[|]");
                String symbol = packet[0];
                String type = packet[1];
                if (type.equals("Tick"))
                {
                    float bid = Float.parseFloat(packet[2]);
                    float ask = Float.parseFloat(packet[3]);
                    System.out.println("New Price! Bid: " + bid + " | Ask: " + ask);
                    callback.paintPrices(bid, ask);
                }
                else if (type.equals("OrderBook"))
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
                else{
                    System.out.println(type);
                }
            }
          
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
            return true;
        }
        if(message == null){
            return true;
        }
        return false;
    }
}