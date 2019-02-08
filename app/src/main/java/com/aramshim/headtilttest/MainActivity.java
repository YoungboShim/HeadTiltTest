package com.aramshim.headtilttest;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends AppCompatActivity {

    private final String TAG = MainActivity.class.getSimpleName();
    ImageView2 imageview2;
    TextView mText1, mText2, mText3, mTextAnswer;
    TextView mMenuNum, mMaxAngleX, mMaxAngleY;
    Button mButton, btnStart, btnEnd, btnCheck;
    Button btnMaxXPlus, btnMaxXMinus, btnMaxYPlus, btnMaxYMinus,  btnMenuNumPlus, btnMenuNumMinus;
    EditText logIDInput;
    double headYaw = 0f, chestYaw = 0f, initYaw = 0f;
    double headPitch = 0f, initPitch = 0f;
    UsbSerialPort mPort;
    DataLogger dLogger;
    String logID = "test";

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    int maxAngleX = 60;
    int maxAngleY = 30;

    int screenWidth = 1280;
    int screenHeight = 720;

    Point centerPoint = new Point(screenWidth / 2, screenHeight / 2 - 50);

    int menuWidth = screenWidth;
    int menuHeight = 200;
    int menuNum = 1;

    int selectedX = 0;
    int selectedY = 0;

    SoundPool sound = new SoundPool(1, AudioManager.STREAM_MUSIC,0);
    int soundId;

    private boolean taskOn = false;
    private Timer dwellTimer;
    private TimerTask confirmTask;

    private SerialInputOutputManager mSerialIoManager;

    private final SerialInputOutputManager.Listener mListener =
            new SerialInputOutputManager.Listener() {

                @Override
                public void onRunError(Exception e) {
                    Log.d(TAG, "Runner stopped.");
                }

                @Override
                public void onNewData(final byte[] data) {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MainActivity.this.updateReceivedData(data);
                        }
                    });
                }
            };


    private void updateReceivedData(byte[] data) {
        String strData = null;
        try {
            strData = new String(data, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        if(strData.split(",")[0].equals("100-0")) {
            mText1.setText(strData);
            headYaw = Double.parseDouble(strData.split(",")[3]);
            headPitch = Double.parseDouble(strData.split(",")[1]);
            double yaw = getYaw(headYaw - initYaw);
            double pitch = getPitch(headPitch - initPitch);

            if(taskOn) {
                imageview2.setAngleX(yaw);
                imageview2.setAngleY(pitch);
                int tempX = (int) ((yaw + maxAngleX) / (maxAngleX * 2f / menuNum));
                if (tempX >= menuNum) tempX = menuNum - 1;
                int tempY = (int) ((pitch + maxAngleY) / (maxAngleY * 2f / menuNum));
                if (tempY >= menuNum) tempY = menuNum - 1;
                tempY = 0;
                if(checkMenuChanged(tempX, tempY)){
                    confirmTask.cancel();
                    createTimerTask();
                    dwellTimer.schedule(confirmTask, 2000);
                }
                imageview2.setSelectedPosition(tempX, tempY);

                try {
                    dLogger.write(strData);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            mText3.setText("Rotation: " + String.valueOf(yaw));
        }
        else if(strData.split(",")[0].equals("100-1")) {
            mText2.setText(strData);
            chestYaw = Double.parseDouble(strData.split(",")[3]);
            //double rotation = headYaw - chestYaw - initAngle;
            //imageview2.setAngle(rotation);
            //mText3.setText("Rotation: " + String.valueOf(rotation));
        }
    }

    private double getYaw(double angle) {
        double angleX;
        angleX = angle;
        if (angle > 180)
            angleX = angle - 360;
        else if (angle < - 180)
            angleX = 360 + angle;
        if (angleX > maxAngleX)
        {
            angleX = maxAngleX;
        } else if (angleX < -maxAngleX)
        {
            angleX = - maxAngleX;
        }
        return angleX;
    }

    private double getPitch(double angle) {
        double angleY;
        angleY = angle;
        if (angle > 180)
            angleY = angle - 360;
        else if (angle < - 180)
            angleY = 360 + angle;
        if (angleY > maxAngleY)
        {
            angleY= maxAngleY;
        } else if (angleY < -maxAngleY)
        {
            angleY = - maxAngleY;
        }
        angleY = -angleY;
        return angleY;
    }

    private boolean checkMenuChanged(int x, int y)
    {
        if (x != selectedX || y != selectedY)
        {
            int streamId = sound.play(soundId, 1.0F, 1.0F,  1,  0,  1.0F);
            selectedX = x;
            selectedY = y;
            return true;
        }
        else
        {
            return false;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //requestWindowFeature(Window.FEATURE_NO_TITLE); // Remove title bar;

        soundId = sound.load(this, R.raw.click, 1);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            }
        }

        try {
            dLogger = new DataLogger(this);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mText1 = (TextView) findViewById(R.id.textView);
        mText2 = (TextView) findViewById(R.id.textView2);
        mText3 = (TextView) findViewById(R.id.textView3);
        mTextAnswer = (TextView) findViewById(R.id.textViewAnswer);
        mText1.setVisibility(View.INVISIBLE);
        mText2.setVisibility(View.INVISIBLE);
        mText3.setVisibility(View.INVISIBLE);

        mMaxAngleX = (TextView) findViewById(R.id.maxAngleX);
        mMaxAngleY = (TextView) findViewById(R.id.maxAngleY);
        mMenuNum = (TextView) findViewById(R.id.textViewMenuNum);

        imageview2 = (ImageView2)findViewById(R.id.graphicView);
        imageview2.setCenterPoint(centerPoint);
        imageview2.setMenuSize(menuWidth,menuHeight);
        imageview2.setMenuNum(menuNum);
        imageview2.setMaxAngleX(maxAngleX);
        imageview2.setMaxAngleY(maxAngleY);
        imageview2.setSelectedPosition(selectedX,selectedY);
        mMaxAngleX.setText(maxAngleX+"");
        mMaxAngleY.setText(maxAngleY+"");

        mButton = (Button) findViewById(R.id.button);
        mButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                taskOn = true;
                imageview2.modeChange(true);
                initYaw = headYaw;
                initPitch = headPitch;
                confirmTask.cancel();
                createTimerTask();
                dwellTimer.schedule(confirmTask, 2000);
            }
        });

        btnStart = (Button) findViewById(R.id.buttonStart);
        //btnStart.setVisibility(View.INVISIBLE);
        btnStart.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    dLogger.start(logID);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        btnEnd = (Button) findViewById(R.id.buttonEnd);
        //btnEnd.setVisibility(View.INVISIBLE);
        btnEnd.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    dLogger.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        btnCheck = (Button) findViewById(R.id.buttonCheck);
        btnCheck.setVisibility(View.INVISIBLE);
        btnCheck.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    dLogger.write("Check!\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        btnMaxXPlus = (Button) findViewById(R.id.buttonAngleXPlus);
        btnMaxXPlus.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                maxAngleX = maxAngleX + 5;
                if (maxAngleX > 90) maxAngleX = 90;
                imageview2.setMaxAngleX(maxAngleX);
                mMaxAngleX.setText(maxAngleX+"");
            }
        });

        btnMaxXMinus = (Button) findViewById(R.id.buttonAngleXMinus);
        btnMaxXMinus.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                maxAngleX = maxAngleX - 5;
                if (maxAngleX < 10) maxAngleX = 10;
                imageview2.setMaxAngleX(maxAngleX);
                mMaxAngleX.setText(maxAngleX+"");
            }
        });

        btnMaxYPlus = (Button) findViewById(R.id.buttonAngleYPlus);
        btnMaxYPlus.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                maxAngleY = maxAngleY + 5;
                if (maxAngleY > 90) maxAngleY = 90;
                imageview2.setMaxAngleY(maxAngleY);
                mMaxAngleY.setText(maxAngleY+"");
            }
        });

        btnMaxYMinus = (Button) findViewById(R.id.buttonAngleYMinus);
        btnMaxYMinus.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                maxAngleY = maxAngleY - 5;
                if (maxAngleY < 10) maxAngleY = 10;
                imageview2.setMaxAngleY(maxAngleY);
                mMaxAngleY.setText(maxAngleY+"");
            }
        });

        btnMenuNumPlus = (Button) findViewById(R.id.buttonMenuNumPlus);
        btnMenuNumPlus.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                menuNum = menuNum + 1;
                if (menuNum > 10) menuNum = 10;
                imageview2.setMenuNum(menuNum);
                mMenuNum.setText(menuNum+"");
            }
        });

        btnMenuNumMinus = (Button) findViewById(R.id.buttonMenuNumMinus);
        btnMenuNumMinus.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                menuNum = menuNum - 1;
                if (menuNum < 1) menuNum = 1;
                imageview2.setMenuNum(menuNum);
                mMenuNum.setText(menuNum+"");
            }
        });

        logIDInput = (EditText)findViewById(R.id.editText);
        logIDInput.setVisibility(View.INVISIBLE);
        logIDInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                logID = s.toString();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                logID = s.toString();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        onDeviceStateChange();

        createTimerTask();
        dwellTimer = new Timer();
    }

    @Override
    protected void onResume(){
        super.onResume();
    }

    private void stopIoManager() {
        if (mSerialIoManager != null) {
            Log.i(TAG, "Stopping io manager ..");
            mSerialIoManager.stop();
            mSerialIoManager = null;
        }
    }

    private void startIoManager() {
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (availableDrivers.isEmpty()) {
            mText1.setText("No available driver");
            return;
        }

        // Open a connection to the first available driver.
        UsbSerialDriver driver = availableDrivers.get(0);

        String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
        PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        manager.requestPermission(driver.getDevice(),mPermissionIntent);

        UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
        if (connection == null) {
            mText1.setText("No connection");
            return;

        }

        // Read data
        mPort = driver.getPorts().get(0);
        try {
            mPort.open(connection);
            mPort.setParameters(921600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
        }catch (IOException e) {
            mText1.setText("Connection failed");
            return;
        }
        if (mPort != null) {
            Log.i(TAG, "Starting io manager ..");
            mSerialIoManager = new SerialInputOutputManager(mPort, mListener);
            mExecutor.submit(mSerialIoManager);
        }
    }

    private void onDeviceStateChange() {
        stopIoManager();
        startIoManager();
    }

    private void createTimerTask(){
        confirmTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new TimerTask() {
                    @Override
                    public void run() {
                        taskOn = false;
                        mTextAnswer.setText(Integer.toString(selectedX));
                        imageview2.modeChange(false);
                    }
                });
            }
        };
    }
}
