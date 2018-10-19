package com.sun.speedtestexample;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import static android.graphics.Paint.Style.FILL;
import static android.graphics.Paint.Style.STROKE;

public class SpeedDisplayView extends View{
    private Paint paint,textPaint,textPaint1;
    private float arcWidth;    //外层弧的宽度
    private float arcWidth2;    //内层弧的宽度
    private float circleWidth;  //内层圆宽度
    private float outsideRadius;  //外圆半径大小
    private float insideRadius;  //内圆半径大小
    private float circleRadius;  //圆心半径
    private float pointerCenterRadius;  //指针圆心半径
    private float indicateRadius;  //指示点半径
    private float pointerRadius;  //指针的半径
    private float scaleLength;  //刻度长度
    private float scaleWidth;   //刻度宽度
    private RectF oval;  //用于定义的圆弧的形状和大小的界限
    private Path path;  //文本显示的路径
    private float Llength;  //弧长
    private String[] speedValues = {"0MB","1MB","5MB","10MB","20MB","30MB","40MB","60MB","80MB"};
    private Path pointerPath;
    private float currentspeed;  //当前的速度
    private float currentdegree;  //当前扫过的角度
    private float newspeed=0.00f;  //最新的速度
    private float newdegree;  //最新扫过的角度
    private float sweepDegree;  //扫过的角度值
    private int animatorType = 1;  //1：增长，0：减小
    private ValueAnimator animator;

    public SpeedDisplayView(Context context) {
        this(context,null);
    }

    public SpeedDisplayView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public SpeedDisplayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        //获取xml属性
        arcWidth = DimenUtil.dp2px(getContext(), 2.0f);
        arcWidth2 = DimenUtil.dp2px(getContext(), 3.0f);
        circleWidth = DimenUtil.dp2px(getContext(), 1.0f);
        outsideRadius = DimenUtil.dp2px(getContext(), 120.0f);
        insideRadius = DimenUtil.dp2px(getContext(), 95.0f);
        circleRadius = DimenUtil.dp2px(getContext(), 5.0f);
        pointerCenterRadius = DimenUtil.dp2px(getContext(), 3.0f);
        indicateRadius = DimenUtil.dp2px(getContext(), 3.0f);
        pointerRadius = DimenUtil.dp2px(getContext(), 90.0f);
        Llength = (30 * 3.14f * insideRadius)/180;
        scaleLength = DimenUtil.dp2px(getContext(),10.0f);
        scaleWidth = DimenUtil.dp2px(getContext(),1.5f);
        paint = new Paint();
        paint.setAntiAlias(true);  //消除锯齿
        textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
        textPaint.setTextSize(20);
        textPaint1 = new Paint();
        textPaint1.setAntiAlias(true);
        textPaint1.setTextAlign(Paint.Align.CENTER);

        oval = new RectF();
        path = new Path();
        pointerPath = new Path();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width;
        int height;
        int size = MeasureSpec.getSize(widthMeasureSpec);
        int mode = MeasureSpec.getMode(widthMeasureSpec);
        if (mode == MeasureSpec.EXACTLY) {
            width = size;
        } else {
            width = (int) ((2 * outsideRadius) + arcWidth);
        }
        size = MeasureSpec.getSize(heightMeasureSpec);
        mode = MeasureSpec.getMode(heightMeasureSpec);
        if (mode == MeasureSpec.EXACTLY) {
            height = size;
        } else {
            height = (int) ((2 * outsideRadius) + arcWidth);
        }
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //画圆弧背景
        drawArc(canvas);
        //绘制刻度
        drawScale(canvas);
        //绘制速度标识文字
        drawSpeedText(canvas);
        //绘制中间文字内容
        drawCenter(canvas);
        //绘制指针
        drawPointer(canvas);
    }

    private void drawArc(Canvas canvas) {
        int circlePoint = getWidth() / 2;
        //画外层圆
        paint.setColor(ContextCompat.getColor(getContext(), R.color.gray_DADADD));  //设置弧的颜色
        paint.setStyle(STROKE); //设置空心
        paint.setStrokeWidth(arcWidth); //设置弧的宽度
        oval.left = oval.top = circlePoint - outsideRadius;
        oval.right = oval.bottom = circlePoint + outsideRadius;
        canvas.drawArc(oval, 150, 240, false, paint);  //根据进度画圆
        //画内层圆背景
        paint.setStrokeWidth(arcWidth2); //设置弧的宽度
        oval.left = oval.top = circlePoint - insideRadius;
        oval.right = oval.bottom = circlePoint + insideRadius;
        canvas.drawArc(oval, 150, 240, false, paint);  //根据进度画圆弧
        //绘制进度
        paint.setColor(ContextCompat.getColor(getContext(), R.color.red));
        if(animatorType == 1){
            canvas.drawArc(oval,150,currentdegree,false,paint);
            canvas.drawArc(oval,150+currentdegree,sweepDegree,false,paint);
        }else{
            canvas.drawArc(oval,150,currentdegree-sweepDegree,false,paint);
            paint.setColor(ContextCompat.getColor(getContext(), R.color.gray_DADADD));
            canvas.drawArc(oval,150+currentdegree,-sweepDegree,false,paint);
        }
        path.addArc(oval,120,300);
    }

    private void drawScale(Canvas canvas) {
        int circlePoint = getWidth() / 2;
        paint.setStrokeWidth(scaleWidth);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
        for (int i = 0; i < 9; i++) {
            canvas.drawLine(circlePoint - insideRadius - DimenUtil.dp2px(getContext(),1.5f), circlePoint, circlePoint - insideRadius - scaleLength, circlePoint, paint);
            if(i == 7){
                canvas.rotate(120, circlePoint, circlePoint);
            }else{
                canvas.rotate(30, circlePoint, circlePoint);
            }
        }
    }

    private void drawSpeedText(Canvas canvas) {
        for (int i = 0;i < 9;i++){
            float width = textPaint.measureText(speedValues[i])/2;
            canvas.drawTextOnPath(speedValues[i],path,(i+1)*Llength - width,-20,textPaint);
        }
    }

    private void drawPointer(Canvas canvas) {
        canvas.save();
        float degree = animatorType == 1?(currentdegree+sweepDegree):(currentdegree-sweepDegree);
        int circlePoint = getWidth() / 2;
        canvas.rotate(degree,circlePoint,circlePoint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.gray_DADADD));  //设置弧的颜色
        paint.setStrokeWidth(circleWidth);
        canvas.drawCircle(circlePoint,circlePoint,circleRadius,paint);
        paint.setColor(ContextCompat.getColor(getContext(),R.color.colorAccent));
        paint.setStyle(FILL);
        canvas.drawCircle(circlePoint-(float)Math.cos(Math.toRadians(30))*insideRadius,circlePoint+(float)Math.sin(Math.toRadians(30))*insideRadius,indicateRadius,paint);
        canvas.drawCircle(circlePoint,circlePoint,pointerCenterRadius,paint);
        pointerPath.moveTo(circlePoint-(float)Math.cos(Math.toRadians(30))*pointerRadius,circlePoint+(float)Math.sin(Math.toRadians(30))*pointerRadius);
        pointerPath.lineTo(circlePoint-(float)Math.sin(Math.toRadians(30))*pointerCenterRadius,circlePoint-(float)Math.cos(Math.toRadians(30))*pointerCenterRadius);
        pointerPath.lineTo(circlePoint+(float)Math.sin(Math.toRadians(30))*pointerCenterRadius,circlePoint+(float)Math.cos(Math.toRadians(30))*pointerCenterRadius);
        pointerPath.close();
        canvas.drawPath(pointerPath,paint);
        canvas.restore();
    }

    private void drawCenter(Canvas canvas) {
        int circlePoint = getWidth() / 2;
        textPaint1.setColor(ContextCompat.getColor(getContext(), R.color.black_323232));
        textPaint1.setTextSize(20);
        /*canvas.drawLine(0,
                circlePoint+(float)Math.cos(Math.toRadians(60))*insideRadius,
                getWidth(),circlePoint+(float)Math.cos(Math.toRadians(60))*insideRadius,paint);*/
        canvas.drawText(getContext().getString(R.string.current_speed),circlePoint,circlePoint+(float)Math.cos(Math.toRadians(60))*insideRadius,textPaint1);
        textPaint1.setColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
        textPaint1.setTextSize(50);
        float y = circlePoint+(float)Math.cos(Math.toRadians(60))*insideRadius + (textPaint1.getFontMetrics().bottom-textPaint1.getFontMetrics().top);
        canvas.drawText(newspeed+"MB",circlePoint,y,textPaint1);
    }

    public void reset(){
        updateSpeedValue(0f);
    }

    //加锁保证线程安全,能在线程中使用
    public synchronized void setSpeedValue(float speed) {
        this.newspeed = speed;
        updateSpeedValue(speed);
    }

    private void updateSpeedValue(float speed) {
        animatorType = speed > currentspeed?1:0;
        //新的角度值和当前的角度值
        newdegree = getDegreeFormSpeed(speed);
        currentdegree = getDegreeFormSpeed(currentspeed);

        animator = ObjectAnimator.ofFloat(0, Math.abs(newdegree-currentdegree));
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                SpeedDisplayView.this.sweepDegree = (float) animation.getAnimatedValue();
                postInvalidate();
            }
        });
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {  //动画开始
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                currentspeed = newspeed;
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        animator.setDuration(500);
        animator.setInterpolator(new LinearInterpolator());
        animator.start();
    }

    private float getDegreeFormSpeed(float speed) {
        if(speed < 0){
            throw new IllegalArgumentException("speed should not be less than 0");
        }
        if(speed >= 0 && speed <= 1){
            return 30*speed;
        }else if(speed > 1 && speed <= 5){
            return 30+(30*(speed-1)/4);
        }else if(speed > 5 && speed <= 10){
            return 60+(30*(speed-5)/5);
        }else if(speed > 10 && speed <= 20){
            return 90+(30*(speed-10)/10);
        }else if(speed > 20 && speed <= 30){
            return 120+(30*(speed-20)/10);
        }else if(speed > 30 && speed <= 40){
            return 150+(30*(speed-30)/10);
        }else if(speed > 40 && speed <= 60){
            return 180+(30*(speed-40)/20);
        }else if(speed > 60 && speed <= 80){
            return 210+(30*(speed-60)/20);
        }else if(speed > 80){
            return 240;
        }
        return 0;
    }
}
