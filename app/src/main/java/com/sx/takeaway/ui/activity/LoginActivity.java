package com.sx.takeaway.ui.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.sx.takeaway.R;
import com.sx.takeaway.utils.PromptManager;
import com.sx.takeaway.utils.SMSUtil;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.smssdk.EventHandler;
import cn.smssdk.SMSSDK;

/**
 * @Author sunxin
 * @Date 2017/6/14 17:22
 * @Description 用户登录界面
 * 1，引入短信验证的相关工具
 * 2，获取验证码
 * 3，提交验证码
 * 4，把手机号发送到自己的服务器，进行用户的添加操作
 */

public class LoginActivity extends BaseActivity {

    private static final String APPKEY = "1eb03a20b554c";
    private static final java.lang.String APPSECRET = "87f874438daaeef992908772b27a8e3a";
    private static final int SENDING = -9;//正在发送
    private static final int RESEND = -8;//重新发送

    @BindView(R.id.iv_user_back)
    ImageView mIvUserBack;
    @BindView(R.id.iv_user_password_login)
    TextView mIvUserPasswordLogin;
    @BindView(R.id.et_user_phone)
    EditText mEtUserPhone;
    @BindView(R.id.tv_user_code)
    TextView mTvUserCode;
    @BindView(R.id.et_user_code)
    EditText mEtUserCode;
    @BindView(R.id.login)
    TextView mLogin;
    private String mPhone;
    //读秒数
    private int seconds = 60;


    /**
     * 1、	权限校验: SMSUtil.checkPermission(this);
     * 2、	初始化工具: SMSSDK.initSDK(this, APPKEY, APPSECRET, true);
     * 3、	注册事件监听：SMSSDK.registerEventHandler(eventHandler);
     * 4、	获取验证码：SMSSDK.getVerificationCode("86", phone);监听事件触发。
     * 5、	发送验证码：SMSSDK.submitVerificationCode("86", phone, code.trim());监听事件触发。
     * 6、	注销监听。
     */

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == SENDING) {
                mTvUserCode.setText("重新发送（" + seconds + "）");
                mTvUserCode.setEnabled(true);
            } else if (msg.what == RESEND) {
                mTvUserCode.setText("获取验证码");
            }else {
                int result = msg.arg1;
                int event = msg.arg2;
                Object data = msg.what;
                if (result == SMSSDK.RESULT_COMPLETE) {
                    //回调完成
                    if (event == SMSSDK.EVENT_SUBMIT_VERIFICATION_CODE) {
                        //提交验证码成功
                        Toast.makeText(LoginActivity.this, "提交验证成功", Toast.LENGTH_SHORT).show();
                    } else if (event == SMSSDK.EVENT_GET_VERIFICATION_CODE) {
                        //获取验证码成功
                        Toast.makeText(LoginActivity.this, "获取验证码成功", Toast.LENGTH_SHORT).show();
                    } else if (event == SMSSDK.EVENT_GET_SUPPORTED_COUNTRIES) {
                        //返回支持发送验证码的国家列表
                    }
                } else {
                    // TODO: 2017/6/17 验证出错了
                    ((Throwable) data).printStackTrace();
                    Toast.makeText(LoginActivity.this, "验证出错了", Toast.LENGTH_SHORT).show();
                    PromptManager.closeProgressDialog();
                }
            }
        }
    };


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);
        //1,权限校验
        SMSUtil.checkPermission(this);
        //2,初始化工具
        SMSSDK.initSDK(this, APPKEY, APPSECRET, true);
    }

    @OnClick({R.id.tv_user_code, R.id.login})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_user_code:
                //获取验证码
                //校验
                mPhone = mEtUserPhone.getText().toString();
                System.out.println("获取验证码" + mPhone);
                boolean phoneNums = SMSUtil.judgePhoneNums(this, mPhone);
                if (!phoneNums) {
                    return;
                }
                //获取验证码
                SMSSDK.getVerificationCode("86", mPhone);

                //读秒
                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        for (; seconds > 0; seconds--) {
                            mHandler.sendEmptyMessage(SENDING);
                            if (seconds < 0) {
                                break;
                            }
                            SystemClock.sleep(999);
                        }

                        mHandler.sendEmptyMessage(RESEND);
                    }
                }).start();
                break;
            case R.id.login:
                //登录，发送验证码
                String code = mEtUserCode.getText().toString();
                if (!TextUtils.isEmpty(code)) {
                    SMSSDK.submitVerificationCode("86", mPhone, code);
                    PromptManager.showProgressDialog(this);
                }
                break;
        }
    }

    private EventHandler eventHandler = new EventHandler() {
        @Override
        public void afterEvent(int event, int result, Object data) {
            //交给我们自己的Handler处理，这里是子线程
            Message msg = Message.obtain();
            msg.arg1 = event;
            msg.arg2 = result;
            msg.obj = data;
            mHandler.sendMessage(msg);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        //注册监听
        SMSSDK.registerEventHandler(eventHandler);
    }

    @Override
    protected void onPause() {
        super.onPause();
        //注销监听
        SMSSDK.unregisterAllEventHandler();
    }
}