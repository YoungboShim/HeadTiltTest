package com.aramshim.headtilttest;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.util.Queue;

public class GestureClassifier {
    private final String TAG = GestureClassifier.class.getSimpleName();
    private long[] time = new long[50];
    private double[] roll = new double[50];
    private double[] yaw = new double[50];
    private int idxStart = 0, idxEnd = 0;
    private double widthThres = 5, heightThres = 10;
    private long taskStartTime;

    GestureClassifier(Context context) throws IOException{
        taskStartTime = System.currentTimeMillis();
    }

    public boolean updateData(double newRoll, double newYaw){
        long currTime = System.currentTimeMillis() - taskStartTime;
        time[idxEnd] = currTime;
        roll[idxEnd] = newRoll;
        yaw[idxEnd] = newYaw;
        if(++idxEnd >= 50)
            idxEnd = 0;

        while(time[idxStart] < currTime - 1000){
            if(++idxStart >= 50)
                idxStart = 0;
        }

        return detect();
    }

    private boolean detect(){
        int minYawIdx = getMinYaw(idxStart, idxEnd);
        int maxYawIdxPrev = getMaxYaw(idxStart, minYawIdx);
        int maxYawIdxAfter = getMaxYaw(minYawIdx, idxEnd);
        int minRollIdx = getMinRoll(idxStart, idxEnd);
        int maxRollIdx = getMaxRoll(idxStart, idxEnd);

        double heightPrev = yaw[maxYawIdxPrev] - yaw[minYawIdx];
        double heightAfter = yaw[maxYawIdxAfter] - yaw[minYawIdx];
        double width = roll[maxRollIdx] - roll[minRollIdx];

        Log.d(TAG, "detect: " + String.valueOf(heightAfter) + ", " + String.valueOf(heightPrev) + ", " + String.valueOf(width));

        if(heightPrev > heightThres && heightAfter > heightThres && width < widthThres)
            return true;
        else
            return false;
    }

    private int getMinYaw(int iStart, int iEnd){
        int idxTmp = iStart;
        double minYaw = yaw[idxTmp];
        int minYawIdx = idxTmp;
        while(idxTmp != iEnd)
        {
            if(++idxTmp >= 50)
                idxTmp = 0;
            if(minYaw > yaw[idxTmp]) {
                minYaw = yaw[idxTmp];
                minYawIdx = idxTmp;
            }
        }
        return minYawIdx;
    }

    private int getMaxYaw(int iStart, int iEnd){
        int idxTmp = iStart;
        double maxYaw = yaw[idxTmp];
        int maxYawIdx = idxTmp;
        while(idxTmp != iEnd)
        {
            if(++idxTmp >= 50)
                idxTmp = 0;
            if(maxYaw < yaw[idxTmp]) {
                maxYaw = yaw[idxTmp];
                maxYawIdx = idxTmp;
            }
        }
        return maxYawIdx;
    }

    private int getMinRoll(int iStart, int iEnd){
        int idxTmp = iStart;
        double minRoll = roll[idxTmp];
        int minRollIdx = idxTmp;
        while(idxTmp != iEnd)
        {
            if(++idxTmp >= 50)
                idxTmp = 0;
            if(minRoll > yaw[idxTmp]) {
                minRoll = yaw[idxTmp];
                minRollIdx = idxTmp;
            }
        }
        return minRollIdx;
    }

    private int getMaxRoll(int iStart, int iEnd){
        int idxTmp = iStart;
        double maxRoll = yaw[idxTmp];
        int maxRollIdx = idxTmp;
        while(idxTmp != iEnd)
        {
            if(++idxTmp >= 50)
                idxTmp = 0;
            if(maxRoll < yaw[idxTmp]) {
                maxRoll = yaw[idxTmp];
                maxRollIdx = idxTmp;
            }
        }
        return maxRollIdx;
    }
}
