package com.glacier.seproject;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    /**
     * developer : Glacier (한병하 2018111316)
     * name      : smart window controller
     * project   : KNU HUSTAR SE FINAL PROJECT
     * keystore  : glacier key0 glacier
     */

    FrameLayout btnMain;
    ImageView mBtnConnect;
    BluetoothAdapter mBluetoothAdapter;
    Set<BluetoothDevice> mPairedDevices;
    List<String> mListPairedDevices;

    Handler mBluetoothHandler;
    ConnectedBluetoothThread mThreadConnectedBluetooth;
    BluetoothDevice mBluetoothDevice;
    BluetoothSocket mBluetoothSocket;

    final static int BT_REQUEST_ENABLE = 1;
    final static int BT_MESSAGE_READ = 2;
    final static int BT_CONNECTING_STATUS = 3;
    final static UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private boolean isChecked = false;
    private boolean isChecked2 = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        mBtnConnect = findViewById(R.id.btn_con);
        TextView btnIntro = findViewById(R.id.btn_intro);
        TextView btnMode = findViewById(R.id.btn_mode);

        // 뒤로가기 버튼 클릭리스너
        findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        // 블루투스 어댑터 초기화 및 페어링 버튼 클릭리스너
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBtnConnect.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                listPairedDevices();
            }
        });

        // 프로젝트 소개 화면 이동 클릭리스너
        btnIntro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, IntroduceActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });

        // 창문 컨트롤 버튼 클릭리스너
        btnMain = findViewById(R.id.btn_main);
        btnMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    if (isChecked) {
                        isChecked = false;
                        // 창문이 닫혀있을 경우 아두이노로 "1" 데이터를 전송함 (창문 OPEN)
                        write("1");
                        Toast.makeText(getApplicationContext(), "스마트 창문을 엽니다!", Toast.LENGTH_SHORT).show();
                    } else {
                        isChecked = true;
                        // 창문이 열려있을 경우 아두이노로 "2" 데이터를 전송함 (창문 CLOSE)
                        write("2");
                        Toast.makeText(getApplicationContext(), "스마트 창문을 닫습니다!", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    // 블루투스가 연결되어있지 않으면 데이터 전송단계에서 오류가발생하여 예외처리 후 기기를 연결하라는 토스트메세지 출력
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "기기를 먼저 연결해주세요", Toast.LENGTH_SHORT).show();
                }

            }
        });

        btnMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    if (isChecked2) {
                        isChecked2 = false;
                        write("3");
                        // 자동모드로 설정되어있을 경우 아두이노로 "3" 데이터를 전송함 (수동모드 변경)
                        Toast.makeText(getApplicationContext(), "지금부터 수동모드로 작동합니다!", Toast.LENGTH_SHORT).show();
                    } else {
                        isChecked2 = true;
                        write("4");
                        // 자동모드로 설정되어있을 경우 아두이노로 "4" 데이터를 전송함 (자동모드 변경)
                        Toast.makeText(getApplicationContext(), "지금부터 자동모드로 작동합니다!", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    // 블루투스가 연결되어있지 않으면 데이터 전송단계에서 오류가발생하여 예외처리 후 기기를 연결하라는 토스트메세지 출력
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "기기를 먼저 연결해주세요", Toast.LENGTH_SHORT).show();
                }

            }
        });


        /**
         *
         * TODO ---- 블루투스 수신단 ---- (코드상 빨간줄이랑 경고는 퍼미션처리 안해줘서 뜨는건데, 블루투스는 따로 퍼미션체크 안해줘도 되서 무시하면 됨)
         * TODO ---- ANDROID 버전 12부터 블루투스 권한 강화로 앱이 튕기는 현상 발견. 추후 수정예정 (일단은 12이하 버전 기기로 테스트)
         */
        mBluetoothHandler = new Handler() {
            @SuppressLint("HandlerLeak")
            public void handleMessage(android.os.Message msg) {
                if (msg.what == BT_MESSAGE_READ) {
                    String readMessage = null;
                    try {
                        readMessage = new String((byte[]) msg.obj, "UTF-8");

                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    //mTvReceiveData.setText(readMessage);
                    if (readMessage.trim().contains("0")) {
                        Toast.makeText(getApplicationContext(), "신호 수신", Toast.LENGTH_SHORT).show();

                    } else {
                        Log.d("BLE", "wrong operation input");
                    }

                }
            }
        };
    }

    /**
     * TODO ---- 블루투스 송신단 ----
     */
    public void write(String input) {
        byte[] bytes = input.getBytes();
        try {
            mBluetoothSocket.getOutputStream().write(bytes);
        } catch (IOException e) {
        }
    }

    // 현재 페어링된 디바이스 리스트를 보여줌
    void listPairedDevices() {
        if (mBluetoothAdapter.isEnabled()) {
            mPairedDevices = mBluetoothAdapter.getBondedDevices();

            if (mPairedDevices.size() > 0) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("장치 선택");

                mListPairedDevices = new ArrayList<String>();
                for (BluetoothDevice device : mPairedDevices) {
                    mListPairedDevices.add(device.getName());
                }
                final CharSequence[] items = mListPairedDevices.toArray(new CharSequence[mListPairedDevices.size()]);
                mListPairedDevices.toArray(new CharSequence[mListPairedDevices.size()]);

                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int item) {
                        connectSelectedDevice(items[item].toString());

                    }
                });
                AlertDialog alert = builder.create();
                alert.show();
            } else {
                Toast.makeText(getApplicationContext(), "페어링된 장치가 없습니다.", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(getApplicationContext(), "블루투스가 비활성화 되어 있습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    // 선택한 디바이스를 연결
    void connectSelectedDevice(String selectedDeviceName) {
        for (BluetoothDevice tempDevice : mPairedDevices) {
            if (selectedDeviceName.equals(tempDevice.getName())) {
                mBluetoothDevice = tempDevice;
                break;
            }
        }
        try {
            mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(BT_UUID);
            mBluetoothSocket.connect();
            mThreadConnectedBluetooth = new ConnectedBluetoothThread(mBluetoothSocket);
            mThreadConnectedBluetooth.start();
            mBluetoothHandler.obtainMessage(BT_CONNECTING_STATUS, 1, -1).sendToTarget();
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "블루투스 연결 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
            Log.e("ble", e.toString());
        }
    }

    // 뒤로가기키 누르면 앱 종료되게
    @Override
    public void onBackPressed() {
        try {
            mBluetoothSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        finish();
    }

    // 블루투스 연결 쓰레드 클래스
    private class ConnectedBluetoothThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedBluetoothThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "블루투스 연결 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = mmInStream.available();
                    if (bytes != 0) {
                        SystemClock.sleep(100);
                        bytes = mmInStream.available();
                        bytes = mmInStream.read(buffer, 0, bytes);
                        mBluetoothHandler.obtainMessage(BT_MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                    }
                } catch (IOException e) {
                    break;
                }
            }
        }
    }

}