package com.example.car_control;

import androidx.annotation.NonNull;

import android.bluetooth.BluetoothGattDescriptor;
import android.content.pm.PackageManager;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.shy.rockerview.MyRockerView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    // ========== BLE 固定UUID Ai-WB2-32S 透传 ==========
    private static final UUID SERVICE_UUID = UUID.fromString("55e405d2-af9f-a98f-e54a-7dfe43535355");
    private static final UUID CHAR_WRITE_UUID = UUID.fromString("16962447-c623-61ba-d94b-4d1e43535349");
    private static final UUID CHAR_NOTIFY_UUID = UUID.fromString("b39b7234-beec-d4a8-f443-418843535349");

    // BLE全局对象
    private BluetoothAdapter mBtAdapter;
    private BluetoothLeScanner mScanner;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCharacteristic mWriteChar;
    private final List<BluetoothDevice> mDeviceList = new ArrayList<>();
    private ArrayAdapter<String> mSpinnerAdapter;
    private boolean isScanning = false;

    // UI控件
    private SeekBar seekBar2;
    private Vibrator vibrator;
    private Spinner myspinner;
    private TextView textViewwdbz, text_show, textView, textViewdy, textView2, textViewwdb2z, text_show873, text_show5432, textViewwdb21z;
    private MyRockerView rockerView;
    private ImageView imageView8, imageView39, imageView319, imageView932, imageView3419, imageView3319, imageView9, imageView9543;
    private ImageButton imageView379;
    private Button btn_scan_ble;

    // 控制参数
    private int speed = 1, stop = 0, left = 0, right = 0;

    // UI数据接收Handler（原有解析逻辑完全保留）
    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            if (msg.what == 0x1234) {
                byte[] obj = (byte[]) msg.obj;
                if (obj == null || obj.length < 12) return;

                StringBuilder adc = new StringBuilder();
                StringBuilder sr04 = new StringBuilder();
                StringBuilder wendu = new StringBuilder();
                StringBuilder wendu2 = new StringBuilder();

                adc.append((char) obj[0]).append((char) obj[1]).append((char) obj[2]).append((char) obj[3]);
                sr04.append((char) obj[4]).append((char) obj[5]).append((char) obj[6]).append((char) obj[7]);
                wendu.append((char) obj[8]).append((char) obj[9]);
                wendu2.append((char) obj[10]).append((char) obj[11]);

                textViewdy.setText("电压:" + adc + "mV");
                textViewwdbz.setText("测距:" + sr04 + "cm");
                textViewwdb2z.setText("温度:" + wendu + "." + wendu2 + "°C");
            }
        }
    };

    // BLE GATT 通信回调（替代旧读取线程）
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                // 连接成功，发现服务
                gatt.discoverServices();
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "蓝牙断开连接", Toast.LENGTH_SHORT).show());
                mWriteChar = null;
                if (mBluetoothGatt != null) {
                    mBluetoothGatt.close();
                    mBluetoothGatt = null;
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "服务发现失败", Toast.LENGTH_SHORT).show());
                gatt.disconnect();
                return;
            }
            BluetoothGattService service = gatt.getService(SERVICE_UUID);
            if (service == null) {
                // 关键：找不到服务主动断开GATT，避免僵死连接
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "未找到WB2透传服务", Toast.LENGTH_SHORT).show());
                gatt.disconnect();
                return;
            }
            mWriteChar = service.getCharacteristic(CHAR_WRITE_UUID);
            BluetoothGattCharacteristic notifyChar = service.getCharacteristic(CHAR_NOTIFY_UUID);
            boolean notifyEnable = gatt.setCharacteristicNotification(notifyChar, true);
            if (!notifyEnable) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "开启通知特征失败", Toast.LENGTH_SHORT).show());
                gatt.disconnect();
                return;
            }
            BluetoothGattDescriptor descriptor = notifyChar.getDescriptor(
                    UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
            if (descriptor != null) {
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(descriptor);
            } else {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "无通知描述符", Toast.LENGTH_SHORT).show());
                gatt.disconnect();
            }
        }

        // 监听Descriptor写入结果，成功才算真正连接完成
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "BLE连接成功！", Toast.LENGTH_SHORT).show());
            } else {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "开启通知失败，断开", Toast.LENGTH_SHORT).show());
                gatt.disconnect();
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (CHAR_NOTIFY_UUID.equals(characteristic.getUuid())) {
                byte[] buffer = characteristic.getValue();
                Message msg = Message.obtain();
                msg.what = 0x1234;
                msg.obj = buffer;
                handler.sendMessage(msg);
            }
        }
    };

    // BLE扫描回调
    private final ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice dev = result.getDevice();
            // 设备去重
            boolean exist = false;
            for (BluetoothDevice d : mDeviceList) {
                if (d.getAddress().equals(dev.getAddress())) {
                    exist = true;
                    break;
                }
            }
            if (!exist) {
                mDeviceList.add(dev);
                String name = dev.getName() == null ? "未知设备" : dev.getName();
                mSpinnerAdapter.add(name + " | " + dev.getAddress());
                mSpinnerAdapter.notifyDataSetChanged();
            }
        }
    };

    // 统一BLE发送指令方法（替换所有outputStream.write）
    private void sendBleData(byte[] data) {
        if (mBluetoothGatt == null || mWriteChar == null) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, "未连接蓝牙，请等待服务加载完成", Toast.LENGTH_SHORT).show());
            return;
        }
        mWriteChar.setValue(data);
        // Ai-WB2必须配置无响应写入，否则极易断开
        mWriteChar.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        mBluetoothGatt.writeCharacteristic(mWriteChar);
    }

    // 开始/停止扫描BLE设备
    private void toggleScan() {
        if (!isScanning) {
            mDeviceList.clear();
            mSpinnerAdapter.clear();
            mScanner.startScan(mScanCallback);
            isScanning = true;
            btn_scan_ble.setText("停止扫描");
            Toast.makeText(this, "正在扫描BLE设备...", Toast.LENGTH_SHORT).show();
            // 自动扫描5秒后停止
            new Handler().postDelayed(() -> {
                if (isScanning) toggleScan();
            }, 5000);
        } else {
            mScanner.stopScan(mScanCallback);
            isScanning = false;
            btn_scan_ble.setText("扫描BLE设备");
            Toast.makeText(this, "扫描结束", Toast.LENGTH_SHORT).show();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 权限申请
        requestBlePermission();

        // 初始化蓝牙适配器
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            Toast.makeText(this, "设备无蓝牙", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (!mBtAdapter.isEnabled()) mBtAdapter.enable();
        mScanner = mBtAdapter.getBluetoothLeScanner();

        // 绑定控件
        bindViews();

        // Spinner初始化
        mSpinnerAdapter = new ArrayAdapter<>(this, R.layout.spinner_res);
        myspinner.setAdapter(mSpinnerAdapter);

        imageView3419.setColorFilter(0xFF888888);
        imageView3319.setColorFilter(0xFF888888);

        // 扫描按钮点击
        btn_scan_ble.setOnClickListener(v -> toggleScan());

        // 连接按钮（imageView8）
        imageView8.setOnClickListener(v -> {
            int pos = myspinner.getSelectedItemPosition();
            if (pos < 0 || pos >= mDeviceList.size()) {
                Toast.makeText(this, "请先扫描并选择BLE设备", Toast.LENGTH_SHORT).show();
                return;
            }
            BluetoothDevice targetDev = mDeviceList.get(pos);
            // 断开旧连接
            if (mBluetoothGatt != null) {
                mBluetoothGatt.disconnect();
                mBluetoothGatt.close();
                mBluetoothGatt = null;
            }
            // 建立GATT连接
            mBluetoothGatt = targetDev.connectGatt(this, false, mGattCallback);
        });

        // 红外循迹
        text_show5432.setOnClickListener(v -> {
            sendBleData(new byte[]{0x09});
            vibrator.vibrate(100);
            Toast.makeText(this, "红外循迹", Toast.LENGTH_SHORT).show();
        });

        // 锁定/解锁操作
        text_show.setOnClickListener(v -> {
            if (stop == 0) {
                stop = 1;
                vibrator.vibrate(100);
                Toast.makeText(this, "操作锁定", Toast.LENGTH_SHORT).show();
            } else {
                stop = 0;
                vibrator.vibrate(100);
                Toast.makeText(this, "取消锁定", Toast.LENGTH_SHORT).show();
                sendBleData(new byte[]{0x02});
                text_show.setText("状态:停止行走");
            }
        });

        // 获取电压
        textViewdy.setOnClickListener(v -> {
            sendBleData(new byte[]{0x14});
            vibrator.vibrate(100);
            Toast.makeText(this, "获取电压", Toast.LENGTH_SHORT).show();
        });

        // 喇叭按键
        imageView379.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    sendBleData(new byte[]{0x12});
                    vibrator.vibrate(100);
                    imageView379.setBackgroundResource(R.drawable.beef);
                    break;
                case MotionEvent.ACTION_UP:
                    sendBleData(new byte[]{0x13});
                    vibrator.vibrate(100);
                    imageView379.setBackgroundResource(R.drawable.b2);
                    break;
            }
            return false;
        });

        // 左转向灯
        imageView3419.setOnClickListener(v -> {
            sendBleData(new byte[]{0x10});
            vibrator.vibrate(100);
            if (left == 0) {
                left = 1;
                imageView3419.clearColorFilter();
            } else {
                left = 0;
                imageView3419.setColorFilter(0xFF888888);
            }
        });

        // 右转向灯
        imageView3319.setOnClickListener(v -> {
            sendBleData(new byte[]{0x11});
            vibrator.vibrate(100);
            if (right == 0) {
                right = 1;
                imageView3319.clearColorFilter();
            } else {
                right = 0;
                imageView3319.setColorFilter(0xFF888888);
            }
        });

        // 超声波
        text_show873.setOnClickListener(v -> {
            sendBleData(new byte[]{0x08});
            vibrator.vibrate(100);
            Toast.makeText(this, "超声波传感器", Toast.LENGTH_SHORT).show();
        });

        // 速度+
        imageView39.setOnClickListener(v -> {
            sendBleData(new byte[]{0x06});
            vibrator.vibrate(100);
            if (speed != 5) {
                speed++;
                seekBar2.setProgress(speed - 1);
                textView.setText("速度:" + speed + "档");
            }
        });

        // 速度-
        imageView319.setOnClickListener(v -> {
            sendBleData(new byte[]{0x07});
            vibrator.vibrate(100);
            if (speed != 1) {
                speed--;
                seekBar2.setProgress(speed - 1);
                textView.setText("速度:" + speed + "档");
            }
        });

        // 速度滑动条
        seekBar2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int targetSpeed = progress + 1;
                if (targetSpeed > speed) {
                    for (int i = 0; i < targetSpeed - speed; i++) {
                        sendBleData(new byte[]{0x06});
                        vibrator.vibrate(30);
                    }
                } else if (targetSpeed < speed) {
                    for (int i = 0; i < speed - targetSpeed; i++) {
                        sendBleData(new byte[]{0x07});
                        vibrator.vibrate(30);
                    }
                }
                speed = targetSpeed;
                textView.setText("速度:" + speed + "档");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // 摇杆控制
        rockerView.setOnShakeListener(MyRockerView.DirectionMode.DIRECTION_4_ROTATE_45, new MyRockerView.OnShakeListener() {
            @Override
            public void onStart() {}

            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void direction(MyRockerView.Direction direction) {
                if (stop == 1) return;
                switch (direction) {
                    case DIRECTION_CENTER:
                        sendBleData(new byte[]{'P'});
                        text_show.setText("状态:停止行走");
                        break;
                    case DIRECTION_UP:
                        sendBleData(new byte[]{'G'});
                        vibrator.vibrate(100);
                        text_show.setText("状态:前进" + speed + "档");
                        break;
                    case DIRECTION_RIGHT:
                        sendBleData(new byte[]{'R'});
                        vibrator.vibrate(100);
                        text_show.setText("状态:右转" + speed + "档");
                        break;
                    case DIRECTION_DOWN:
                        sendBleData(new byte[]{'B'});
                        vibrator.vibrate(100);
                        text_show.setText("状态:后退" + speed + "档");
                        break;
                    case DIRECTION_LEFT:
                        sendBleData(new byte[]{'L'});
                        vibrator.vibrate(100);
                        text_show.setText("状态:左转" + speed + "档");
                        break;
                }
            }

            @Override
            public void onFinish() {}
        });
    }

    // 绑定所有控件
    private void bindViews() {
        seekBar2 = findViewById(R.id.seekBar2);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        myspinner = findViewById(R.id.myspinner);
        btn_scan_ble = findViewById(R.id.btn_scan_ble);
        imageView8 = findViewById(R.id.imageView8);
        imageView39 = findViewById(R.id.imageView39);
        imageView319 = findViewById(R.id.imageView319);
        imageView9 = findViewById(R.id.imageView9);
        imageView3419 = findViewById(R.id.imageView3419);
        imageView3319 = findViewById(R.id.imageView3319);
        imageView9543 = findViewById(R.id.imageView9543);
        imageView379 = findViewById(R.id.imageView379);
        imageView932 = findViewById(R.id.imageView932);
        text_show = findViewById(R.id.text_show);
        textViewwdb21z = findViewById(R.id.textViewwdb21z);
        textViewdy = findViewById(R.id.textViewdy);
        textViewwdb2z = findViewById(R.id.textViewwdb2z);
        textViewwdbz = findViewById(R.id.textViewwdbz);
        textView2 = findViewById(R.id.textView2);
        text_show873 = findViewById(R.id.text_show873);
        text_show5432 = findViewById(R.id.text_show5432);
        rockerView = findViewById(R.id.rocker_view);
        textView = findViewById(R.id.textView);
    }

    // BLE动态权限申请
    private void requestBlePermission() {
        List<String> perms = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            perms.add(Manifest.permission.BLUETOOTH_SCAN);
            perms.add(Manifest.permission.BLUETOOTH_CONNECT);
        } else {
            perms.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        perms.add(Manifest.permission.BLUETOOTH);
        perms.add(Manifest.permission.BLUETOOTH_ADMIN);

        List<String> needReq = new ArrayList<>();
        for (String p : perms) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                needReq.add(p);
            }
        }
        if (!needReq.isEmpty()) {
            ActivityCompat.requestPermissions(this, needReq.toArray(new String[0]), 1001);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 停止扫描
        if (isScanning) mScanner.stopScan(mScanCallback);
        // 断开BLE连接释放资源
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
    }
}