package com.aramshim.headtilttest;

import android.content.Context;
import android.os.Environment;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class DataLogger {
    private final String TAG = this.getClass().getSimpleName();
    private Context dlContext;
    private String fileID;
    private FileWriter fWriter = null;
    private long taskStartTime = 0;
    private boolean connectionOn = false;

    DataLogger(Context context) throws IOException {
        dlContext = context;
    }

    public void start(String logId) throws IOException {
        fileID = logId;
        if(isExternalStorageWritable()) {
            File saveFile = new File(Environment.getExternalStorageDirectory() + File.separator + fileID + ".csv");
            Log.d(TAG, "start: " + saveFile.getPath());
            fWriter = new FileWriter(saveFile, false);
            taskStartTime = System.currentTimeMillis();
            fWriter.write("Time,ID,Roll,Pitch,Yaw,DistX,DistY,DistZ,Battery\n");
            fWriter.flush();
            connectionOn = true;
            Toast.makeText(dlContext, "Log writer started", Toast.LENGTH_SHORT).show();
        }
        else{
            Log.d(TAG, "start: External storage not available");
            Toast.makeText(dlContext, "External storage not available", Toast.LENGTH_SHORT).show();
        }
    }

    public void close() throws IOException {
        if(isOpen()) {
            fWriter.flush();
            fWriter.close();
            connectionOn = false;
            Toast.makeText(dlContext, "Log writer closed", Toast.LENGTH_SHORT).show();
        }
    }

    public boolean isOpen(){
        return connectionOn;
    }

    public void write(String data) throws IOException {
        if(isOpen()) {
            long tmpTime = System.currentTimeMillis() - taskStartTime;
            String timeStr = Long.toString(tmpTime);
            fWriter.write(timeStr + "," + data);
            fWriter.flush();
        }
    }

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public long getCurrentTime(){
        long tmpTime = System.currentTimeMillis() - taskStartTime;
        return tmpTime;
    }
}
