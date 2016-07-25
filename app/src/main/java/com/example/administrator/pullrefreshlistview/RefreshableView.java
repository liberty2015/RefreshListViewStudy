package com.example.administrator.pullrefreshlistview;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;

import java.util.IllegalFormatCodePointException;

/**
 * Created by Administrator on 2016/7/7.
 */
public class RefreshableView extends ListView implements View.OnTouchListener{
    private Context context;
    private int headerHeight;
    private float downY,distanceY;
    private int headerLayoutId;
    private View header;
    private float touchSlop;
    private RotateAnimation upRotate,downRotate;
    private boolean angleState=true;//true 为向下状态 false为向上状态
    /**
     * 下拉状态
     */
    private int STATUS_PULL_REFRESH=0;
    /**
     * 释放刷新状态
     */
    private int STATUS_RELEASE_REFRESH=1;
    /**
     * 正在刷新状态
     */
    private int STATUS_REFRESHING=2;
    /**
     * 刷新完成或未刷新状态
     */
    private int STATUS_REFRESH_FINISHED=3;

    private int currentState=STATUS_REFRESH_FINISHED;

    private boolean ableToPull;

    private ProgressBar progressBar;

    private ImageView arrow;

    private RefreshListener refreshListener;

    private int lastStatus=currentState;

    private boolean isDefaultHeader;

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        setIsAbleToPull(motionEvent);
        if (ableToPull){
            switch (motionEvent.getAction()){
                case MotionEvent.ACTION_DOWN:
                    /**
                     * 获取第一次触摸屏幕时的y坐标
                     * getRawY()获取到的是相对于屏幕左上角的Y坐标，而getY()则是相对于当前View自身左上角的Y坐标
                     */
                    downY=motionEvent.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    float distanceY=motionEvent.getRawY()-downY;
                    /**
                     *当滑动状态属于往下滑动的状态，，并且下拉头完全隐藏时屏蔽下拉事件
                     */
                    if (distanceY<=0&&getY()<=headerHeight){
                        return false;
                    }
                    if (distanceY<touchSlop){
                        return false;
                    }
                    if (currentState!=STATUS_REFRESHING){
                        /**
                         * 判断下拉头是否完全出现
                         */
                        if (getY()>=0){
                            currentState=STATUS_RELEASE_REFRESH;
                        }else {
                            currentState=STATUS_PULL_REFRESH;
                        }
                        setY(distanceY/2+headerHeight);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                default:
                    if (currentState==STATUS_PULL_REFRESH){
                        //若松手时是下拉状态，则调用隐藏下拉头动画函数
                        hideHeaderAnimation();
                    }else if (currentState==STATUS_RELEASE_REFRESH){
                        //若松手时是释放刷新状态，则调用刷新头函数
                        refreshHeader();
                    }
                    break;
            }
            //时刻更新下拉头信息
            if (currentState==STATUS_PULL_REFRESH||currentState==STATUS_RELEASE_REFRESH){
                updateHeaderView();
                //当正处于下拉或释放状态时，为防止被点击的那一项一直处于选中状态，要让listview失去焦点
                setPressed(false);
                setFocusable(false);
                setFocusableInTouchMode(false);
                lastStatus=currentState;
                //当正处于下拉或释放状态时，通过返回true屏蔽掉listview的OnTouch滚动触摸事件
                return true;
            }
        }
        return false;
    }

    /**
     * 当刷新逻辑完成后，将当前状态修改为完成状态
     */
    public void finishRefresh(){
        currentState=STATUS_REFRESH_FINISHED;
        hideHeaderAnimation();
    }

    /**
     *更新下拉头信息
     */
    private void updateHeaderView(){
        if (lastStatus!=currentState){
            if (currentState==STATUS_PULL_REFRESH){
                arrow.setVisibility(VISIBLE);
                progressBar.setVisibility(INVISIBLE);
                rotateArrow();
            }else if (currentState==STATUS_RELEASE_REFRESH){
                arrow.setVisibility(VISIBLE);
                progressBar.setProgress(INVISIBLE);
                rotateArrow();
            }else if (currentState==STATUS_REFRESHING){
                arrow.startAnimation(downRotate);
                angleState=true;
                arrow.clearAnimation();
                arrow.setVisibility(INVISIBLE);
                progressBar.setVisibility(VISIBLE);
            }
        }
    }

    /**
     *
     */
    public interface RefreshListener{
        public void onRefresh();
    }

    public RefreshableView(Context context) {
        this(context, null);
    }

    public RefreshableView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray typedArray=context.obtainStyledAttributes(attrs,R.styleable.RefreshableView);
        headerLayoutId=typedArray.getResourceId(R.styleable.RefreshableView_top_header, R.layout.header_layout);
        header= LayoutInflater.from(context).inflate(headerLayoutId, this, false);
        addHeaderView(header);
        touchSlop= ViewConfiguration.get(context).getScaledTouchSlop();
        setOnTouchListener(this);
        if (headerLayoutId==R.layout.header_layout){
            initView();
            isDefaultHeader=true;
        }
    }

    private void initView(){
        progressBar= (ProgressBar) header.findViewById(R.id.progress);
        arrow= (ImageView) header.findViewById(R.id.arrow);
    }

    public void setRefreshListener(RefreshListener refreshListener) {
        this.refreshListener = refreshListener;
    }

    /**
     * 判断当前是否允许下拉刷新
     * 即判断当前是否在listview的第一个列表项或者判断当前列表是否含有内容
     * @param ev
     */
    private void setIsAbleToPull(MotionEvent ev){
        View firstChild=getChildAt(0);
        if (firstChild!=null){
            if (getFirstVisiblePosition()==0&&firstChild.getTop()==0){
                if (!ableToPull){
                    downY=ev.getRawY();
                }
                ableToPull=true;
            }else {
                if (getY()!=headerHeight){
                    setY(headerHeight);
                }
                ableToPull=false;
            }
        }else {
            ableToPull=true;
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        headerHeight=-header.getHeight();
        setY(headerHeight);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int size=MeasureSpec.getSize(heightMeasureSpec)-headerHeight;
        int mode=MeasureSpec.getMode(heightMeasureSpec);
        int expandHeight=MeasureSpec.makeMeasureSpec(size,mode);
        super.onMeasure(widthMeasureSpec, expandHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }


    @Override
    public void computeScroll() {
        super.computeScroll();
    }

    /**
     * 旋转箭头
     */
    private void rotateArrow(){
        float pivotX=arrow.getWidth()/2f;
        float pivotY=arrow.getHeight()/2f;
        float fromDegree=0f,toDegree=0f;
        if (upRotate==null){
            upRotate=new RotateAnimation(0,180,pivotX,pivotY);
            upRotate.setDuration(300);
            upRotate.setFillAfter(true);
        }
        if (downRotate==null){
            downRotate=new RotateAnimation(180,360,pivotX,pivotY);
            downRotate.setDuration(300);
            downRotate.setFillAfter(true);
        }
        if (currentState==STATUS_PULL_REFRESH&&!angleState){
//            fromDegree=180;
//            toDegree=360;
            arrow.startAnimation(downRotate);
            angleState=true;
        }else if (currentState==STATUS_RELEASE_REFRESH&&angleState){
//            fromDegree=0;
//            toDegree=180;
            arrow.startAnimation(upRotate);
            angleState=false;
        }
//        RotateAnimation rotateAnimation=new RotateAnimation(fromDegree,toDegree,pivotX,pivotY);
//        rotateAnimation.setDuration(300);
//        rotateAnimation.setFillAfter(true);
//        arrow.startAnimation(rotateAnimation);
    }

    /**
     * 隐藏头
     */
    private void hideHeaderAnimation(){
        ObjectAnimator animator=ObjectAnimator.ofFloat(this,"y",getY(),headerHeight);
        animator.setDuration(100);
        animator.start();
        currentState=STATUS_REFRESH_FINISHED;
    }

    /**
     * 更新头
     */
    private void refreshHeader(){
        /**
         * 若列表当前y坐标不为0，则使用属性动画移动到0
         */
        if (getY()>0){
            ObjectAnimator animator=ObjectAnimator.ofFloat(this,"y",getY(),0);
            animator.setDuration(100);
            animator.start();
        }
        //将当前状态更新为正在刷新
        currentState=STATUS_REFRESHING;
        updateHeaderView();
        refreshListView();
//        /**
//         * 模拟网络延时操作
//         */
//        new Thread(){
//            @Override
//            public void run() {
//                try {
//                    sleep(5*1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                handler.sendEmptyMessage(0x123);
//            }
//        }.start();
    }

    private void refreshListView(){
        if (refreshListener!=null){
            refreshListener.onRefresh();
        }
//        finishRefresh();
    }
}
