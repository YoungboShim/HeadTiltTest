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
import android.widget.Button;

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
    private Paint selectedMenuPaint;

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

        centerRect = new Rect(638,160,642,340);
        centerPaint = new Paint();
        centerPaint.setStrokeWidth(2);
        centerPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        centerPaint.setColor(Color.YELLOW);

        menuRect = new Rect[10][10];

        menuPaint = new Paint();
        menuPaint.setStrokeWidth(2);
        menuPaint.setStyle(Paint.Style.STROKE);
        menuPaint.setColor(Color.YELLOW);

        selectedMenuPaint = new Paint();
        selectedMenuPaint.setStrokeWidth(2);
        selectedMenuPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        selectedMenuPaint.setColor(Color.BLUE);

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
        for (int i = 0; i < menuNum; i++) {
            if (i == selectedX)
                canvas.drawRect(menuRect[i][0], selectedMenuPaint);
            else
                canvas.drawRect(menuRect[i][0], menuPaint);
        }
        //canvas.drawCircle(centerPoint.x + (int)(angleX  / maxAngleX * (menuWidth / 2)), centerPoint.y  + (int)(angleY / maxAngleY * (menuHeight / 2)), 5, cursorPaint);
        invalidate();
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
        /*
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
        */
    }

    public void setAngleY(double angle) {
        angleY = angle;
        /*
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
        */
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

}