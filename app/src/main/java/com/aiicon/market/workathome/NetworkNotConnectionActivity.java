package com.aiicon.market.workathome;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class NetworkNotConnectionActivity extends AppCompatActivity {

    /**
     * 네트워크 오류
     * NOTE: 네트워크 오류시 alert창을 띄워준다
     * */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.network);

        AlertDialog.Builder alert_confirm = new AlertDialog.Builder(NetworkNotConnectionActivity.this);
        alert_confirm.setMessage("네트워크 연결이 원활하지 않습니다\n모바일데이터 및 WIFI 연결상태를 확인해주세요.");
        alert_confirm.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intent = new Intent(getApplicationContext(), WebViewActivity.class);
                startActivity(intent);
                finish();
            }
        });
        AlertDialog alert = alert_confirm.create();
        alert.setTitle("알림");
        alert.setCanceledOnTouchOutside(false);
        alert.setCancelable(false);
        alert.show();
    }
}
