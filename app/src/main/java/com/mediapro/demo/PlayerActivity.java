package com.mediapro.demo;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import org.webrtc.SurfaceViewRenderer;

import com.sd.Constant;
import com.sd.Constant.LogLevel;
import com.sd.SDInterfacePlayer;
import com.sd.SDInterface;

import static com.sd.Constant.SystemStatusType.SYS_NOTIFY_EXIT_KICKED;
import static com.sd.Constant.SystemStatusType.SYS_NOTIFY_RECON_START;
import static com.sd.Constant.SystemStatusType.SYS_NOTIFY_RECON_SUCCESS;
import static com.sd.Constant.SystemStatusType.SYS_NOTIFY_ONPOSITION;
import static com.sd.Constant.SystemStatusType.SYS_NOTIFY_OFFPOSITION;

/**
 * Created by Eric on 2016/9/16.
 */
public class PlayerActivity extends Activity {
    private RelativeLayout mLayout = null;

    //播放窗口
    private SurfaceViewRenderer mSurfaceView = null;
    
    //播放API
    private SDInterfacePlayer mPlayer = null;
    
    //基础API
    private SDInterface mInterface = null;

    //服务器IP
    String mServerIp = null;
    //房间ID
    long mRoomId = 0;
    //DEMO使用随机生成的用户ID
    int mUserId = 100000 + (int)(Math.random() * (999999 - 100000));
    //DEMO固定使用1号域
    int mDomainId = 1;
    //本DEMO接收0号位置视频
    int mRecvPosition = 0;
    
    //是否启动播放
    boolean mbPlayStart = false;

    //日志文件存放路径
    private String mLogfileDir = "/sdcard/mediapro/";



    //来自底层消息的处理
    private final Handler mHandler = new Handler(){

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what)
            {
                case SYS_NOTIFY_EXIT_KICKED:
                    //停止播放
                    stopPlay();
                    //下线
                    offLineProcess();
                    Toast.makeText(PlayerActivity.this, "账号在其他位置登录，与服务器断开连接", Toast.LENGTH_SHORT).show();
                    break;
                case SYS_NOTIFY_RECON_START:
                    Toast.makeText(PlayerActivity.this, "网络超时，开始重连服务器...", Toast.LENGTH_SHORT).show();
                    break;
                case SYS_NOTIFY_RECON_SUCCESS:
                    Toast.makeText(PlayerActivity.this, "连服务器成功", Toast.LENGTH_SHORT).show();
                    break;
                case SYS_NOTIFY_ONPOSITION:
                    long uid_on = (long)msg.arg1;
                    int on_position = msg.arg2;
                    Toast.makeText(PlayerActivity.this, uid_on + " 加入房间，位置：" + on_position, Toast.LENGTH_SHORT).show();
                    break;
                case SYS_NOTIFY_OFFPOSITION:
                    long uid_off = (long)msg.arg1;
                    int off_position = msg.arg2;
                    Toast.makeText(PlayerActivity.this, uid_off + " 离开房间，位置：" + off_position, Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }

    };

    private static final String TAG = "SDMedia";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        mLayout = (RelativeLayout) getLayoutInflater().inflate(
                R.layout.activity_player, null);
        setContentView(mLayout);

        //Init UI
        mSurfaceView = (SurfaceViewRenderer) findViewById(R.id.suface_view);

        //初始化基础API、播放API
        initAvResource();

        //登录服务器
        int nRet = onLineProcess();
        if (nRet == 0)
        {
            //开始播放渲染
            //获得渲染窗口宽高比，以便对底层渲染进行宽高比指导。
            //注意：底层只关心宽高比值，而非精确宽高像素值
            mSurfaceView.post(new Runnable()
                              {
                                  @Override
                                  public void run() {
                                        int rendHeight = mSurfaceView.getMeasuredHeight();
                                        int rendWidth = mSurfaceView.getMeasuredWidth();
                                        Log.i(TAG, "get render width:" + rendWidth + "  render height:" + rendHeight);
                                        
                                        //开始播放
                                        startPlay(rendWidth, rendHeight);
                                  }
                              }
            );
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        //停止播放
        stopPlay();
        
        //下线
        offLineProcess();
        
        //资源回收
        uninitAvResource();
    }

    /**
     * the button click event listener
     *
     * @param btn
     */
    public void OnBtnClicked(View btn) {
        if (btn.getId() == R.id.btn_close) {
            stopPlay();
            finish();
        }
    }



    //初始化基础API、播放API
    private void initAvResource()
    {
        mInterface = new SDInterface(mHandler);
        mPlayer = new SDInterfacePlayer();


        //服务器IP
        mServerIp = getIntent().getExtras().getString("server_ip");
        //房间ID
        mRoomId = getIntent().getExtras().getLong("room_id");
    
    
        // 初始化系统，指定服务器IP地址、本地客户端输出日志文件级别和存放路径
        int ret = mInterface.SDsysinit(mServerIp, mLogfileDir, LogLevel.LOG_LEVEL_INFO);
        if(0 != ret)
        {
            Toast.makeText(this, "初始化音视频资源返回错误编码:" + ret, Toast.LENGTH_LONG).show();
            Log.e(TAG, "SDsysinit failed return:" + ret);
        }
		
		mPlayer.Init(this, mSurfaceView, false, false);
    }
    
    //反初始化基础API、播放API
    private void uninitAvResource()
    {
        // 相关资源回收
        mPlayer.Destroy();
        mInterface.SDsysexit();
    }
    
    
    //登录服务器
    private int onLineProcess()
    {

        //FEC参数
        int nRedunRatio = 0;
        int nGroupSize = 28;
        boolean bEnableNack = true;
        int nJitterBuffTime = 200;

        //设置传输参数，未调用则使用默认值
        mInterface.SDSetTransParams(nRedunRatio, nGroupSize, bEnableNack == true ? 1:0, nJitterBuffTime);

        //本DEMO接收所有成员音频（服务器混音模式下会走0号位置下发混音后的音频流）
        //本DEMO接收指定位置视频
        int recvMaskVideo = 0x1 << (mRecvPosition);
        mInterface.SDSetAvDownMasks(0xFFFFFFFF, recvMaskVideo);

        int ret = mInterface.SDOnlineUser((int)mRoomId, mUserId, Constant.UserType.USER_TYPE_AV_RECV_ONLY, mDomainId);
        Log.i(TAG, "SDOnlineUser return:" + ret);
        if (ret != 0)
        {
            Toast.makeText(this, "登录服务器失败:" + ret, Toast.LENGTH_LONG).show();
            Log.e(TAG, "SDOnlineUser failed");
            return ret;
        }

        return 0;
    }

    //下线服务器
    private void offLineProcess()
    {
        mInterface.SDOfflineUser();
    }
    
    //开始播放
    private void startPlay(int rendWidth, int rendHeight)
    {
        mPlayer.startPlay(mRecvPosition, rendWidth, rendHeight);
        mbPlayStart = true;
    }
    
    //停止播放
    private void stopPlay()
    {
        if (mbPlayStart == true)
        {
            mPlayer.stopPlay();
            mbPlayStart = false;
        }
    }
}
