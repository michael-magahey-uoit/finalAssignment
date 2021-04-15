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
using System.Net;
using System.Threading;

namespace MetaTrader5FinalProject
{
    public partial class FrmConnect : Form
    {
        public Socket listenerSocket;
        public Socket bridgeSocket;
        public FrmMain callback;
        public FrmConnect(FrmMain callback)
        {
            if (callback.InvokeRequired)
            {
                callback.BeginInvoke((MethodInvoker)delegate
                {
                    callback.Hide();
                });
            }
            else
            {
                callback.Hide();
            }
            InitializeComponent();
            this.callback = callback;
        }

        private void button1_Click(object sender, EventArgs e)
        {
            listenerSocket = new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp);
            listenerSocket.Bind(new IPEndPoint(IPAddress.Parse("0.0.0.0"), (int)numericUpDown1.Value));
            listenerSocket.Listen(1);
            FrmConnect threadClose = this;
            Thread t = new Thread(() =>
            {
                if (label2.InvokeRequired)
                {
                    label2.BeginInvoke((MethodInvoker)delegate
                    {
                        label2.Text = "Status: Listening @ " + ((int)numericUpDown1.Value).ToString() + "...";
                    });
                }
                else
                {
                    label2.Text = "Status: Listening @ " + ((int)numericUpDown1.Value).ToString() + "...";
                }
                bridgeSocket = listenerSocket.Accept();
                MessageBox.Show("MetaTrader 5 Connected!");
                this.callback.setBridgeSocket(bridgeSocket);
                if (this.callback.InvokeRequired)
                {
                    this.callback.BeginInvoke((MethodInvoker)delegate
                    {
                        this.callback.Show();
                    });
                }
                else
                {
                    this.callback.Show();
                }
                if (threadClose.InvokeRequired)
                {
                    threadClose.BeginInvoke((MethodInvoker)delegate
                    {
                        threadClose.Hide();
                    });
                }
                else
                {
                    threadClose.Hide();
                }
            });
            t.Start();
        }

        private void FrmConnect_FormClosing(object sender, FormClosingEventArgs e)
        {
            try
            {
                listenerSocket.Close();
                if (bridgeSocket.Connected)
                {
                    bridgeSocket.Close();
                }
            }
            catch (Exception ex) 
            { 
                MessageBox.Show(ex.ToString(), "Terminal Error!");
                Environment.Exit(-1);
            }
        }
    }
}
