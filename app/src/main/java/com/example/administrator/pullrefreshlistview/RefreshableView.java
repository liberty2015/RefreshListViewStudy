package com.example.administrator.pullrefreshlistview;

import android.animation.IntEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.IllegalFormatCodePointException;

/**
 * Created by Administrator on 2016/7/7.
 */
public class RefreshableView extends ListView implements View.OnTouchListener,AbsListView.OnScrollListener{
    private Context context;
    private int headerHeight;
    private FrameLayout content_container;
    private float downY,distanceY;
    private int headerLayoutId;
    private View header;
    private View footer;
    private float touchSlop;
    private RotateAnimation upRotate,downRotate;
    private RelativeLayout headerContainer;
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

    private TextView title;

    private pullDownRefreshListener pullDownRefreshListener;

    private pullUpRefreshListener pullUpRefreshListener;

    private int lastStatus=currentState;

    private boolean itemCanClick=true;

    private boolean isDefaultHeader;

    public void setPullDownRefreshListener(RefreshableView.pullDownRefreshListener pullDownRefreshListener) {
        this.pullDownRefreshListener = pullDownRefreshListener;
    }

    public void setPullUpRefreshListener(RefreshableView.pullUpRefreshListener pullUpRefreshListener) {
        this.pullUpRefreshListener = pullUpRefreshListener;
    }

    @Override
    public void setOnItemClickListener(OnItemClickListener listener) {
        super.setOnItemClickListener(listener);
    }

    /**
     * 下拉刷新时屏蔽item单击事件
     * @param view
     * @param position
     * @param id
     * @return
     */
    @Override
    public boolean performItemClick(View view, int position, long id) {
        if (itemCanClick){
            return super.performItemClick(view, position, id);
        }else {
            return false;
        }
    }

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
                    if (distanceY<=0&&getHeaderHeight()<=headerHeight){
                        return false;
                    }
                    if (distanceY<touchSlop){
                        return false;
                    }
                    if (currentState!=STATUS_REFRESHING){
                        /**
                         * 判断下拉头是否完全出现
                         */
                        if (getHeaderHeight()>=headerHeight){
                            currentState=STATUS_RELEASE_REFRESH;
                        }else {
                            currentState=STATUS_PULL_REFRESH;
                        }
                        setHeaderHeight((int)(distanceY/2));
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
                setItemsCanFocus(false);
                itemCanClick=false;
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
        title.setText("下拉刷新");
        hideHeaderAnimation();
    }

    public void finishRefresh(BaseAdapter adapter){
        if (adapter!=null){
            adapter.notifyDataSetChanged();
        }
        currentState=STATUS_REFRESH_FINISHED;
        title.setText("下拉刷新");
        itemCanClick=true;
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
                title.setText("正在刷新");
                arrow.clearAnimation();
                arrow.setVisibility(INVISIBLE);
                progressBar.setVisibility(VISIBLE);
            }
        }
    }


    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (scrollState==OnScrollListener.SCROLL_STATE_IDLE){
            if (view.getLastVisiblePosition()==view.getCount()-1&&pullUpRefreshListener!=null){
                pullUpRefreshListener.onRefresh();

            }
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

    }

    /**
     *下拉刷新监听器
     */
    public interface pullDownRefreshListener{
        public void onRefresh();
    }

    /**
     * 上拉刷新监听器
     */
    public interface pullUpRefreshListener{
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
        footer=LayoutInflater.from(context).inflate(R.layout.footer_layout, this, false);
        content_container= (FrameLayout) header.findViewById(R.id.content_container);
        RelativeLayout.LayoutParams params=new RelativeLayout.LayoutParams (ViewGroup.LayoutParams.MATCH_PARENT,0);
        headerContainer=new RelativeLayout(context);
        headerContainer.addView(header,params);
        addHeaderView(headerContainer);
        addFooterView(footer);
        touchSlop= ViewConfiguration.get(context).getScaledTouchSlop();
        setOnTouchListener(this);
        if (headerLayoutId==R.layout.header_layout){
            initView();
            isDefaultHeader=true;
        }
        headerContainer.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        headerHeight=content_container.getHeight();
                        Log.d("xxxxxxx","headerHeight="+headerHeight);
                        getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                }
        );
        setOnScrollListener(this);
    }


    private int getHeaderHeight(){
        return header.getLayoutParams().height;
    }

    private void setHeaderHeight(int height){
        RelativeLayout.LayoutParams params= (RelativeLayout.LayoutParams) header.getLayoutParams();
        params.height=height;
        header.setLayoutParams(params);
    }

    private void initView(){
        progressBar= (ProgressBar) header.findViewById(R.id.progress);
        arrow= (ImageView) header.findViewById(R.id.arrow);
        title= (TextView) header.findViewById(R.id.title);
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
                if (getHeaderHeight()!=0){
                    setHeaderHeight(0);
                }
                ableToPull=false;
            }
        }else {
            ableToPull=true;
        }
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
            arrow.startAnimation(downRotate);
            angleState=true;
        }else if (currentState==STATUS_RELEASE_REFRESH&&angleState){
            arrow.startAnimation(upRotate);
            angleState=false;
        }
    }

    /**
     * 隐藏头
     */
    private void hideHeaderAnimation(){
        performAnimate(headerContainer,getHeaderHeight(),0);
        currentState=STATUS_REFRESH_FINISHED;
    }


    private void performAnimate(final View target, final int start, final int end){
        ValueAnimator valueAnimator=ValueAnimator.ofInt(start,end);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            private IntEvaluator mEvaluator=new IntEvaluator();
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int currentValue= (int) animation.getAnimatedValue();
                Log.d("xxxxx","current value:"+currentValue);
                float fraction=animation.getAnimatedFraction();
                setHeaderHeight(mEvaluator.evaluate(fraction,start,end));
                target.requestLayout();
            }
        });
        valueAnimator.setDuration(100).start();
    }
    /**
     * 更新头
     */
    private void refreshHeader(){
        /**
         * 若列表当前y坐标不为0，则使用属性动画移动到0
         */
        if (getHeaderHeight()>0){
            performAnimate(header,getHeaderHeight(),headerHeight);
        }
        //将当前状态更新为正在刷新
        currentState=STATUS_REFRESHING;
        updateHeaderView();
        refreshListView();
    }

    private void refreshListView(){
        if (pullDownRefreshListener!=null){
            pullDownRefreshListener.onRefresh();
        }
    }
}
