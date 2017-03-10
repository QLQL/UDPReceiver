package com.qlgc.udpreceiver;

import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.renderscript.Sampler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.channels.DatagramChannel;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private static final String IP_ADDRESS = "192.168.0.7";//"131.227.95.234";//"10.64.8.78"; // the sender's ip address to test reach or not
    private boolean Server_aktiv = false;
    private boolean playMode = true;
    private TextView textView, textViewIP;

    private int port = 5001; // The default receiving port

    public static int sampleRate = 16000;//44100;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    AudioTrack player;
    private int playBufSize;// = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, audioFormat);

    private boolean receiveMode = false; // the app begins to receive data streaming
    //private byte[] totalByteBuffer =  new byte[65536];
    //private int totalByteBufferLen = totalByteBuffer.length;
    private short[] totalShortBuffer =  new short[65536];
    private int totalShortBufferLen = totalShortBuffer.length;

    private int receiveOffset = 0;
    private int playerOffset = 0;
    private int batchN = 1920;
    //private byte[] writeBufferBatch = new byte[batchN];
    private short[] writeBufferBatch = new short[batchN];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
        String myIPAdress = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        textViewIP = (TextView) findViewById(R.id.textViewIP);
        textViewIP.setText(myIPAdress);

        textView = (TextView) findViewById(R.id.textView);

        Spinner spinner = (Spinner) findViewById(R.id.spinnerFs);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.Fs_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);

        //playBufSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, audioFormat);

        Button btn = (Button) findViewById(R.id.finishButton);
        btn.setEnabled(false);

        addListenerOnSpinnerItemSelection();

//        udpListening(); // started the udp listening thread
    }

    public void addListenerOnSpinnerItemSelection() {
        Spinner spinner = (Spinner) findViewById(R.id.spinnerFs);
        spinner.setOnItemSelectedListener(new SpinnerActivity());
    }


    /** Called when the user clicks the Start button */
    public void startListening(View view) {
        // Do something in response
        //        TextView textView = (TextView) findViewById(R.id.textView);
        textView.setText("Start Listening......");

        //        //Receiver Side:
        //        Intent mIntent = getIntent();
        //        sampleRate = mIntent.getIntExtra("QLQLQLQL", 0);
        //Toast.makeText(this, "The sampling rate is: " + Integer.toString(sampleRate), Toast.LENGTH_SHORT).show();

        EditText editTextPort = (EditText) findViewById(R.id.editPort);
        //Pattern p = Pattern.compile("\\w+([0-9]+)\\w+([0-9]+)");
        Pattern p = Pattern.compile("(\\d+)");
        Matcher m = p.matcher(editTextPort.getText().toString());
        if (m.find()) {
            port = Integer.parseInt(m.group());
        }


        Toast.makeText(this, "The sampling rate is: " + Integer.toString(sampleRate) + "  The port is: " + Integer.toString(port), Toast.LENGTH_SHORT).show();

        playBufSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, audioFormat);
        Server_aktiv = true;
        udpListening();

        Button startButton = (Button) findViewById(R.id.startButton);
        startButton.setEnabled(false);
        Button finishButton = (Button) findViewById(R.id.finishButton);
        finishButton.setEnabled(true);
    }

    public void finishListening(View view) {
        // Do something in response
        // TextView textView = (TextView) findViewById(R.id.textView);
        textView.setText("Finish Listening......");


        Server_aktiv = false;


        Button startButton = (Button) findViewById(R.id.startButton);
        startButton.setEnabled(true);
        Button finishButton = (Button) findViewById(R.id.finishButton);
        finishButton.setEnabled(false);
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

                byte[] message = new byte[8000]; //You must set the incoming message's maximum size

                try {
                    if (playMode) {
                        player = new AudioTrack(AudioManager.STREAM_MUSIC,
                                sampleRate,
                                AudioFormat.CHANNEL_OUT_MONO,
                                AudioFormat.ENCODING_PCM_16BIT,
                                playBufSize,
                                AudioTrack.MODE_STREAM);
                        player.setPlaybackRate(sampleRate);
                        Log.w("VS", "Player initialized");
                        player.play();
                        //writeBuffer(); // started another thread to buffer data to the player
                    }

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
                    ds.setSoTimeout(100);

                    DatagramPacket dp = new DatagramPacket(message, message.length);
                    while (Server_aktiv){
                        try {
                            long startTime = System.nanoTime();
                            Log.w("UDP", "Try to receive data");
                            ds.receive(dp);
                            //Log.d("Udp","Roger, received");
                            Frame++;
                            Log.d("Udp",String.valueOf(Frame)+" received "+String.valueOf(dp.getLength())+" data");

                            // byte to short
                            byte[] buffer = dp.getData();
                            int Sample  = buffer.length/2;
                            short LSB, MSB;
                            short shortrecover;
                            if (Sample>1){
                                short[] shortBuffer = new short[Sample];
//                                short[] shortBuffer2 = new short[Sample];
                                for (int i=0;i<Sample;i++){
                                    LSB = (short) buffer[i*2];
                                    MSB = (short) buffer[i*2+1];
//                                    if (LSB<0){
//                                        shortrecover = (short) (MSB*256 + LSB + 256);
//                                    } else {
//                                        shortrecover = (short) (MSB*256 + LSB);
//                                    }
//
//                                    shortBuffer2[i] = shortrecover;

                                    shortrecover = (short) ( (MSB<<8) | LSB&0xFF );
                                    shortBuffer[i] = shortrecover;
                                }
//                                Log.w("UDP", "C:  short1: " + Arrays.toString(Arrays.copyOfRange(shortBuffer, 0, 50)));
//                                Log.w("UDP", "C:  short2: " + Arrays.toString(Arrays.copyOfRange(shortBuffer2, 0, 50)));
//                                Log.w("UDP", "C: read in: " + Arrays.toString(Arrays.copyOfRange(buffer, 0, 50)));
//                                if (playMode) {
//                                    player.write(shortBuffer, 0, shortBuffer.length);
//                                }
//
//                                if (playMode) {
//                                    player.write(buffer, 0, buffer.length);
//                                }

                                if (receiveOffset+shortBuffer.length>totalShortBufferLen){
                                    int L = totalShortBufferLen-receiveOffset;
                                    System.arraycopy(shortBuffer, 0, totalShortBuffer, receiveOffset, L);
                                    System.arraycopy(shortBuffer, L, totalShortBuffer, 0, shortBuffer.length-L);
                                } else {
                                    System.arraycopy(shortBuffer, 0, totalShortBuffer, receiveOffset, shortBuffer.length);
                                }
                                receiveOffset += shortBuffer.length;
                                if (receiveOffset>=totalShortBufferLen){ receiveOffset -= totalShortBufferLen;}

                                if (playMode) {
                                    player.write(shortBuffer, 0, shortBuffer.length, AudioTrack.WRITE_NON_BLOCKING);
                              }
                            }





                            // transfer the data to the totalByteBuffer
//                            if (receiveOffset+buffer.length>totalByteBufferLen){
//                                int L = totalByteBufferLen-receiveOffset;
//                                System.arraycopy(buffer, 0, totalByteBuffer, receiveOffset, L);
//                                System.arraycopy(buffer, L, totalByteBuffer, 0, buffer.length-L);
//                            } else {
//                                System.arraycopy(buffer, 0, totalByteBuffer, receiveOffset, buffer.length);
//                            }
//                            for (int i=0;i<buffer.length;i++){
//                                int tmp = i+receiveOffset;
//                                if (tmp>=totalByteBufferLen){ tmp -= totalByteBufferLen;}
//                                totalByteBuffer[tmp] = buffer[i];
//                            }

//                            receiveOffset += buffer.length;
//                            if (receiveOffset>=totalByteBufferLen){ receiveOffset -= totalByteBufferLen;}


                            // did get time in the time slot allowed
                            receiveMode = true;


                            // show the frame number and the length in the screen
                            final int LL = dp.getLength();
                            final int FFrame = Frame;
                            handler.post(new Runnable(){
                                public void run() {
                                    textView.setText(String.valueOf(FFrame)+"-----"+String.valueOf(LL));
                                }
                            });

                            long endTime = System.nanoTime();
                            long duration = (endTime - startTime);
                            Log.w("UDP","receive data time "+String.valueOf(duration/1000000.0));

                        } catch (SocketTimeoutException e) {
                            // didn't get time in the time slot allowed
                            receiveMode = false;
                            Log.w("Test","Continue to listen");
                            continue;
                        } catch (IOException e) {
                            Log.e(" UDP ", "error");
                            e.printStackTrace();
                        }
                    }
                    receiveMode = false;
                    ds.close();
                    if (playMode){
                        player.flush();
                        player.stop();
                        player.release();
                        Log.d("VS","Player released");
                    }
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }

        });

        //start the listener
        udpListenStartThread.start();
    }

    public void writeBuffer()
    {

        //final Handler handler = new Handler();
        Thread writeBufferStartThread = new Thread(new Runnable() {

            @Override
            public void run() {
                // pause the programme for 1ooo milliseconds
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }



                try {

                    player = new AudioTrack(AudioManager.STREAM_MUSIC,
                            sampleRate,
                            AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT,
                            batchN*16,//playBufSize,
                            AudioTrack.MODE_STREAM);
                    player.setPlaybackRate(sampleRate);
                    Log.w("VS", "Player initialized");
                    player.play();


                    while (Server_aktiv){
                        // If receiveMode, the data is streaming in, we will push them to the buffer. Otherwise, wait
                        while (receiveMode){
                            long startTime = System.nanoTime();

//                            if (playerOffset+batchN>totalByteBufferLen){
//                                int L = totalByteBufferLen-playerOffset;
//                                System.arraycopy(totalByteBuffer, playerOffset, writeBufferBatch, 0, L);
//                                System.arraycopy(totalByteBuffer, 0, writeBufferBatch, L, batchN-L);
//                            } else {
//                                System.arraycopy(totalByteBuffer, playerOffset, writeBufferBatch, 0, batchN);
//                            }
                            if (playerOffset+batchN>totalShortBufferLen){
                                int L = totalShortBufferLen-playerOffset;
                                System.arraycopy(totalShortBuffer, playerOffset, writeBufferBatch, 0, L);
                                System.arraycopy(totalShortBuffer, 0, writeBufferBatch, L, batchN-L);
                            } else {
                                System.arraycopy(totalShortBuffer, playerOffset, writeBufferBatch, 0, batchN);
                            }
//                            for (int i=0;i<batchN;i++){
//                                int tmp = i+receiveOffset;
//                                if (tmp>=totalByteBufferLen){ tmp -= totalByteBufferLen;}
//                                writeBufferBatch[i] = totalByteBuffer[tmp];
//                            }
                            int playbufferflag = player.write(writeBufferBatch, 0, batchN, AudioTrack.WRITE_NON_BLOCKING);
                            try {
                                Thread.sleep(40);
                            } catch (InterruptedException e1) {
                                // TODO Auto-generated catch block
                                e1.printStackTrace();
                            }
                            playerOffset += batchN;
                            // if (playerOffset>=totalByteBufferLen){ playerOffset -= totalByteBufferLen;}
                            if (playerOffset>=totalShortBufferLen){ playerOffset -= totalShortBufferLen;}
                            long endTime = System.nanoTime();
                            long duration = (endTime - startTime);
                            Log.w("Player","send data to the player "+String.valueOf(duration/1000000.0)+"======"+String.valueOf(playbufferflag));
                        }
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e1) {
                            // TODO Auto-generated catch block
                            e1.printStackTrace();
                        }

                    }
                    player.flush();
                    player.stop();
                    player.release();
                    Log.d("VS","Player released");
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }

        });

        //start the listener
//        writeBufferStartThread.setPriority(Thread.MAX_PRIORITY);
        writeBufferStartThread.start();
    }

}
