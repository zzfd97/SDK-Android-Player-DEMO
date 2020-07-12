package com.mediapro.demo;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {

    private EditText mEditServerIp;
    private EditText mEditRoomId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        {//* Init UI
            mEditServerIp = (EditText) findViewById(R.id.login_server_ip);
            mEditRoomId = (EditText)findViewById(R.id.login_room_id);

        }
    }

    public void OnBtnClicked(View view) {
        //服务IP地址
        String serverIp = mEditServerIp.getEditableText().toString();
        if (Isipv4(serverIp) == false)
        {
            Toast.makeText(MainActivity.this, "IP地址非法", Toast.LENGTH_SHORT).show();
            return;
        }
        //房间ID
        String strRoomId = mEditRoomId.getEditableText().toString().trim();
        if(strRoomId.length() == 0) {
            Toast.makeText(this, "请输入房间ID", Toast.LENGTH_SHORT).show();
            return;
        }
        long roomId = 1;
        try {
            roomId = Long.parseLong(strRoomId);
        } catch (Exception e) {
            Toast.makeText(this, "请输入合法的房间ID", Toast.LENGTH_SHORT).show();
            return;
        }


        switch(view.getId()){
            case R.id.btn_watch_live:
                Intent it = new Intent(this, PlayerActivity.class);
                Bundle bd = new Bundle();
                bd.putString("server_ip", serverIp);
                bd.putLong("room_id", roomId);
                it.putExtras(bd);
                startActivity(it);
                break;
        }

    }

    public static boolean Isipv4(String ipv4)
    {
        if(ipv4==null || ipv4.length()==0){
            return false;//字符串为空或者空串
        }
        String[] parts=ipv4.split("\\.");//因为java doc里已经说明, split的参数是reg, 即正则表达式, 如果用"|"分割, 则需使用"\\|"
        if(parts.length!=4){
            return false;//分割开的数组根本就不是4个数字
        }
        for(int i=0;i<parts.length;i++){
            try{
                int n=Integer.parseInt(parts[i]);
                if(n<0 || n>255){
                    return false;//数字不在正确范围内
                }
            }catch (NumberFormatException e) {
                return false;//转换数字不正确
            }
        }
        return true;
    }
}
