package com.qlgc.udpreceiver;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.channels.DatagramChannel;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private static final String IP_ADDRESS = "192.168.0.7";//"131.227.95.234";//"10.64.8.78";
    private boolean Server_aktiv = true;
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = (TextView) findViewById(R.id.textView);
        udpListening(); // started the udp listening thread
    }




    public void udpListening()
    {

        final Handler handler = new Handler();
        Thread udpListenStartThread = new Thread(new Runnable() {

            @Override
            public void run() {
                // pause the programme for 1ooo milliseconds
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }

                int port = 50007;
                byte[] message = new byte[8000]; //You must set the incoming message's maximum size

                try {
                    int Frame = 0;
                    InetAddress serverAddr = InetAddress.getByName(IP_ADDRESS);
                    if (serverAddr.isReachable(1000))
                        Log.w("Test", "host is reachable");
                    else
                        Log.w("Test", "host is not reachable");


                    // It is very important that the socket need to be bind, otherwise, it cannot receive any packet
                    DatagramChannel channel = DatagramChannel.open();
                    DatagramSocket ds = channel.socket();// <-- create an unbound socket first
                    ds.setReuseAddress(true);
                    ds.bind(new InetSocketAddress(port)); // <-- now bind it
                    ds.setSoTimeout(2000);

                    DatagramPacket dp = new DatagramPacket(message, message.length);
                    while (Server_aktiv){
                        try {
                            Log.w("UDP", "Try to receive data");
                            ds.receive(dp);
                            //Log.d("Udp","Roger, received");
                            Frame++;
                            Log.d("Udp",String.valueOf(Frame)+" received "+String.valueOf(dp.getLength())+" data");


                            // show the frame number and the length in the screen
                            final int LL = dp.getLength();
                            final int FFrame = Frame;
                            handler.post(new Runnable(){
                                public void run() {
                                    textView.setText(String.valueOf(FFrame)+"-----"+String.valueOf(LL));
                                }
                            });

                        } catch (SocketTimeoutException e) {
                            // resend
                            Log.w("Test","Continue to listen");
                            continue;
                        } catch (IOException e) {
                            Log.e(" UDP ", "error");
                            e.printStackTrace();
                        }
                    }
                    ds.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }

        });

        //start the listener
        udpListenStartThread.start();
    }



}
