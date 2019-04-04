package com.aramshim.headtilttest;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.util.Queue;

public class GestureClassifier {
    private final String TAG = GestureClassifier.class.getSimpleName();
    private int binSize = 100;
    private long[] time = new long[binSize];
    private double[] roll = new double[binSize];
    private double[] yaw = new double[binSize];
    private int idxStart = 0, idxEnd = 0;
    private double widthThres = 5, heightThres = 10;
    private long taskStartTime;

    GestureClassifier(Context context) throws IOException{
        //taskStartTime = System.currentTimeMillis();
    }

    public boolean updateData(double newRoll, double newYaw, boolean targetChanged, long timeSend){
        long currTime = timeSend;
        time[idxEnd] = currTime;
        roll[idxEnd] = newRoll;
        yaw[idxEnd] = newYaw;
        if(targetChanged)
            idxStart = idxEnd;
        if(++idxEnd >= binSize)
            idxEnd = 0;

        while(time[idxStart] < currTime - 500){
            if(++idxStart >= binSize)
                idxStart = 0;
        }

        return detect();
    }

    private boolean detect(){
        int minYawIdx = getMinYaw(idxStart, idxEnd);
        int maxYawIdxPrev = getMaxYaw(idxStart, minYawIdx);
        int maxYawIdxAfter = getMaxYaw(minYawIdx, idxEnd);
        //int minRollIdx = getMinRoll(idxStart, idxEnd);
        //int maxRollIdx = getMaxRoll(idxStart, idxEnd);

        double heightPrev = yaw[maxYawIdxPrev] - yaw[minYawIdx];
        double heightAfter = yaw[maxYawIdxAfter] - yaw[minYawIdx];
        //double width = roll[maxRollIdx] - roll[minRollIdx];

        Log.d(TAG, "detect: " + String.valueOf(time[idxStart]) + ", " + String.valueOf(idxStart) + ", " + String.valueOf(idxEnd) + ", " + String.valueOf(maxYawIdxPrev) + ", " + String.valueOf(minYawIdx) + ", " + String.valueOf(maxYawIdxAfter));
        Log.d(TAG, "detect: " + String.valueOf(heightPrev) + ", " + String.valueOf(heightAfter));

        if(heightPrev > heightThres && heightAfter > heightThres) {// && width < widthThres)
            Log.d(TAG, "detect: DETECT!!");
            return true;
        }
        else
            return false;
    }

    private int getMinYaw(int iStart, int iEnd){
        int idxTmp = iStart;
        double minYaw = yaw[idxTmp];
        int minYawIdx = idxTmp;
        while(idxTmp != iEnd)
        {
            if(minYaw > yaw[idxTmp]) {
                minYaw = yaw[idxTmp];
                minYawIdx = idxTmp;
            }
            if(++idxTmp >= binSize)
                idxTmp = 0;
        }
        return minYawIdx;
    }

    private int getMaxYaw(int iStart, int iEnd){
        int idxTmp = iStart;
        double maxYaw = yaw[idxTmp];
        int maxYawIdx = idxTmp;
        while(idxTmp != iEnd)
        {
            if(maxYaw < yaw[idxTmp]) {
                maxYaw = yaw[idxTmp];
                maxYawIdx = idxTmp;
            }
            if(++idxTmp >= binSize)
                idxTmp = 0;
        }
        return maxYawIdx;
    }

    private int getMinRoll(int iStart, int iEnd){
        int idxTmp = iStart;
        double minRoll = roll[idxTmp];
        int minRollIdx = idxTmp;
        while(idxTmp != iEnd)
        {
            if(minRoll > yaw[idxTmp]) {
                minRoll = yaw[idxTmp];
                minRollIdx = idxTmp;
            }
            if(++idxTmp >= binSize)
                idxTmp = 0;
        }
        return minRollIdx;
    }

    private int getMaxRoll(int iStart, int iEnd){
        int idxTmp = iStart;
        double maxRoll = yaw[idxTmp];
        int maxRollIdx = idxTmp;
        while(idxTmp != iEnd)
        {
            if(maxRoll < yaw[idxTmp]) {
                maxRoll = yaw[idxTmp];
                maxRollIdx = idxTmp;
            }
            if(++idxTmp >= binSize)
                idxTmp = 0;
        }
        return maxRollIdx;
    }
}
