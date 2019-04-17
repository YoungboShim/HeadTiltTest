package com.aramshim.headtilttest;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Point;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.media.AudioManager;
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

    double maxAngleX = 62;
    double maxAngleY = 60;
    double screenWidth = 1280;
    double screenHeight = 720;
    double[] centerPoint = {screenWidth / 2, screenHeight / 2};
    double menuWidth = screenWidth;
    double menuHeight = 150;
    int menuNum = 1;
    int selectedX = 0;
    int selectedY = 0;

    int step = 1;

    int target;
    double[] fittsTarget = new double[2];
    double[] fittsTarget2 = new double[2];

    int numBlock = 3;
    int numRepeat = 12;

    ArrayList<Integer> numTargets = new ArrayList<>(Arrays.asList(menuNum));
    ArrayList<Point> targets;

    int cntTrial = 0;
    int numTrial = 0;

    ArrayList<double[]> list_hTargets= new ArrayList<double[]>();
    ArrayList<double[]> list_vTargets= new ArrayList<double[]>();

    private DataLogger dLogger;
    private DataLogger dLogger_result;

    private Timer dwellTimer;
    private TimerTask confirmTask;

    private boolean dwellMode = false;
    private boolean seatMode = true;
    private boolean fittsMode = true;
    private boolean twoStepMode = true;
    private boolean orthMode = true;

    private GestureClassifier gestureClassifier;

    private SerialInputOutputManager mSerialIoManager;

    private State state = State.INIT;
    private boolean isOnTarget = false;

    ArrayList<Double> list_htargetDistance =  new ArrayList<Double>(Arrays.asList(0.0, 0.0, 28.0, -28.0, 56.0, -56.0));
    ArrayList<Double> list_htargetSize =  new ArrayList<Double>(Arrays.asList(12.0));
    ArrayList<Double> list_vtargetDistance =  new ArrayList<Double>(Arrays.asList(20.0, -20.0, 40.0, -40.0));
    ArrayList<Double> list_vtargetSize =  new ArrayList<Double>(Arrays.asList(4.0, 8.0, 12.0));

    private double vBoundHeight = 20;
    private double hBoundWidth = 12;

    private long startTime = 0;
    private long endTime;
    private int numCross;
    private boolean previousIsOnTarget;
    private int currentBlock = 0;
    private int comfortLevel = -1;
    private double endPoint;
    private int currentCondition = 0;
    private Timer mTimer;
    private double degOnEntry = 0;

    Point tempStep2Target = new Point(360 - 30 * 8, 15 * 8);

    static AssetManager assetManager;
    static final int CLIP_NONE = 0;
    static final int CLIP_HELLO = 1;
    static final int CLIP_ANDROID = 2;
    static final int CLIP_SAWTOOTH = 3;
    static final int CLIP_PLAYBACK = 4;

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
                if (endTime - startTime >= 15000)
                {
                    confirmTask.cancel();
                    mTextTarget.setText("Failed");
                    endTime = startTime + 15000;
                    try {
                        //dLogger.trace_write(strData);
                        if (fittsMode)
                            dLogger.just_write(currentBlock + "," + cntTrial + "," + (fittsTarget[0]-640)/8 + "," + fittsTarget[1]/8 + "," + (endTime - startTime) + ","+ strData);
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

                    if (twoStepMode) {
                        if (step == 1)
                        {
                            //isOnTarget = checkOnTarget(centerPoint.x + (int) (yaw * (1280 / maxAngleX / 2)));
                            isOnTarget = checkOnTargetMod(yaw, pitch, X2ang(fittsTarget[0] - screenWidth / 2.0), 0, X2ang(fittsTarget[1]), vBoundHeight);
                            if (isOnTarget == false && previousIsOnTarget == true)
                                numCross++;
                            imageview2.setIsonTarget(isOnTarget);
                            if (previousIsOnTarget == true && Math.abs(pitch) > vBoundHeight / 2)
                            {
                                degOnEntry = X2ang(fittsTarget[0] - screenWidth / 2);
                                isOnTarget = false;
                                step = 2;
                                imageview2.setStep(2);
                            }
                        } else if (step == 2) {
                            if(orthMode){
                                isOnTarget = checkOnTargetMod(yaw, pitch, X2ang(fittsTarget[0] - screenWidth / 2.0), Y2ang(fittsTarget2[0] - screenHeight / 2.0), hBoundWidth, Y2ang(fittsTarget2[1]));
                                //isOnTarget = checkOnTarget2(centerPoint.y + (int) (pitch * (720 / maxAngleY / 2)));
                                if (isOnTarget == false && previousIsOnTarget == true)
                                    numCross++;

                                imageview2.setIsonTarget(isOnTarget);
                                if (previousIsOnTarget == true && Math.abs(yaw - degOnEntry) > hBoundWidth / 2)
                                {
                                    trialDone();
                                }
                            }
                            else {
                                isOnTarget = checkOnTarget2((int)centerPoint[1] + (int) (pitch / maxAngleY * (720 / 2)));
                                imageview2.setIsonTarget(isOnTarget);
                                if (!isOnTarget) {
                                    confirmTask.cancel();
                                    createTimerTask();
                                    dwellTimer.schedule(confirmTask, 500);
                                }
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
                        dLogger.just_write(currentBlock + "," + cntTrial + "," + (fittsTarget[0]-640)/8 + "," + fittsTarget[1]/8 + "," + (endTime - startTime) + ","+ strData);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    public void trialDone() {
        state  = State.TRIAL_BREAK;
        degOnEntry = 0;
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
            int targetIdx;
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
                targetIdx = new Random().nextInt(list_hTargets.size());
                fittsTarget = list_hTargets.remove(targetIdx);
                fittsTarget2 = list_vTargets.remove(targetIdx);
                imageview2.setFittsTarget(new Point((int)fittsTarget[0], (int)fittsTarget[1]));
                imageview2.setFittsTarget2(new Point((int)fittsTarget2[0], (int)fittsTarget2[1]));
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

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            }
        }

        assetManager = getAssets();

        // initialize native audio system
        createEngine();

        int sampleRate = 0;
        int bufSize = 0;
        /*
         * retrieve fast audio path sample rate and buf size; if we have it, we pass to native
         * side to create a player with fast audio enabled [ fast audio == low latency audio ];
         * IF we do not have a fast audio path, we pass 0 for sampleRate, which will force native
         * side to pick up the 8Khz sample rate.
         */
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            AudioManager myAudioMgr = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            String nativeParam = myAudioMgr.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
            sampleRate = Integer.parseInt(nativeParam);
            nativeParam = myAudioMgr.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);
            bufSize = Integer.parseInt(nativeParam);
        }
        createBufferQueueAudioPlayer(sampleRate, bufSize);

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
                                    dLogger.start("exp2_" + logID + "_sit_trace");
                                    dLogger_result.start("exp2_" + logID+"_sit_result");
                                } else {
                                    dLogger.start("exp2_" + logID + "_walk_trace");
                                    dLogger_result.start("exp2_" + logID+"_walk_result");
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
                                    dLogger_result.just_write(currentBlock + "," + cntTrial + "," + (fittsTarget[0]-640)/8 + "," + fittsTarget[1]/8
                                            + "," + seatMode + "," + dwellMode + "," + (endTime - startTime)  + "," +  numCross  + "," + comfortLevel + "," + (endPoint - (fittsTarget[0]-640)/8)+"\n");}
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
                                    list_hTargets = new ArrayList<double[]>();
                                    for (double size : list_htargetSize) {
                                        for (double distance : list_htargetDistance) {
                                            for (int i = 0; i < list_vtargetSize.size() * list_vtargetDistance.size() ; i++) {
                                                list_hTargets.add(new double[]{640 + distance * (1280.0 / maxAngleX / 2), size * (1280.0 / maxAngleX / 2)});
                                                // list_hTargets.add(new Point(640 - distance * (1280 / maxAngleX / 2), size * (1280 / maxAngleX / 2)));
                                            }
                                        }
                                    }

                                    list_vTargets = new ArrayList<double[]>();
                                    for (int i = 0; i < list_htargetSize.size() * list_htargetDistance.size(); i++) {
                                        for (double size : list_vtargetSize) {
                                            for (double distance : list_vtargetDistance) {
                                                list_vTargets.add(new double[]{360 - distance * (720.0 / maxAngleY / 2), size * (720.0 / maxAngleY / 2)});
                                                // list_vTargets.add(new Point(360 + distance * (720 / maxAngleY / 2), size * (720 / maxAngleY / 2)));
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
        imageview2.setCenterPoint(new Point((int)centerPoint[0], (int) centerPoint[1]));
        menuHeight = (int)ang2Y(vBoundHeight);
        imageview2.setMenuSize((int)menuWidth, (int)menuHeight);
        imageview2.setMenuNum(menuNum);
        imageview2.setMaxAngleX((int)maxAngleX);
        imageview2.setMaxAngleY((int)maxAngleY);
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
                selectClip(CLIP_SAWTOOTH, 1);
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
                imageview2.setMaxAngleX((int)maxAngleX);
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
                imageview2.setMaxAngleX((int)maxAngleX);
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
                imageview2.setMaxAngleY((int)maxAngleY);
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
                imageview2.setMaxAngleY((int)maxAngleY);
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
            // TODO: play tick sound using native-audio
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
        if (fittsTarget[0] - fittsTarget[1] / 2  <= x && x <= fittsTarget[0] + fittsTarget[1] / 2)
        {
            return true;
        } else {
            return false;
        }
    }

    private boolean checkOnTargetMod(double cursorX, double cursorY, double targetX, double targetY, double targetWidth, double targetHeight){
        if(targetX - targetWidth / 2  <= cursorX && cursorX <= targetX + targetWidth / 2 && targetY - targetHeight / 2 <= cursorY && cursorY <= targetY + targetHeight / 2)
            return true;
        else
            return false;
    }

    private boolean checkOnTarget2(int y)
    {
        if (fittsTarget2[0] - fittsTarget2[1] / 2  <= y && y <= fittsTarget2[0] + fittsTarget2[1] / 2)
        {
            return true;
        } else {
            return false;
        }
    }

    private double ang2X(double angle){
        return angle * (screenWidth / maxAngleX / 2);
    }

    private double ang2Y(double angle){
        return angle * (screenHeight / maxAngleY/ 2);
    }

    private double X2ang(double xPos){
        return xPos / (screenWidth / maxAngleX / 2);
    }

    private double Y2ang(double yPos){
        return yPos / (screenHeight / maxAngleY/ 2);
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

    public static native void createEngine();
    public static native void createBufferQueueAudioPlayer(int sampleRate, int samplesPerBuf);
    public static native boolean createAssetAudioPlayer(AssetManager assetManager, String filename);
    // true == PLAYING, false == PAUSED
    public static native void setPlayingAssetAudioPlayer(boolean isPlaying);
    public static native boolean selectClip(int which, int count);
    public static native void shutdown();

    /** Load jni .so on initialization */
    static {
        System.loadLibrary("native-audio-jni");
    }
}
