package com.aramshim.headtilttest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

public class ImageView2 extends View {

    private Bitmap image; // 이미지
    private Rect boundRect;
    private Paint boundPaint;
    private Rect cursorRect;
    private Paint cursorPaint;

    private Rect centerRect;
    private Paint centerPaint;

    private Rect[][] menuRect;
    private Paint menuPaint;
    private Paint selectedMenuPaint, selectedMenuPaint2;
    private Paint confirmedMenuPaint;
    private Paint targetMenuPaint;

    private Rect fittsTargetRect;
    private Paint fittsTargetPaint;

    private Rect fittsTarget2Rect;
    private Rect verticalBoundRect;

    private Rect fittsCursorRect;
    private Paint fittsCursorPaint;

    private Point centerPoint;
    private int menuWidth;
    private int menuHeight;
    private int menuNum;

    private double angleX;
    private double angleY = 0f;
    private int maxAngleX;
    private int maxAngleY;

    private int selectedX;
    private int selectedY;
    private boolean onTrial = false;

    private int target;
    private boolean fittsMode = true;
    private int fittsTargetDistance;
    private int fittsTargetLength;
    private int fittsTarget2Distance;
    private int fittsTarget2Length;
    private boolean isOnTarget = false;
    private int step = 1;
    private boolean twoStepMode = true;

    private int step2Height = 720;
    private boolean isFail = false;

    public ImageView2(Context paramContext, AttributeSet paramAttributeSet) {
        super(paramContext, paramAttributeSet);
        boundRect = new Rect(40,200,1240,300);

        boundPaint = new Paint();
        boundPaint.setStrokeWidth(2);
        boundPaint.setStyle(Paint.Style.STROKE);
        boundPaint.setColor(Color.GREEN);

        cursorRect = new Rect(630,180,650,320);

        cursorPaint = new Paint();
        cursorPaint.setStrokeWidth(2);
        cursorPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        cursorPaint.setColor(Color.RED);

        fittsCursorPaint = new Paint();
        fittsCursorPaint.setStrokeWidth(2);
        fittsCursorPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        fittsCursorPaint.setColor(Color.YELLOW);

        centerRect = new Rect(638,160,642,340);
        centerPaint = new Paint();
        centerPaint.setStrokeWidth(2);
        centerPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        centerPaint.setColor(Color.YELLOW);

        menuRect = new Rect[20][20];

        menuPaint = new Paint();
        menuPaint.setStrokeWidth(2);
        menuPaint.setStyle(Paint.Style.STROKE);
        menuPaint.setColor(Color.YELLOW);

        selectedMenuPaint = new Paint();
        selectedMenuPaint.setStrokeWidth(2);
        selectedMenuPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        selectedMenuPaint.setColor(Color.BLUE);

        selectedMenuPaint2 = new Paint();
        selectedMenuPaint2.setStrokeWidth(2);
        selectedMenuPaint2.setStyle(Paint.Style.FILL_AND_STROKE);
        selectedMenuPaint2.setColor(Color.GREEN);

        confirmedMenuPaint = new Paint();
        confirmedMenuPaint.setStrokeWidth(2);
        confirmedMenuPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        confirmedMenuPaint.setColor(Color.RED);

        targetMenuPaint = new Paint();
        targetMenuPaint.setStrokeWidth(2);
        targetMenuPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        targetMenuPaint.setColor(Color.RED);

        fittsTargetPaint = new Paint();
        fittsTargetPaint.setStrokeWidth(2);
        fittsTargetPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        fittsTargetPaint.setColor(Color.BLUE);

        fittsTargetRect = new Rect(0,0,0,0);
        fittsTarget2Rect = new Rect(0,0,0,0);
        centerRect = new Rect(0,0,0,0);
    }

    public ImageView2(Context paramContext, AttributeSet paramAttributeSet,
                        int paramInt) {
        super(paramContext, paramAttributeSet, paramInt);
    }

    public ImageView2(Context context) {
        super(context);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!fittsMode) {
            for (int i = 0; i < menuNum; i++) {
                if (i == selectedX) {
                    if (i == target)
                        canvas.drawRect(menuRect[i][0], selectedMenuPaint2);
                    else
                        canvas.drawRect(menuRect[i][0], selectedMenuPaint);
                } else if (i == target) {
                    canvas.drawRect(menuRect[i][0], targetMenuPaint);
                } else {
                    canvas.drawRect(menuRect[i][0], menuPaint);
                }
            }
            canvas.drawCircle(centerPoint.x + (int)(angleX  / maxAngleX * (menuWidth / 2)), centerPoint.y  + (int)(angleY / maxAngleY * (menuHeight / 2)), 5, cursorPaint);
            invalidate();
        } else if (fittsMode) {
            for (int i = 0; i < menuNum; i++) {
               canvas.drawRect(menuRect[0][0], menuPaint);
            }

            if (twoStepMode)
            {
                if (onTrial)
                {
                    if (step == 1)
                    {
                        if (!isOnTarget)
                            canvas.drawRect(fittsTargetRect, fittsTargetPaint);
                        else
                            canvas.drawRect(fittsTargetRect, selectedMenuPaint2);
                        //canvas.drawRect(fittsTarget2Rect, menuPaint);
                        if (fittsTarget2Rect.top > centerPoint.y)
                            canvas.drawCircle(fittsTargetDistance, centerPoint.y + 100, 10, menuPaint);
                        else
                            canvas.drawCircle(fittsTargetDistance, centerPoint.y - 100, 10, menuPaint);
                    } // else
                        // canvas.drawRect(fittsTargetRect, selectedMenuPaint2);

                    if(step == 2) {
                        if (!isOnTarget)
                            canvas.drawRect(fittsTarget2Rect, fittsTargetPaint);
                        else
                            canvas.drawRect(fittsTarget2Rect, selectedMenuPaint2);
                        canvas.drawRect(verticalBoundRect, menuPaint);
                    }

                } else {
                    if (isFail) {
                        canvas.drawRect(fittsTargetRect, confirmedMenuPaint);
                        canvas.drawRect(fittsTarget2Rect, confirmedMenuPaint);
                    } else
                    {
                        canvas.drawRect(fittsTargetRect, selectedMenuPaint2);
                        canvas.drawRect(fittsTarget2Rect, selectedMenuPaint2);
                    }
                    if (verticalBoundRect != null)
                        canvas.drawRect(verticalBoundRect, menuPaint);
                }
            } else {
                if (onTrial)
                {
                    if (!isOnTarget)
                        canvas.drawRect(fittsTargetRect, fittsTargetPaint);
                    else
                        canvas.drawRect(fittsTargetRect, selectedMenuPaint2);
                } else {
                    canvas.drawRect(fittsTargetRect, confirmedMenuPaint);
                }
            }

            if (step == 1)
            {
                fittsCursorRect = new Rect(centerPoint.x + (int)(angleX  / maxAngleX * (menuWidth / 2)) - 1, centerPoint.y - menuHeight / 2,
                        centerPoint.x + (int)(angleX  / maxAngleX * (menuWidth / 2)) + 1, centerPoint.y + menuHeight / 2);
            } else if (step == 2)
            {
                fittsCursorRect = new Rect(fittsTargetDistance - fittsTargetLength / 2, centerPoint.y + (int)(angleY  / maxAngleY * (step2Height / 2)) - 1,
                        fittsTargetDistance + fittsTargetLength / 2, centerPoint.y + (int)(angleY  / maxAngleY * (step2Height / 2)) + 1);
            }
            //canvas.drawRect(fittsCursorRect,fittsCursorPaint);
            canvas.drawCircle(centerPoint.x + (int)(angleX  / maxAngleX * (menuWidth / 2)), centerPoint.y + (int)(angleY  / maxAngleY * (step2Height / 2)), 5, cursorPaint);
            invalidate();
        }

        super.onDraw(canvas);
    }

    public void setMenuNum(int num) {
        menuNum = num;
        for (int i = 0; i < menuNum; i++) {
            menuRect[i][0] = new Rect(centerPoint.x - menuWidth / 2 + menuWidth / menuNum  * i, centerPoint.y - menuHeight / 2,
                    centerPoint.x  - menuWidth / 2 + menuWidth / menuNum  * (i  + 1), centerPoint.y + menuHeight / 2);
        }
    }

    public void setMenuSize(int menuWidth, int menuHeight) {
        this.menuWidth = menuWidth;
        this.menuHeight = menuHeight;
    }

    public void setCenterPoint(Point centerPoint) {
        this.centerPoint  = centerPoint;
    }

    public void setAngleX(double angle) {
        angleX = angle;
    }

    public void setAngleY(double angle) {
        angleY = angle;
    }

    public void setMaxAngleX(int angle) {
        maxAngleX = angle;
    }

    public void setMaxAngleY(int angle) {
        maxAngleY = angle;
    }

    public void setSelectedPosition(int x, int y) {
        selectedX = x;
        selectedY = y;
    }

    public void setOnTrial(boolean mode){
        onTrial = mode;
    }

    public void setTarget(int target) {
        this.target  = target;
    }

    public void setIsonTarget(boolean x) {
        isOnTarget = x;
    }

    public void setFittsTarget(Point target) {
        fittsTargetDistance  = target.x;
        fittsTargetLength = target.y;
        fittsTargetRect = new Rect(fittsTargetDistance - fittsTargetLength / 2, centerPoint.y - menuHeight / 2,
                fittsTargetDistance + fittsTargetLength / 2, centerPoint.y + menuHeight / 2);

        verticalBoundRect = new Rect(fittsTargetDistance - fittsTargetLength / 2, centerPoint.y - step2Height / 2,
                fittsTargetDistance + fittsTargetLength / 2, centerPoint.y + step2Height / 2);
    }

    public void setFittsTarget2(Point target) {
        fittsTarget2Distance  = target.x;
        fittsTarget2Length = target.y;
        fittsTarget2Rect = new Rect(fittsTargetDistance - fittsTargetLength / 2, fittsTarget2Distance - fittsTarget2Length / 2,
                fittsTargetDistance + fittsTargetLength / 2, fittsTarget2Distance + fittsTarget2Length / 2);
    }

    public void setStep(int s) {
        step = s;
    }

    public void setIsFail(boolean fail) {
        isFail = fail;
    }
}