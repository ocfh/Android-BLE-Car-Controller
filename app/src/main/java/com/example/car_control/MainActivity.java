package com.example.car_control;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.shy.rockerview.MyRockerView;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.ProtectionDomain;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothDevice remoteDevice;
    private BluetoothSocket socket;
    private OutputStream outputStream;
    private DataInputStream inputStream;
    private Vibrator vibrator;
    private SeekBar seekBar2;

    private Spinner myspinner;
    private BluetoothAdapter defaultAdapter;
    private String address;
    private TextView textViewwdbz,text_show,textView,textViewdy,textView2,textViewwdb2z,text_show873,text_show5432,textViewwdb21z;
    private MyRockerView rockerView;
    private ImageView imageView8,imageView39,imageView319,imageView932,imageView3419,imageView3319,imageView9,imageView9543;
    private ImageButton imageView379;
    private int speed=1,date=0,stop=0,left=0,right=0;
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            StringBuffer stringBuffer = new StringBuffer();
            StringBuffer adc = new StringBuffer();
            StringBuffer sr04 = new StringBuffer();
            StringBuffer wendu = new StringBuffer();
            StringBuffer wendu2 = new StringBuffer();
            if (msg.what == 0x1234){
                byte[] obj = (byte[])msg.obj;
                for (int i=0;i<13;i++) {
                    String s = String.format("%c",obj[i]);
                    stringBuffer.append(s);
                }
                adc.append(String.format("%c",obj[0]));
                adc.append(String.format("%c",obj[1]));
                adc.append(String.format("%c",obj[2]));
                adc.append(String.format("%c",obj[3]));
                sr04.append(String.format("%c",obj[4]));
                sr04.append(String.format("%c",obj[5]));
                sr04.append(String.format("%c",obj[6]));
                sr04.append(String.format("%c",obj[7]));
                wendu.append(String.format("%c",obj[8]));
                wendu.append(String.format("%c",obj[9]));
                wendu2.append(String.format("%c",obj[10]));
                wendu2.append(String.format("%c",obj[11]));

                    textViewdy.setText("电压:");
                    textViewdy.append(adc);
                    textViewdy.append("mV");


                    textViewwdbz.setText("测距:");
                    textViewwdbz.append(sr04);
                    textViewwdbz.append("cm");

                textViewwdb2z.setText("温度:");
                textViewwdb2z.append(wendu);
                textViewwdb2z.append(".");
                textViewwdb2z.append(wendu2);
                textViewwdb2z.append("°C");
              //  textViewwdb21z.setText(stringBuffer);
            }

        }
    };

    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowManager.LayoutParams lp=getWindow().getAttributes();
        lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        getWindow().setAttributes(lp);

        setContentView(R.layout.activity_main);
        seekBar2=(SeekBar)findViewById(R.id.seekBar2);
        vibrator = (Vibrator)getSystemService(Service.VIBRATOR_SERVICE);
        myspinner = (Spinner)findViewById(R.id.myspinner);
        imageView8=(ImageView)findViewById(R.id.imageView8);
        imageView39=(ImageView)findViewById(R.id.imageView39);
        imageView319=(ImageView)findViewById(R.id.imageView319);
        imageView9=(ImageView)findViewById(R.id.imageView9);
        imageView3419=(ImageView)findViewById(R.id.imageView3419);
        imageView3319=(ImageView)findViewById(R.id.imageView3319);
        imageView9543=(ImageView)findViewById(R.id.imageView9543);
        imageView379=(ImageButton)findViewById(R.id.imageView379);
        imageView932=(ImageView)findViewById(R.id.imageView932);
        text_show = (TextView)findViewById(R.id.text_show);
        textViewwdb21z = (TextView)findViewById(R.id.textViewwdb21z);
        textViewdy = (TextView)findViewById(R.id.textViewdy);
        textViewwdb2z = (TextView)findViewById(R.id.textViewwdb2z);
        textViewwdbz = (TextView)findViewById(R.id.textViewwdbz);
        textView2 = (TextView)findViewById(R.id.textView2);
        text_show873 = (TextView)findViewById(R.id.text_show873);
        text_show5432 = (TextView)findViewById(R.id.text_show5432);
        rockerView=(MyRockerView)findViewById(R.id.rocker_view);
        textView=(TextView) findViewById(R.id.textView);
        defaultAdapter = BluetoothAdapter.getDefaultAdapter();
        if (defaultAdapter == null){
            Toast.makeText(MainActivity.this,
                    "当前设备蓝牙不可用",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        defaultAdapter.enable();

        if (!defaultAdapter.isEnabled()){
            Toast.makeText(MainActivity.this,
                    "蓝牙使能失败！",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Set<BluetoothDevice> bondedDevices = defaultAdapter.getBondedDevices();

        if (bondedDevices.size() < 1){
            Toast.makeText(MainActivity.this,
                    "当前安卓系统蓝牙连接设备数量为零！",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String[] str = new String[bondedDevices.size()];
        int i = 0;
        for (BluetoothDevice device : bondedDevices){
            str[i++] = device.getAddress() + "";
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this,
                R.layout.spinner_res,str);

        myspinner.setAdapter(adapter);
        imageView3419.setColorFilter(Color.GRAY);
        imageView3319.setColorFilter(Color.GRAY);
        textView2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this,
                        "Developed Thanks\n遥杆:RockerView@y141111\n图标:双色线性ICON@Konan君",
                        Toast.LENGTH_SHORT).show();
            }
        });
        text_show5432.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                byte[] bytes = {0x09};
                try {
                    outputStream.write(bytes);
                    vibrator.vibrate(100);
                    Toast.makeText(MainActivity.this,
                            "红外循迹",
                            Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        text_show.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(stop==0){
                    stop=1;
                    vibrator.vibrate(100);
                    Toast.makeText(MainActivity.this,
                            "操作锁定",
                            Toast.LENGTH_SHORT).show();
                }else{
                    stop=0;
                    vibrator.vibrate(100);
                    Toast.makeText(MainActivity.this,
                            "取消锁定",
                            Toast.LENGTH_SHORT).show();
                    byte[] bytes0 = {0x02};
                    try {
                        outputStream.write(bytes0);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    text_show.setText("状态:停止行走");
                }
            }
        });
        textViewdy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                byte[] bytes = {0x14};
                try {
                    outputStream.write(bytes);
                    vibrator.vibrate(100);
                    Toast.makeText(MainActivity.this,
                            "获取电压",
                            Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        textViewwdbz.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        imageView379.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // 按下时的逻辑
                        byte[] bytes = {0x12};
                        try {
                            outputStream.write(bytes);
                            vibrator.vibrate(100);
                            Toast.makeText(MainActivity.this,
                                    "打开喇叭",
                                    Toast.LENGTH_SHORT).show();
                            imageView379.setBackgroundResource(R.drawable.beef);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        // 放开时的逻辑
                        byte[] bytes2 = {0x13};
                        try {
                            outputStream.write(bytes2);
                            vibrator.vibrate(100);
                            Toast.makeText(MainActivity.this,
                                    "关闭喇叭",
                                    Toast.LENGTH_SHORT).show();
                            imageView379.setBackgroundResource(R.drawable.b2);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                }
                return false;
            }
        });
        imageView3419.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                byte[] bytes = {0x10};
                try {
                    outputStream.write(bytes);
                    vibrator.vibrate(100);
                    Toast.makeText(MainActivity.this,
                            "左转向灯",
                            Toast.LENGTH_SHORT).show();
                    if(left==0){
                        left=1;
                        imageView3419.setColorFilter(null);
                    }else{
                        left=0;
                        imageView3419.setColorFilter(Color.GRAY);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        imageView3319.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                byte[] bytes = {0x11};
                try {
                    outputStream.write(bytes);
                    vibrator.vibrate(100);
                    Toast.makeText(MainActivity.this,
                            "右转向灯",
                            Toast.LENGTH_SHORT).show();
                    if(right==0){
                        right=1;
                        imageView3319.setColorFilter(null);
                    }else{
                        right=0;
                        imageView3319.setColorFilter(Color.GRAY);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        text_show873.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                byte[] bytes = {0x08};
                try {
                    outputStream.write(bytes);
                    vibrator.vibrate(100);
                    Toast.makeText(MainActivity.this,
                            "超声波传感器",
                            Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        imageView39.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                byte[] bytes = {0x06};
                try {
                    outputStream.write(bytes);
                    vibrator.vibrate(100);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if(speed!=5){
                    speed+=1;
                    seekBar2.setProgress(speed-1);
                    textView.setText("速度:"+speed+"档");
                }
            }
        });
        imageView319.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                byte[] bytes = {0x07};
                try {
                    outputStream.write(bytes);
                    vibrator.vibrate(100);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if(speed!=1){
                    speed-=1;
                    seekBar2.setProgress(speed-1);
                    textView.setText("速度:"+speed+"档");
                }
            }
        });
        seekBar2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress+1 > speed) {
                    // i > 0 时，发送0x06指令i次
                    for (int count = 0; count < progress+1-speed; count++) {
                        byte[] bytes = {0x06};
                        try {
                            outputStream.write(bytes);
                            vibrator.vibrate(100);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else if (progress+1 < speed) {
                    // i < 0 时，发送0x07指令-i次（因为循环不能有负数，所以我们取绝对值）
                    for (int count = 0; count < speed-progress-1; count++) {
                        byte[] bytes = {0x07};
                        try {
                            outputStream.write(bytes);
                            vibrator.vibrate(100);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                speed=progress+1;
                textView.setText("速度:"+speed+"档");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        imageView8.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                address = myspinner.getSelectedItem().toString();


                try {
                    remoteDevice = defaultAdapter.getRemoteDevice(address);
                    socket = remoteDevice.createRfcommSocketToServiceRecord(uuid);
                    socket.connect();
                    outputStream = socket.getOutputStream();
                    inputStream = new DataInputStream(
                            new BufferedInputStream(socket.getInputStream()));
                    Toast.makeText(MainActivity.this,
                            "蓝牙连接成功！",Toast.LENGTH_SHORT).show();

                    vibrator.vibrate(100);

                    bluetooth_socketMSG bluetoothSocketMSG = new bluetooth_socketMSG(inputStream,handler);

                    bluetoothSocketMSG.start();


                } catch (IOException e) {

                    Toast.makeText(MainActivity.this,
                            "蓝牙连接失败！",Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        });
       rockerView.setOnShakeListener(MyRockerView.DirectionMode.DIRECTION_4_ROTATE_45, new MyRockerView.OnShakeListener() {
           @Override
           public void onStart() {

           }

           @RequiresApi(api = Build.VERSION_CODES.O)
           @Override
           public void direction(MyRockerView.Direction direction) {
               if(stop==0){
               switch (direction){
                   case DIRECTION_CENTER:
                       byte[] bytes0 = {0x02};
                       try {
                           outputStream.write(bytes0);
                       } catch (IOException e) {
                           e.printStackTrace();
                       }
                       text_show.setText("状态:停止行走");
                       break;
                   case DIRECTION_UP:
                       byte[] bytes1 = {0x01};
                       try {
                           outputStream.write(bytes1);
                           vibrator.vibrate(100);
                       } catch (IOException e) {
                           e.printStackTrace();
                       }
                       text_show.setText("状态:前进"+speed+"档");
                       break;
                   case DIRECTION_RIGHT:
                       byte[] bytes2 = {0x04};
                       try {
                           outputStream.write(bytes2);
                           vibrator.vibrate(100);
                       } catch (IOException e) {
                           e.printStackTrace();
                       }
                       text_show.setText("状态:右转"+speed+"档");
                       break;
                   case DIRECTION_DOWN:
                       byte[] bytes3 = {0x03};
                       try {
                           outputStream.write(bytes3);
                           vibrator.vibrate(100);
                       } catch (IOException e) {
                           e.printStackTrace();
                       }
                       text_show.setText("状态:后退"+speed+"档");
                       break;
                   case DIRECTION_LEFT:
                       byte[] bytes4 = {0x05};
                       try {
                           outputStream.write(bytes4);
                           vibrator.vibrate(100);
                       } catch (IOException e) {
                           e.printStackTrace();
                       }
                       text_show.setText("状态:左转"+speed+"档");
                       break;
               }}
           }

           @Override
           public void onFinish() {

           }
       });


    }

}