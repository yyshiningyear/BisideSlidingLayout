package com.yy.besideslidinglayout;

import android.content.Context;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.RelativeLayout;

/**
 * Created by YY on 2015/6/1.
 */
//TODO 将左边菜单隐藏时滑到一半并不行，必须要滑动速度大于阈值才行
//TODO 右边菜单隐藏时无动画效果，而且主界面移动时总会先跑到最边上
public class BesideSlidingLayout extends RelativeLayout implements View.OnTouchListener {

    /**
     * 滚动显示和隐藏左侧布局时，手指滑动需要达到的速度。
     */
    public static final int SNAP_VELOCITY = 200;

    /**
     * 滑动状态的一种，表示未进行任何滑动
     */
    public static final int DO_NOTHING = 0;

    /**
     * 滑动状态的一种，表示正在滑出左侧菜单
     */
    public static final int SHOW_LEFT_MENU = 1;

    /**
     * 滑动状态的一种，表示正在滑出右侧菜单
     */
    public static final int SHOW_RIGHT_MENU = 2;

    /**
     * 滑动状态的一种，表示正在隐藏左侧菜单
     */
    public static final int HIDE_LEFT_MENU = 3;

    /**
     * 滑动状态的一种，表示正在隐藏右侧菜单
     */
    public static final int HIDE_RIGHT_MENU = 4;

    private int slideState;
    private int screenWidth;

    /**
     * 在判断为滚动之前手指可以移动的最大值
     */
    private int touchSlop;

    /**
     * 手指按下时的分类
     */
    private float xDown;
    private float yDown;
    /**
     * 手指移动时的坐标
     */
    private float xMove;
    private float yMove;
    /**
     * 手指抬起时的坐标
     */
    private float xUp;

    private boolean isLeftMenuVisible;
    private boolean isRightMenuVisible;
    private boolean isSliding;

    private View leftMenuLayout;
    private View rightMenuLayout;
    private View contentLayout;

    /**
     * 用于监听滑动事件的View
     */
    private View mBindView;

    private MarginLayoutParams leftMenuLayoutParams;
    private MarginLayoutParams rightMenuLayoutParams;
    private RelativeLayout.LayoutParams contentLayoutParams;

    /**
     * 用于计算手指滑动的速度
     */
    private VelocityTracker mVelocityTracker;

    /**
     *
     * @param context
     * @param attrs
     */
    public BesideSlidingLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        screenWidth = wm.getDefaultDisplay().getWidth();
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    /**
     * 绑定监听滑动事件的view
     * @param bindView
     *          需要绑定的view对象
     */
    public void setScrollEvent(View bindView) {
        mBindView = bindView;
        mBindView.setOnTouchListener(this);
    }

    /**
     * 将界面滚动到左侧菜单界面，滚动速度为-30
     */
    public void scrollToLeftMenu() {
        new LeftMenuScrollTask().execute(-30);
    }

    /**
     * 将界面滚动到右侧菜单界面，滚动速度为-30
     */
    public void scrollToRightMenu() {
        new RightMenuScrollTask().execute(-30);
    }

    /**
     * 将界面从左侧菜单界面滚动到主界面，滚动速度为30
     */
    public void scrollToContentMenuFromLeftMenu() {
        new LeftMenuScrollTask().execute(30);
    }

    /**
     * 将界面从右侧菜单界面滚动到主界面，滚动速度为30
     */
    public void scrollToContentMenuFromRightMenu() {
        new RightMenuScrollTask().execute(30);
    }

    /**
     * 左侧菜单是否完全显示出来，滑动过程中此值无效
     * @return 左侧菜单完全显示出来则返回true，否则返回false
     */
    public boolean isLeftLayoutVisible() {
        return isLeftMenuVisible;
    }

    /**
     * 右侧菜单是否完全显示出来，滑动过程中此值无效
     * @return 右侧菜单完全显示则返回true，否则返回false
     */
    public boolean isRightLayoutVisible() {
        return isRightMenuVisible;
    }

    /**
     * 在onLayout中更新左侧菜单右侧菜单以及主界面布局的参数
     * @param isChanged 界面是否变化
     * @param l left
     * @param t top
     * @param r right
     * @param b bottom
     */
    @Override
    protected void onLayout(boolean isChanged, int l, int t, int r, int b) {
        super.onLayout(isChanged, l, t, r, b);
        if (isChanged) {
            //获取左侧菜单布局对象,getChildAt(int i)获取此View的第i个布局，这里是BesideSlidingLayout的子布局
            //getChildAt(int index)index取决布局的顺序
            leftMenuLayout = this.getChildAt(0);
            leftMenuLayoutParams = (MarginLayoutParams)leftMenuLayout.getLayoutParams();
            //获取右侧菜单布局
            rightMenuLayout = this.getChildAt(1);
            rightMenuLayoutParams = (MarginLayoutParams)rightMenuLayout.getLayoutParams();
            //获取主内容视图布局
            contentLayout = this.getChildAt(2);
            contentLayoutParams = (RelativeLayout.LayoutParams)contentLayout.getLayoutParams();
            contentLayoutParams.width = screenWidth;
            contentLayout.setLayoutParams(contentLayoutParams);
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        createVelocityTracker(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                //手指按下 getRawX是相对于屏幕的位置坐标
                xDown = event.getRawX();
                yDown = event.getRawY();
                //初始化滑动状态为无动作
                slideState = DO_NOTHING;
                break;
            case MotionEvent.ACTION_MOVE:
                xMove = event.getRawX();
                yMove = event.getRawY();
                //手指移动，计算移动的距离
                int moveDisX = (int) (xMove - xDown);
                int moveDisY = (int) (yMove - yDown);
                //检查当前滑动状态
                checkSlideState(moveDisX, moveDisY);
                //根据当前滑动状态决定如何偏移内容布局
                //TODO 显示时怎样变化的？？？
                switch (slideState) {
                    case SHOW_LEFT_MENU:
                        contentLayoutParams.rightMargin = -moveDisX;
                        checkLeftMenuBorder();
                        contentLayout.setLayoutParams(contentLayoutParams);
                        break;
                    case SHOW_RIGHT_MENU:
                        contentLayoutParams.leftMargin = moveDisX;
                        checkRightMenuBorder();
                        contentLayout.setLayoutParams(contentLayoutParams);
                        break;
                    case HIDE_LEFT_MENU:
                        contentLayoutParams.rightMargin = -leftMenuLayoutParams.width - moveDisX;
                        checkLeftMenuBorder();
                        contentLayout.setLayoutParams(contentLayoutParams);
                        break;
                    case HIDE_RIGHT_MENU:
                        contentLayoutParams.leftMargin = -rightMenuLayoutParams.width + moveDisX;
                        checkRightMenuBorder();
                        contentLayout.setLayoutParams(contentLayoutParams);
                    default:
                        break;
                }
                break;
            case MotionEvent.ACTION_UP:
                xUp = event.getRawX();
                int upDisX = (int) (xUp - xDown);
                if (isSliding) {
                    //手指抬起时判断当前手势的意图
                    switch (slideState) {
                        case SHOW_LEFT_MENU:
                            if (shouldScrollToLeftMenu()) {
                                scrollToLeftMenu();
                            } else {
                                scrollToContentMenuFromLeftMenu();
                            }
                            break;
                        case SHOW_RIGHT_MENU:
                            if (shouldScrollToRightMenu()) {
                                scrollToRightMenu();
                            } else {
                                scrollToContentMenuFromRightMenu();
                            }
                            break;
                        case HIDE_LEFT_MENU:
                            if (shouldScrollToContentFromLeftMenu()) {
                                scrollToContentMenuFromLeftMenu();
                            } else {
                                scrollToLeftMenu();
                            }
                            break;
                        case HIDE_RIGHT_MENU:
                            if (shouldScrollToContentFromRightMenu()) {
                                scrollToContentMenuFromRightMenu();
                            } else {
                                scrollToRightMenu();
                            }
                            break;
                        default:
                            break;
                    }
                } else if ((upDisX < touchSlop) && isLeftMenuVisible) {
                    //当显示为左侧菜单时，点击一下主布局内容则直接滚动到主布局
                    scrollToContentMenuFromLeftMenu();
                } else if ((upDisX < touchSlop) && isRightMenuVisible) {
                    //右侧
                    scrollToContentMenuFromRightMenu();
                }
                recycleVelocityTracker();
                break;
            default:
                break;
        }
        if(v.isEnabled()) {
            if (isSliding) {
                //正在滑动时让控件得不到焦点
                unFocusBindView();
                return true;
            }
            if (isLeftMenuVisible || isRightMenuVisible) {
                //当左侧或右侧布局显示时，将绑定控件的事情屏蔽掉
                return true;
            }
            return false;
        }
        return true;
    }

    /**
     * 根据手指移动的距离判断当前滑动意图，然后赋值给slideState
     * @param moveDisX 横向移动的距离
     * @param moveDisY 纵向移动的距离
     */
    private void checkSlideState(int moveDisX, int moveDisY) {
        if( isLeftMenuVisible) {
            if ( (!isSliding) && (Math.abs(moveDisX) >= touchSlop) && (moveDisX < 0)) {
                isSliding = true;
                slideState = HIDE_LEFT_MENU;
            }
        } else if ( isRightMenuVisible) {
            if ( (!isSliding) && (Math.abs(moveDisX) >= touchSlop) && (moveDisX > 0)) {
                isSliding = true;
                slideState = HIDE_RIGHT_MENU;
            }
        } else {
            if ( (!isSliding) && (Math.abs(moveDisX) >= touchSlop) && (moveDisX < 0) &&
                    (Math.abs(moveDisY) < touchSlop)) {
                isSliding = true;
                slideState = SHOW_RIGHT_MENU;
                contentLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
                contentLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                contentLayout.setLayoutParams(contentLayoutParams);
                //如果想要显示右侧菜单，则将显示右侧菜单，隐藏左侧菜单
                rightMenuLayout.setVisibility(View.VISIBLE);
                leftMenuLayout.setVisibility(View.GONE);
            } else if ( (!isSliding) && (Math.abs(moveDisX) >= touchSlop) && (moveDisX > 0) &&
                    (Math.abs(moveDisY) < touchSlop)) {
                isSliding = true;
                slideState = SHOW_LEFT_MENU;
                contentLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                contentLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0);
                contentLayout.setLayoutParams(contentLayoutParams);
                //如果想要显示左侧菜单，则将显示左侧菜单，隐藏右侧菜单
                leftMenuLayout.setVisibility(View.VISIBLE);
                rightMenuLayout.setVisibility(View.GONE);
            }
        }
    }

    /**
     * 让可获得焦点的控件在滑动时失去焦点
     */
    private void unFocusBindView() {
        if (mBindView != null) {
            mBindView.setPressed(false);
            mBindView.setFocusable(false);
            mBindView.setFocusableInTouchMode(false);
        }
    }

    /**
     * 创建VelocityTracker对象，并将触摸事件添加到VelocityTracker
     * @param event
     *          右侧布局监听控件的滑动事件
     */
    private void createVelocityTracker(MotionEvent event) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);
    }

    /**
     * 获取滑动速度
     * @return 滑动速度，单位为每秒钟移动的像素值数
     */
    private int getScrollVelocity() {
        mVelocityTracker.computeCurrentVelocity(1000);
        int velocity = (int) mVelocityTracker.getXVelocity();
        return Math.abs(velocity);
    }

    /**
     * 回收VelocityTracker对象
     */
    private void recycleVelocityTracker() {
        mVelocityTracker.recycle();
        mVelocityTracker = null;
    }

    /**
     * 判断是否应该显示左边菜单
     * @return 如果手指移动距离大于左菜单的一半或者移动速度大于指定速度，则返回去true，否则false
     */
    private boolean shouldScrollToLeftMenu() {
        if ( ((xUp - xDown) > (leftMenuLayoutParams.width / 2)) || (getScrollVelocity() > SNAP_VELOCITY)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 判断是否应该显示右边菜单
     * @return 如果手指移动距离大于右菜单的一半或者移动速度大于指定速度，则返回去true，否则false
     */
    private boolean shouldScrollToRightMenu() {
        if ( ((xDown - xUp) > (rightMenuLayoutParams.width / 2)) || (getScrollVelocity() > SNAP_VELOCITY)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 判断是否应该从左边菜单滑动到主布局
     * @return 如果手指移动距离大于左菜单的一半或者移动速度大于指定速度，则返回去true，否则false
     */
    private boolean shouldScrollToContentFromLeftMenu() {
        //注意不是up-down ？？？？
        if ( ((xDown - xUp) > (leftMenuLayoutParams.width / 2)) || (getScrollVelocity() > SNAP_VELOCITY)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 判断是否应该从右边菜单滑动到主布局
     * @return 如果手指移动距离大于右菜单的一半或者移动速度大于指定速度，则返回去true，否则false
     */
    private boolean shouldScrollToContentFromRightMenu() {
        if ( ((xUp - xDown) > (rightMenuLayoutParams.width / 2)) || (getScrollVelocity() > SNAP_VELOCITY)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 滑动过程中检查左侧菜单边界值，防止绑定布局滑出屏幕
     */
    private void checkLeftMenuBorder() {
        if (contentLayoutParams.rightMargin > 0) {
            contentLayoutParams.rightMargin = 0;
        } else if (contentLayoutParams.rightMargin < -leftMenuLayoutParams.width) {
            contentLayoutParams.rightMargin = -leftMenuLayoutParams.width;
        }
    }

    /**
     * 检查右侧边界值防止滑出屏幕
     */
    private void checkRightMenuBorder() {
        if (contentLayoutParams.leftMargin > 0) {
            contentLayoutParams.leftMargin = 0;
        } else if (contentLayoutParams.leftMargin < -rightMenuLayoutParams.width) {
            contentLayoutParams.leftMargin = -rightMenuLayoutParams.width;
        }
    }

    class LeftMenuScrollTask extends AsyncTask<Integer, Integer, Integer> {

        /**
         * 用于执行较为费时的后台操作，接收入参并返回计算结果，在执行过程中可以调用publishProgress来更新UI
         * @param speed
         * @return rightMargin
         */
        @Override
        protected Integer doInBackground(Integer... speed) {
            int rightMargin = contentLayoutParams.rightMargin;
            //根据传入的速度滚动界面，当滚动达到边界时跳出循环
            while(true) {
                rightMargin = rightMargin + speed[0];
                if (rightMargin < -leftMenuLayoutParams.width) {
                    rightMargin = -leftMenuLayoutParams.width;
                    break;
                }
                if (rightMargin > 0) {
                    rightMargin = 0;
                    break;
                }
                publishProgress(rightMargin);
                //为了看到滚动效果，每次循环线程睡眠一段时间，才能让肉眼可见动画
                try {
                    Thread.sleep(15);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (speed[0] > 0) {
                isLeftMenuVisible = false;
            } else {
                isLeftMenuVisible = true;
            }
            isSliding = false;
            return rightMargin;
        }

        /**
         * 在调用publishProgress时会执行此方法，将进度信息直接更新到UI组件上
         * @param rightMargin
         */
        @Override
        protected void onProgressUpdate(Integer... rightMargin) {
            contentLayoutParams.rightMargin = rightMargin[0];
            contentLayout.setLayoutParams(contentLayoutParams);
            unFocusBindView();
        }

        /**
         * 当后台操作完成后此方法将会被调用，计算结果会作为入参会直接显示到UI组件里
         * @param rightMargin
         *          入参即是doInBackground的返回值
         */
        @Override
        protected void onPostExecute(Integer rightMargin) {
            contentLayoutParams.rightMargin = rightMargin;
            contentLayout.setLayoutParams(contentLayoutParams);
        }
    }

    /**
     *
     * execute(Params... params)
     * AsyncTask<Params, Progress, Result>
     *     doInBackground(Params... params)
     *     onProgressUpdate(Progress)
     *     publishProgress(Progress)
     *     onPostExecute(Result)
     */
    class RightMenuScrollTask extends AsyncTask<Integer, Integer, Integer> {

        /**
         * 用于执行较为费时的后台操作，接收入参并返回计算结果，在执行过程中可以调用publishProgress来更新UI
         * @param speed
         * @return leftMargin
         */
        @Override
        protected Integer doInBackground(Integer... speed) {
            int leftMargin = contentLayoutParams.leftMargin;
            //根据传入的速度滚动界面，当滚动达到边界时跳出循环
            while(true) {
                //每次向左偏离屏幕右边缘30
                leftMargin = leftMargin + speed[0];
                if (leftMargin < -rightMenuLayoutParams.width) {
                    leftMargin = -rightMenuLayoutParams.width;
                    break;
                }
                if (leftMargin > 0) {
                    leftMargin = 0;
                    break;
                }
                publishProgress(leftMargin);
                //为了看到滚动效果，每次循环线程睡眠一段时间，才能让肉眼可见动画
                try {
                    Thread.sleep(15);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (speed[0] > 0) {
                isRightMenuVisible = false;
            } else {
                isRightMenuVisible = true;
            }
            isSliding = false;
            return leftMargin;
        }

        /**
         * 在调用publishProgress时会执行此方法，将进度信息直接更新到UI组件上
         * @param leftMargin
         */
        @Override
        protected void onProgressUpdate(Integer... leftMargin) {
            contentLayoutParams.leftMargin = leftMargin[0];
            contentLayout.setLayoutParams(contentLayoutParams);
            unFocusBindView();
        }

        /**
         * 当后台操作完成后此方法将会被调用，计算结果会作为入参会直接显示到UI组件里
         * @param leftMargin
         *          入参即是doInBackground的返回值
         */
        @Override
        protected void onPostExecute(Integer leftMargin) {
            contentLayoutParams.leftMargin = leftMargin;
            contentLayout.setLayoutParams(contentLayoutParams);
        }
    }
}
