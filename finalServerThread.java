import java.io.*;
import java.net.*;
import java.util.*;

public class finalServerThread extends Thread
{
    protected Socket socket     = null;
    protected PrintWriter out   = null;
    protected BufferedReader in = null;

    public finalServerThread(Socket socket)
    {
        super();
        this.socket = socket;
        try
        {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
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
            String[] packet = message.split("|");
            String symbol = packet[0];
            String type = packet[1];
            if (type == "Tick")
            {
                float bid = Float.parseFloat(packet[2]);
                float ask = Float.parseFloat(packet[3]);
                //Update price scene
            }
            else if (type == "OrderBook")
            {
                String[] orderbook = packet[2].split("!");
                for (String pricePoint : orderbook)
                {
                    String[] data = pricePoint.split("-");
                    float price = Float.parseFloat(data[0]);
                    float volume = Float.parseFloat(data[1]);
                    String bookType = data[2];
                }
                //Update orderbook scene
            }
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
            return true;
        }
        if (message == null)
        {
            return true;
        }
        return false;
    }
}