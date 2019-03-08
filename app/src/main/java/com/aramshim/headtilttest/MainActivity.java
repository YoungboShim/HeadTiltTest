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
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends AppCompatActivity {

    private final String TAG = MainActivity.class.getSimpleName();
    ImageView2 imageview2;
    TextView  mTextTarget;
    TextView mMenuNum, mMaxAngleX, mMaxAngleY;
    TextView mQuestion, mBTCheck, mSensorCheck;
    Button  btnMode, btnCondition, btnBluetooth;
    Button btnMaxXPlus, btnMaxXMinus, btnMaxYPlus, btnMaxYMinus,  btnMenuNumPlus, btnMenuNumMinus;
    EditText logIDInput;
    double headYaw = 0f, chestYaw = 0f, initYaw = 0f;
    double headPitch = 0f, initPitch = 0f;
    UsbSerialPort mPort;
    String logID = "test";

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    int maxAngleX = 80;
    int maxAngleY = 30;
    int screenWidth = 1280;
    int screenHeight = 720;
    Point centerPoint = new Point(screenWidth / 2, screenHeight / 2);
    int menuWidth = screenWidth;
    int menuHeight = 200;
    int menuNum = 1;
    int selectedX = 0;
    int selectedY = 0;
    SoundPool sound = new SoundPool(1, AudioManager.STREAM_MUSIC,0);
    int soundId;
    int target;
    Point fittsTarget;


    int numBlock = 3;
    int numRepeat = 2;
    ArrayList<Integer> numTargets = new ArrayList<>(Arrays.asList(15));
    ArrayList<Point> targets;
    int cnt = 0;
    int numTrial = 0;
    boolean onBlock = false;

    ArrayList<Point> fittsTargets= new ArrayList<Point>();;

    private boolean taskOn = false;
    private Timer dwellTimer;
    private TimerTask confirmTask;
    private boolean dwellMode = false;
    private boolean seatMode = true;
    private boolean fittsMode = true;

    private GestureClassifier gestureClassifier;

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
            headYaw = Double.parseDouble(strData.split(",")[3]);
            headPitch = Double.parseDouble(strData.split(",")[1]);
            double yaw = getYaw(headYaw - initYaw);
            double pitch = getPitch(headPitch - initPitch);
            boolean menuChanged = false;
            boolean isOnTarget = false;
            if(taskOn) {
                imageview2.setAngleX(yaw);
                imageview2.setAngleY(pitch);
                int tempX = (int) ((yaw + maxAngleX) / (maxAngleX * 2f / menuNum));
                if (tempX >= menuNum){
                    tempX = menuNum - 1;
                }
                int tempY = (int) ((pitch + maxAngleY) / (maxAngleY * 2f / menuNum));
                if (tempY >= menuNum){
                    tempY = menuNum - 1;
                }
                tempY = 0;
                menuChanged = checkMenuChanged(tempX, tempY);
                isOnTarget = checkOnTarget(centerPoint.x + (int) (yaw  / maxAngleX * (menuWidth / 2)));
                imageview2.setIsonTarget(isOnTarget);
                if(dwellMode && !isOnTarget){
                    confirmTask.cancel();
                    createTimerTask();
                    dwellTimer.schedule(confirmTask, 2000);
                }
                if(!dwellMode)
                {
                    if(gestureClassifier.updateData(headYaw, headPitch, !isOnTarget, System.currentTimeMillis()))
                    {
                        trialDone();
                    }
                }
                imageview2.setSelectedPosition(tempX, tempY);
            }
        }
    }

    public void trialDone() {
        taskOn = false;
        mTextTarget.setVisibility(View.VISIBLE);
        //mTextTarget.setText("Target : " + Integer.toString(target) + "\nSelected : " + Integer.toString(selectedX));
        mQuestion.setVisibility(View.VISIBLE);
        imageview2.setOnTrial(false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        soundId = sound.load(this, R.raw.click, 1);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            }
        }

        final BluetoothModule btModule = new BluetoothModule(this);
        BluetoothModule.onButtonClickCb callback = new BluetoothModule.onButtonClickCb() {
            @Override
            public void onButtonClick(String str) {
                String strData = null;
                strData = str;

                if (strData.trim().split(" ")[0].equals("l"))
                {
                    if (onBlock)
                        mQuestion.setText("Question : Yes");
                    else
                        mBTCheck.setText("l");
                }
                else if (strData.trim().split(" ")[0].equals("r"))
                {
                    if (onBlock)
                        mQuestion.setText("Question : No");
                    else
                        mBTCheck.setText("r");
                }
                else if (strData.trim().split(" ")[0].equals("c"))
                {
                    if (fittsMode) {
                        if (onBlock) {
                            taskOn = true;
                            imageview2.setOnTrial(true);
                            initYaw = headYaw;
                            initPitch = headPitch;
                            mQuestion.setText("Question : ");
                            mQuestion.setVisibility(View.INVISIBLE);
                            mTextTarget.setVisibility(View.INVISIBLE);
                            fittsTarget = fittsTargets.remove(new Random().nextInt(fittsTargets.size()));
                            Log.d("target",fittsTarget.x + "");
                            imageview2.setFittsTarget(fittsTarget);
                            if (dwellMode) {
                                confirmTask.cancel();
                                createTimerTask();
                                dwellTimer.schedule(confirmTask, 2000);
                            }
                            mSensorCheck.setText(cnt + " / " + numTrial);
                            cnt++;
                        } else {
                            onBlock = true;
                            fittsTargets.add(new Point(100, 25));
                            fittsTargets.add(new Point(150, 50));
                            fittsTargets.add(new Point(200, 75));
                            fittsTargets.add(new Point(300, 25));
                            fittsTargets.add(new Point(350, 50));
                            fittsTargets.add(new Point(400, 75));
                            fittsTargets.add(new Point(450, 25));
                            fittsTargets.add(new Point(500, 50));
                            fittsTargets.add(new Point(550, 75));
                            numTrial = fittsTargets.size();
                            mSensorCheck.setText(cnt + " / " + numTrial);
                            cnt++;
                            mBTCheck.setVisibility(View.INVISIBLE);
                            //mSensorCheck.setVisibility(View.INVISIBLE);
                        }
                    } else {
                        if (onBlock)
                        {
                            taskOn = true;
                            imageview2.setOnTrial(true);
                            initYaw = headYaw;
                            initPitch = headPitch;
                            mQuestion.setText("Question : ");
                            mQuestion.setVisibility(View.INVISIBLE);
                            Point tempTarget = targets.remove(new Random().nextInt(targets.size()));
                            target = tempTarget.x;
                            menuNum = tempTarget.y + 1;
                            imageview2.setMenuNum(tempTarget.y + 1);
                            target = new Random().nextInt(menuNum);
                            mTextTarget.setVisibility(View.INVISIBLE);
                            imageview2.setTarget(target);
                            if(dwellMode) {
                                confirmTask.cancel();
                                createTimerTask();
                                dwellTimer.schedule(confirmTask, 2000);
                            }
                            mSensorCheck.setText(cnt + " / " + numTrial);
                            cnt++;
                        }
                        else
                        {
                            onBlock = true;
                            targets = new ArrayList<Point>();
                            for (int num: numTargets) {
                                for (int i = 0; i < num; i++) {
                                    for (int j = 0; j < numRepeat; j++) {
                                        targets.add(new Point(i, num-1));
                                        numTrial++;
                                    }
                                }
                            }
                            mSensorCheck.setText(cnt + " / " + numTrial);
                            cnt++;
                            mBTCheck.setVisibility(View.INVISIBLE);
                            //mSensorCheck.setVisibility(View.INVISIBLE);
                        }
                    }
                }
            }
        };
        btModule.setOnButtonClickCb(callback);

        mTextTarget = (TextView) findViewById(R.id.textViewTarget);
        mMaxAngleX = (TextView) findViewById(R.id.textViewMaxAngleX);
        mMaxAngleY = (TextView) findViewById(R.id.textViewMaxAngleY);
        mMenuNum = (TextView) findViewById(R.id.textViewMenuNum);
        mQuestion = (TextView) findViewById(R.id.textViewQuestion);
        mQuestion.setVisibility(View.INVISIBLE);
        mBTCheck = (TextView) findViewById(R.id.textViewBTCheck);
        mSensorCheck = (TextView) findViewById(R.id.textViewSensorCheck);
        imageview2 = (ImageView2)findViewById(R.id.graphicView);
        imageview2.setCenterPoint(centerPoint);
        imageview2.setMenuSize(menuWidth,menuHeight);
        imageview2.setMenuNum(menuNum);
        imageview2.setMaxAngleX(maxAngleX);
        imageview2.setMaxAngleY(maxAngleY);
        imageview2.setSelectedPosition(selectedX,selectedY);
        mMaxAngleX.setText(maxAngleX+"");
        mMaxAngleY.setText(maxAngleY+"");

        btnMode = (Button) findViewById(R.id.buttonMode);
        btnMode.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dwellMode)
                {
                    dwellMode = false;
                    btnMode.setText("NOD");
                } else
                {
                    dwellMode = true;
                    btnMode.setText("DWELL");
                }
            }
        });

        btnCondition = (Button) findViewById(R.id.buttonCondition);
        btnCondition.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (seatMode)
                {
                    seatMode = false;
                    btnCondition.setText("WALK");
                } else
                {
                    seatMode = true;
                    btnCondition.setText("SEAT");
                }
            }
        });

        btnBluetooth = (Button) findViewById(R.id.buttonBT);
        btnBluetooth.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
               btModule.selectBluetoothDevice();
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
        try {
            gestureClassifier = new GestureClassifier(this);
        } catch (IOException e) {
            e.printStackTrace();
        }

        /*
        if(dwellMode) {
            createTimerTask();
            dwellTimer = new Timer();
        }
        else{
            try {
                gestureClassifier = new GestureClassifier(this);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        */
    }

    @Override
    protected void onResume(){
        super.onResume();
    }


    private void createTimerTask(){
        confirmTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new TimerTask() {
                    @Override
                    public void run() {
                        trialDone();
                    }
                });
            }
        };
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

    private boolean checkOnTarget(int x)
    {
        if (fittsTarget.x - fittsTarget.y /2 <= x && x <= fittsTarget.x + fittsTarget.y / 2)
        {
            return true;
        } else {
            return false;
        }
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
            mSensorCheck.setText("No available driver");
            return;
        }

        // Open a connection to the first available driver.
        UsbSerialDriver driver = availableDrivers.get(0);

        String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
        PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        manager.requestPermission(driver.getDevice(),mPermissionIntent);

        UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
        if (connection == null) {
            mSensorCheck.setText("No connection");
            return;
        }

        // Read data
        mPort = driver.getPorts().get(0);
        try {
            mPort.open(connection);
            mPort.setParameters(921600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            mSensorCheck.setText("Connection Successful");
        }catch (IOException e) {
            mSensorCheck.setText("Connection failed");
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
}
