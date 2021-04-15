using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;
using System.Net.Sockets;
using System.Threading;

namespace MetaTrader5FinalProject
{
    public partial class FrmMain : Form
    {
        private Socket bridgeSocket;
        private Thread panelUpdater;

        public double[] prices = null;
        public double[] volumes = null;
        public String[] orderTypes = null;

        public int historyIndex = 0;
        public double[] bidHistory = new double[200];
        public double[] askHistory = new double[200];
        public double callBid;
        public double callAsk;


        public FrmMain()
        {
            InitializeComponent();
            prices = new double[0];
            volumes = new double[0];
            orderTypes = new String[0];
            panel1.Paint += new PaintEventHandler(panel1_Paint);
            panel2.Paint += new PaintEventHandler(panel2_Paint);
            FrmConnect connectFrm = new FrmConnect(this);
            connectFrm.Show();
        }

        public void setBridgeSocket(Socket bridge)
        {
            this.bridgeSocket = bridge;
            Thread t = new Thread(() =>
            {
                Updater(this);
            });
            t.Start();
        }

        private void Updater(FrmMain callback)
        {
            while (this.bridgeSocket.Connected)
            {
                //MessageBox.Show("Preparing To Receive Message...");
                byte[] rawData = new byte[1024 * 1024]; //Receive 1MB of data
                int dataLen = this.bridgeSocket.Receive(rawData);
                String Data = Encoding.ASCII.GetString(rawData);
                MessageBox.Show("Message Received! " + Data, "Message");
                String[] packet = Data.Split('\0');
                foreach (String pack in packet)
                {
                    String[] part = pack.Split('|');
                    if (part.Length >= 3)
                    {
                        String Symbol = part[0];
                        if (callback.InvokeRequired)
                        {
                            callback.BeginInvoke((MethodInvoker)delegate
                            {
                                callback.Text = "MetaTrader 5 Bridge - " + Symbol;
                            });
                        }
                        else
                        {
                            callback.Text = "MetaTrader 5 Bridge - " + Symbol;
                        }
                        String Type = part[1];
                        switch (Type)
                        {
                            case "Tick":
                                String bidStr = part[2];
                                String askStr = part[3];
                                MessageBox.Show(bidStr, "bid");
                                MessageBox.Show(askStr, "ask");
                                double bid = double.Parse(part[2]);
                                double ask = double.Parse(part[3]);
                                MessageBox.Show(pack);
                                if (callback.panel2.InvokeRequired)
                                {
                                    callback.panel2.BeginInvoke((MethodInvoker)delegate
                                    {
                                        callback.panel2.Refresh();
                                    });
                                }
                                else
                                {
                                    callback.panel2.Refresh();
                                }
                                break;
                            case "OrderBook":
                                MessageBox.Show(pack, "Orderbook packet");
                                String[] orderbookPoints = part[2].Split('!');
                                prices = new double[orderbookPoints.Length];
                                volumes = new double[orderbookPoints.Length];
                                orderTypes = new string[orderbookPoints.Length];
                                for (int i = 0; i < orderbookPoints.Length; i++)
                                {
                                    String[] orderInfo = orderbookPoints[i].Split('-');
                                    if (orderInfo.Length == 3)
                                    {
                                        prices[i] = double.Parse(orderInfo[0]);
                                        volumes[i] = (double)(int.Parse(orderInfo[1]));
                                        orderTypes[i] = orderInfo[2];
                                    }
                                }
                                if (callback.panel1.InvokeRequired)
                                {
                                    callback.panel1.BeginInvoke((MethodInvoker)delegate
                                    {
                                        callback.panel1.Refresh();
                                    });
                                }
                                else
                                {
                                    callback.panel1.Refresh();
                                }
                                break;
                        }
                    }
                }
            }
        }

        private void Form1_Load(object sender, EventArgs e)
        {

        }

        private void panel1_Paint(object sender, PaintEventArgs e)
        {

            var p = sender as Panel;
            var g = e.Graphics;
            if (volumes.Length > 0)
            {
                int lastX = 0;
                double yMax = double.MinValue;

                for (int i = 0; i < volumes.Length; i++)
                {
                    yMax = Math.Max(volumes[i], yMax);
                }

                if (volumes.Length != prices.Length)
                {
                    throw new Exception("Volume Price mismatch!");
                }

                int xBase = (int)p.Width / volumes.Length;
                xBase -= 10;
                for (int i = 0; i < volumes.Length; i++)
                {
                    int height = (int)(volumes[i] / yMax) * (p.Height - 50);

                    if (orderTypes[i] == "BOOK_TYPE_BUY")
                    {
                        g.FillRectangle(new SolidBrush(Color.Blue), lastX, 0, xBase, height);
                    }
                    else if (orderTypes[i] == "BOOK_TYPE_SELL")
                    {
                        g.FillRectangle(new SolidBrush(Color.Red), lastX, 0, xBase, height);
                    }
                    lastX = lastX + xBase + 10;
                }
            }
        }

        private void panel2_Paint(object sender, PaintEventArgs e)
        {
            var p = sender as Panel;
            var g = e.Graphics;
          
            for (int i = historyIndex; i > 0; i--)
            {
                bidHistory[i] = bidHistory[i - 1];
                askHistory[i] = askHistory[i - 1];
            }

            bidHistory[0] = callBid;
            askHistory[0] = callAsk;

            callBid = 0.0;
            callAsk = 0.0;

            historyIndex++;
            if (historyIndex > 200)
            {
                historyIndex = 200;
            }

            double yMax = double.MinValue;
            double yMin = double.MaxValue;

            for (int i = 0; i < historyIndex; i++)
            {
                yMax = Math.Max(yMax, bidHistory[i]);
                yMax = Math.Max(yMax, askHistory[i]);

                yMin = Math.Min(yMin, bidHistory[i]);
                yMin = Math.Min(yMin, askHistory[i]);
            }

            int prevX = 0;
            if (yMax - yMin != 0)
            {
                double norm = (bidHistory[0] - yMin) / (yMax - yMin);
                double prevY = p.Height - (norm * (p.Height - 50));
                double incX = p.Width / bidHistory.Length;
                for (int i = 1; i < historyIndex; i++)
                {
                    double num = (bidHistory[i] - yMin) / (yMax - yMin);
                    double yPos = p.Height - (num * (p.Height - 50));
                    g.DrawLine(new Pen(Color.Blue), prevX, (int)prevY, (int)(prevX + incX), (int)yPos);
                    prevX = prevX + (int)incX;
                    prevY = (int)yPos;
                }
            }

            if (yMax - yMin != 0)
            {
                prevX = 0;
                double norm = (askHistory[0] - yMin) / (yMax - yMin);
                double prevY = p.Height - (norm * (p.Height - 50));
                double incX = p.Width / askHistory.Length;
                for (int i = 1; i < historyIndex; i++)
                {
                    double num = (askHistory[i] - yMin) / (yMax - yMin);
                    double yPos = p.Height - (norm * (p.Height - 50));
                    g.DrawLine(new Pen(Color.Red), prevX, (int)prevY, (int)(prevX + incX), (int)yPos);
                    prevX = prevX + (int)incX;
                    prevY = (int)yPos;
                }
            }
        }

        private void button1_Click(object sender, EventArgs e)
        {
            this.bridgeSocket.Close();
            this.Close();
        }
    }
}
