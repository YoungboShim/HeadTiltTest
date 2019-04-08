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
    TextView mQuestion, mBTCheck, mSensorCheck;
    Button  btnMode, btnCondition, btnBluetooth;

    TextView mMenuNum, mMaxAngleX, mMaxAngleY;
    Button btnMaxXPlus, btnMaxXMinus, btnMaxYPlus, btnMaxYMinus,  btnMenuNumPlus, btnMenuNumMinus;

    EditText logIDInput;
    double headYaw = 0f, initYaw = 0f;
    double headPitch = 0f, initPitch = 0f;
    double headRoll = 0f;

    UsbSerialPort mPort;
    String logID = "test";

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    int maxAngleX = 55;
    int maxAngleY = 60;
    int screenWidth = 1280;
    int screenHeight = 720;
    Point centerPoint = new Point(screenWidth / 2, screenHeight / 2);
    int menuWidth = screenWidth;
    int menuHeight = 150;
    int menuNum = 1;
    int selectedX = 0;
    int selectedY = 0;
    SoundPool sound = new SoundPool(1, AudioManager.STREAM_MUSIC,0);
    int soundId;

    int step = 1;

    int target;
    Point fittsTarget;
    Point fittsTarget2;

    int numBlock = 3;
    int numRepeat = 12;

    ArrayList<Integer> numTargets = new ArrayList<>(Arrays.asList(menuNum));
    ArrayList<Point> targets;

    int cntTrial = 0;
    int numTrial = 0;

    ArrayList<Point> list_hTargets= new ArrayList<Point>();
    ArrayList<Point> list_vTargets= new ArrayList<Point>();

    private DataLogger dLogger;
    private DataLogger dLogger_result;

    private Timer dwellTimer;
    private TimerTask confirmTask;

    private boolean dwellMode = false;
    private boolean seatMode = true;
    private boolean fittsMode = true;
    private boolean twoStepMode = true;

    private GestureClassifier gestureClassifier;

    private SerialInputOutputManager mSerialIoManager;

    private State state = State.INIT;
    private boolean isOnTarget = false;

    ArrayList<Integer> list_htargetDistance =  new ArrayList<Integer>(Arrays.asList(0, 28, 56));
    ArrayList<Integer> list_htargetSize =  new ArrayList<Integer>(Arrays.asList(12));
    ArrayList<Integer> list_vtargetDistance =  new ArrayList<Integer>(Arrays.asList(0, 25, 50));
    ArrayList<Integer> list_vtargetSize =  new ArrayList<Integer>(Arrays.asList(8, 16));

    private long startTime = 0;
    private long endTime;
    private int numCross;
    private boolean previousIsOnTarget;
    private int currentBlock = 0;
    private int comfortLevel = -1;
    private double endPoint;
    private int currentCondition = 0;
    private Timer mTimer;

    Point tempStep2Target = new Point(360 - 30 * 8, 15 * 8);

    public enum State {
        INIT, BLOCK_BREAK, TRIAL_BREAK, TRIAL
    }

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

        if(strData.split(",")[0].equals("100-1")) {
            headYaw = Double.parseDouble(strData.split(",")[3]);
            headPitch = Double.parseDouble(strData.split(",")[1]);
            double yaw = getYaw(headYaw - initYaw);
            double pitch = getPitch(headPitch - initPitch);
            boolean menuChanged = false;

            if(state == State.TRIAL) {
                endTime = System.currentTimeMillis();
                if (endTime - startTime >= 5000)
                {
                    confirmTask.cancel();
                    mTextTarget.setText("Failed");
                    endTime = startTime + 5000;
                    try {
                        //dLogger.trace_write(strData);
                        if (fittsMode)
                            dLogger.just_write(currentBlock + "," + cntTrial + "," + (fittsTarget.x-640)/8 + "," + fittsTarget.y/8 + "," + (endTime - startTime) + ","+ strData);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    trialDone();
                    mTextTarget.setVisibility(View.VISIBLE);
                    return;
                }

                mTextTarget.setVisibility(View.INVISIBLE);
                imageview2.setAngleX(yaw);
                imageview2.setAngleY(pitch);
                endPoint = yaw;

                if (fittsMode) {
                    previousIsOnTarget = isOnTarget;
                    isOnTarget = checkOnTarget(centerPoint.x + (int) (yaw  / maxAngleX * (menuWidth / 2)));
                    if (isOnTarget == false && previousIsOnTarget == true)
                        numCross++;
                    imageview2.setIsonTarget(isOnTarget);

                    if (twoStepMode) {
                        if (step == 1)
                        {
                            isOnTarget = checkOnTarget(centerPoint.x + (int) (yaw  / maxAngleX * (menuWidth / 2)));
                            if (isOnTarget == true && Math.abs(pitch) > 10)
                            {
                                step = 2;
                                imageview2.setStep(2);
                            }
                        } else if (step == 2) {
                            isOnTarget = checkOnTarget2(centerPoint.y + (int) (pitch  / maxAngleY * (720 / 2)));
                            imageview2.setIsonTarget(isOnTarget);
                            if(!isOnTarget){
                                confirmTask.cancel();
                                createTimerTask();
                                dwellTimer.schedule(confirmTask, 500);
                            }
                        }
                    } else  {
                        if(dwellMode && !isOnTarget){
                            confirmTask.cancel();
                            createTimerTask();
                            dwellTimer.schedule(confirmTask, 500);
                        }
                        if(!dwellMode)
                        {
                            if(gestureClassifier.updateData(headYaw, headPitch, !isOnTarget, System.currentTimeMillis()))
                                trialDone();
                        }
                    }

                } else if (!fittsMode) {
                    int tempX = (int) ((yaw + maxAngleX) / (maxAngleX * 2f / menuNum));
                    if (tempX >= menuNum){
                        tempX = menuNum - 1;
                    }
                    int tempY = (int) ((pitch + maxAngleY) / (maxAngleY * 2f / menuNum));
                    if (tempY >= menuNum){
                        tempY = menuNum - 1;
                    }
                    //tempY = 0;
                    menuChanged = checkMenuChanged(tempX, tempY);
                    if(dwellMode && menuChanged){
                        confirmTask.cancel();
                        createTimerTask();
                        dwellTimer.schedule(confirmTask, 500);
                    }
                    if(!dwellMode)
                    {
                        if(gestureClassifier.updateData(headYaw, headPitch, menuChanged, System.currentTimeMillis()))
                        {
                            trialDone();
                        }
                    }
                    imageview2.setSelectedPosition(tempX, tempY);
                }

                try {
                    //dLogger.trace_write(strData);
                    if (fittsMode)
                        dLogger.just_write(currentBlock + "," + cntTrial + "," + (fittsTarget.x-640)/8 + "," + fittsTarget.y/8 + "," + (endTime - startTime) + ","+ strData);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    public void trialDone() {
        state  = State.TRIAL_BREAK;
        isOnTarget = false;
        mQuestion.setVisibility(View.VISIBLE);
        mTextTarget.setVisibility(View.INVISIBLE);
        imageview2.setOnTrial(false);
        imageview2.setIsonTarget(false);
        imageview2.setStep(1);
        step = 1;
        //endTime = System.currentTimeMillis();

        // set block or task termination term here
        /*
        if(cnt>=numTrial) {
            try {
                dLogger.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        */
    }

    class CustomTimer extends TimerTask{
        @Override
        public void run() {
            cntTrial += 1;
            numCross = 0;
            comfortLevel = -1;
            initYaw = headYaw;
            initPitch = headPitch;
            startTime = System.currentTimeMillis();
            state = State.TRIAL;
            imageview2.setAngleX(0);
            imageview2.setAngleY(0);
            imageview2.setOnTrial(true);
            if (fittsMode) {
                //fittsTarget = list_hTargets.remove(new Random().nextInt(list_hTargets.size()));
                fittsTarget = list_hTargets.remove(0);
                fittsTarget2 = list_vTargets.remove(0);
                imageview2.setFittsTarget(fittsTarget);
                imageview2.setFittsTarget2(fittsTarget2);
            }
            else {
                Point tempTarget = targets.remove(new Random().nextInt(targets.size()));
                imageview2.setMenuNum(tempTarget.y + 1);
                imageview2.setTarget(tempTarget.x);
            }
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
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
                    if (state == State.TRIAL_BREAK) {
                        mQuestion.setText("Question : Yes");
                        comfortLevel = 1;
                    }
                    else
                        mBTCheck.setText("l");
                }
                else if (strData.trim().split(" ")[0].equals("r"))
                {
                    if (state == State.TRIAL_BREAK) {
                        mQuestion.setText("Question : No");
                        comfortLevel = 0;
                    }
                    else
                        mBTCheck.setText("r");
                }
                else if (strData.trim().split(" ")[0].equals("c"))
                {
                    if (true) {
                        if (state == State.INIT) {
                            if (System.currentTimeMillis() - startTime < 10000) {
                                return;
                            }

                            mBTCheck.setVisibility(View.INVISIBLE);
                            logIDInput.setVisibility(View.INVISIBLE);
                            btnCondition.setVisibility(View.INVISIBLE);
                            btnMode.setVisibility(View.INVISIBLE);
                            mTextTarget.setVisibility(View.VISIBLE);

                            currentCondition++;
                            mTextTarget.setText("Start Condition " + currentCondition);

                            state = State.BLOCK_BREAK;
                            currentBlock = 0;

                            try {
                                dLogger = new DataLogger(getApplicationContext());
                                dLogger_result = new DataLogger((getApplicationContext()));

                                if (seatMode == true)
                                {
                                    if (dwellMode == true)
                                    {
                                        dLogger.start(logID+"sit_dwell_trace");
                                        dLogger_result.start(logID+"sit_dwell_result");
                                    } else {
                                        dLogger.start(logID+"sit_nod_trace");
                                        dLogger_result.start(logID+"sit_nod_result");
                                    }
                                } else {
                                    if (dwellMode == true)
                                    {
                                        dLogger.start(logID+"walk_dwell_trace");
                                        dLogger_result.start(logID+"walk_dwell_result");
                                    } else {
                                        dLogger.start(logID+"walk_nod_trace");
                                        dLogger_result.start(logID+"walk_nod_result");
                                    }
                                }
                                dLogger.just_write("Block, Trial, Angle, Size, Time,ID,Roll,Pitch,Yaw,DistX,DistY,DistZ,Battery\n");
                                dLogger_result.just_write("Block, Trial, Angle, Size, Condition, Gesture, Response time, Cross, Comfort, Error\n");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else if (state == State.TRIAL_BREAK) {
                            if (cntTrial != 0 && comfortLevel == -1)
                                return;

                            if (cntTrial > 0) {
                                try {
                                    if (fittsMode){
                                    dLogger_result.just_write(currentBlock + "," + cntTrial + "," + (fittsTarget.x-640)/8 + "," + fittsTarget.y/8
                                            + "," + seatMode + "," + dwellMode + "," + (endTime - startTime)  + "," +  numCross  + "," + comfortLevel + "," + (endPoint - (fittsTarget.x-640)/8)+"\n");}
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }

                            if (cntTrial >= numTrial)
                            {
                                mTextTarget.setVisibility(View.VISIBLE);
                                mTextTarget.setText("Block " + currentBlock + " end");
                                mQuestion.setVisibility(View.INVISIBLE);
                                state = State.BLOCK_BREAK;
                                imageview2.setFittsTarget(new Point(0,0));
                                imageview2.setFittsTarget2(new Point(0,0));
                                startTime = System.currentTimeMillis();
                                return;
                            }

                            mTimer = new Timer();
                            mTimer.schedule(new CustomTimer(), 1000);

                            mTextTarget.setVisibility(View.VISIBLE);
                            mTextTarget.setText("Please look ahead");
                            mSensorCheck.setText((cntTrial + 1) + " / " + numTrial);
                            mQuestion.setText("Question : ");
                            mQuestion.setVisibility(View.INVISIBLE);

                            imageview2.setFittsTarget(new Point(0,0));
                            imageview2.setFittsTarget2(new Point(0,0));
                            imageview2.setAngleX(0);

                        } else if (state == State.BLOCK_BREAK) {

                            if (currentBlock < numBlock) {
                                if (System.currentTimeMillis() - startTime < 10000) {
                                    return;
                                }

                                currentBlock++;
                                mTextTarget.setText("Block " + currentBlock + " start");
                                state = State.TRIAL_BREAK;

                                if (fittsMode) {
                                    list_hTargets = new ArrayList<Point>();
                                    for (int size : list_htargetSize) {
                                        for (int distance : list_htargetDistance) {
                                            for (int i = 0; i < numRepeat ; i++) {
                                                list_hTargets.add(new Point(640 + distance * (1280 / 160), size * (1280 / 160)));
                                                list_hTargets.add(new Point(640 - distance * (1280 / 160), size * (1280 / 160)));
                                            }
                                        }
                                    }

                                    list_vTargets = new ArrayList<Point>();
                                    for (int i = 0; i < numRepeat / 2 ; i++){
                                        for (int size : list_vtargetSize){
                                            for (int distance : list_vtargetDistance) {
                                                list_vTargets.add(new Point(360 - distance * (720 / maxAngleY / 2), size * (720 / maxAngleY / 2)));
                                                list_vTargets.add(new Point(360 + distance * (720 / maxAngleY / 2), size * (720 / maxAngleY / 2)));
                                            }
                                        }
                                    }
                                    numTrial = list_hTargets.size();
                                } else {
                                    targets = new ArrayList<Point>();
                                    for (int num: numTargets) {
                                        for (int i = 0; i < num; i++) {
                                            for (int j = 0; j < numRepeat; j++) {
                                                targets.add(new Point(i, num-1));
                                                numTrial++;
                                            }
                                        }
                                    }
                                }
                                cntTrial = 0;
                                mSensorCheck.setText(cntTrial + " / " + numTrial);
                            } else {
                                try {
                                    btnCondition.setVisibility(View.VISIBLE);
                                    btnMode.setVisibility(View.VISIBLE);
                                    state = State.INIT;
                                    mTextTarget.setText("Finished");
                                    dLogger.close();
                                    dLogger_result.close();
                                    startTime = System.currentTimeMillis();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    } else {
                        /*
                        if (onBlock)
                        {
                            state = State.TRIAL;
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
                                dwellTimer.schedule(confirmTask, 500);
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
                        */
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

        mMaxAngleX.setVisibility(View.INVISIBLE);
        mMaxAngleY.setVisibility(View.INVISIBLE);
        mMenuNum.setVisibility(View.INVISIBLE);


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
                    btnCondition.setText("SIT");
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
        btnMaxXPlus.setVisibility(View.INVISIBLE);


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
        btnMaxXMinus.setVisibility(View.INVISIBLE);

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
        btnMaxYPlus.setVisibility(View.INVISIBLE);

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
        btnMaxYMinus.setVisibility(View.INVISIBLE);

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
        btnMenuNumPlus.setVisibility(View.INVISIBLE);

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
        btnMenuNumMinus.setVisibility(View.INVISIBLE);

        logIDInput = (EditText)findViewById(R.id.editText);
        //logIDInput.setVisibility(View.INVISIBLE);
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

    //for Block menu
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
        if (fittsTarget.x - fittsTarget.y / 2  <= x && x <= fittsTarget.x + fittsTarget.y / 2)
        {
            return true;
        } else {
            return false;
        }
    }

    private boolean checkOnTarget2(int y)
    {
        if (fittsTarget2.x - fittsTarget2.y / 2  <= y && y <= fittsTarget2.x + fittsTarget2.y / 2)
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
