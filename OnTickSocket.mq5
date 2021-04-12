//+------------------------------------------------------------------+
//|                                                 OnTickSocket.mq5 |
//|                                  Copyright 2021, MetaQuotes Ltd. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
#property copyright "Copyright 2021, MetaQuotes Ltd."
#property link      "https://www.mql5.com"
#property version   "1.00"

input ulong ExtSkipFirstTicks=10;  // Skip First Ticks
input string Address="127.0.0.1";  // Remote Address
input uint Port=1337;              // Remote Port
int socket;

bool book_subscribed=false;
MqlBookInfo  book[];
//+------------------------------------------------------------------+
//| Expert initialization function                                   |
//+------------------------------------------------------------------+
int OnInit()
{
   if (AccountInfoInteger(ACCOUNT_MARGIN_MODE)!=ACCOUNT_MARGIN_MODE_RETAIL_HEDGING)
   {
      Print("Error! This trader only works with hedging type accounts. Please switch your account to hedging");
      return(INIT_FAILED);
   }

   Comment(StringFormat("Waiting for the first %I64u ticks to arrive",ExtSkipFirstTicks));
   PrintFormat("Waiting for the first %I64u ticks to arrive",ExtSkipFirstTicks);

   if(MarketBookAdd(_Symbol))
   {
      book_subscribed=true;
      PrintFormat("%s: MarketBookAdd(%s) function returned true",__FUNCTION__,_Symbol);
   }
   else
   {
      PrintFormat("%s: MarketBookAdd(%s) function returned false! GetLastError()=%d",__FUNCTION__,_Symbol,GetLastError());
   }

   socket = SocketCreate();
   if (socket != INVALID_HANDLE)
   {
      if (SocketConnect(socket,Address,Port,1000))
      {
         Print("Established connection to ",Address,":",Port,"!");
         return(INIT_SUCCEEDED);
      }
      else
      {
         Print("Connection to ",Address,":",Port," failed, error",GetLastError());
         return(INIT_FAILED);
      }
   }
   else
   {
      Print("Failed to create a socket, error ",GetLastError());
      return(INIT_FAILED);
   }

   return(INIT_FAILED);
}
//+------------------------------------------------------------------+
//| Expert deinitialization function                                 |
//+------------------------------------------------------------------+
void OnDeinit(const int reason)
{
   SocketClose(socket);
   EventKillTimer();
}
//+------------------------------------------------------------------+
//| Reconnect Function                                               |
//+------------------------------------------------------------------+
void Reconnect()
{
   SocketClose(socket);
   socket = SocketCreate();
   if (socket != INVALID_HANDLE)
   {
      if (SocketConnect(socket, Address, Port, 1000))
      {
         Print("Established connection to ",Address,":",Port,"!");
         return;
      }
      else
      {
         Print("Connection to ",Address,":",Port," failed, error ", GetLastError());
         return;
      }
   }
   else
   {
      Print("Failed to create socket, error ", GetLastError());
      return;
   }
}
//+------------------------------------------------------------------+
//| Expert tick function                                             |
//+------------------------------------------------------------------+
void OnTick()
{
   Print("New Tick for ", _Symbol,"! Bid: ",SymbolInfoDouble(_Symbol, SYMBOL_BID)," ASK: ",SymbolInfoDouble(_Symbol, SYMBOL_ASK));
   Print("Sending Data...");
   char data[];
   string baseData = _Symbol + "|" + "Tick" + "|" + SymbolInfoDouble(_Symbol, SYMBOL_BID) + "|" + SymbolInfoDouble(_Symbol, SYMBOL_ASK);
   StringToCharArray(baseData,data);
   int len = ArraySize(data);
   int res = SocketSend(socket,data,len);
   if (res == -1)
   {
      Print("Failed to send bytes to socket, error ", GetLastError());
      Print("Attempting reconnect...");
      Reconnect();
   }
   else
   {
      Print("Sent ", res, " bytes of data!");
   }
}
//+------------------------------------------------------------------+
//| BookEvent function                                               |
//+------------------------------------------------------------------+
void OnBookEvent(const string &symbol)
{
   static ulong starttime=0;
   static ulong tickcounter=0;
   if (!book_subscribed)
      return;
   if(symbol!=_Symbol)
      return;
   tickcounter++;
   if(tickcounter<ExtSkipFirstTicks)
      starttime=GetMicrosecondCount();

   bool getBook=MarketBookGet(symbol,book);
   
   if (getBook)
   {
      int size=ArraySize(book);
      string data = _Symbol + "|OrderBook|";
      for (int i=0; i<size; i++)
      {
         data += book[i].price+"-"+book[i].volume+"-"+EnumToString(book[i].type)+"!";
      }
      char packet[];
      StringToCharArray(data, packet);
      int len = ArraySize(packet);
      int res = SocketSend(socket, packet, len);
      if (res == -1)
      {
         Print("Failed to send bytes to socket, error ", GetLastError());
         Print("Attempting reconnect...");
         Reconnect();
      }
      else
      {
         Print("Send ", res, " bytes of data!");
      }
   }
   ulong endtime=GetMicrosecondCount()-starttime;
   ulong ticks  =1+tickcounter-ExtSkipFirstTicks;
   Comment(StringFormat("%I64u ticks for %.1f seconds: %.1f ticks/sec ", ticks, endtime/1000.0/1000.0,ticks*1000.0*1000.0/endtime));
}
//+------------------------------------------------------------------+