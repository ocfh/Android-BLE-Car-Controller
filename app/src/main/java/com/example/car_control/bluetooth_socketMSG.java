package com.example.car_control;

import android.os.Handler;
import android.os.Message;

import java.io.DataInputStream;
import java.io.IOException;

public class bluetooth_socketMSG extends Thread{

    private DataInputStream inputStream;
    private Handler handler;

    public bluetooth_socketMSG(DataInputStream inputStream02, Handler handler02){
        this.inputStream = inputStream02;
        this.handler = handler02;
    }

    @Override
    public void run() {
        super.run();

        byte[] buffer = new byte[64];

        while (!Thread.interrupted()){

            try {
                inputStream.readFully(buffer,0,12);
                Message message = new Message();
                message.what = 0x1234;
                message.obj = buffer;
                handler.sendMessage(message);


            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
