package com.example.nfc;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private NfcAdapter mNfcAdapter;
    private Tag mTag;
    private Button queryButton;
    private TextView balanceText;
    private EditText amountEdit;
    private Button rechargeButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 获取NFC适配器
        mNfcAdapter = M1CardUtils.isNfcAble(this);
        M1CardUtils.setPendingIntent(PendingIntent.getActivity(this, 0, new Intent(this,
                getClass()), 0));
        mTag = getIntent().getParcelableExtra(NfcAdapter.EXTRA_TAG);
        // 获取布局中的控件
        queryButton = findViewById(R.id.query_button);
        balanceText = findViewById(R.id.balance_text);
        amountEdit = findViewById(R.id.amount_edit);
        rechargeButton = findViewById(R.id.recharge_button);

        // 设置查询按钮的监听器
        queryButton.setOnClickListener(v -> {
            // 执行查询余额的操作
            String balance = getBalanceFromNFC(mTag);
            balanceText.setText("当前余额：¥ " + balance);
        });

        // 设置确认充值按钮的监听器
        rechargeButton.setOnClickListener(v -> {
            // 执行充值操作
            String amount = amountEdit.getText().toString();
            if (!TextUtils.isEmpty(amount)) {
                doRecharge(mTag, amount);
            }
        });
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        mNfcAdapter = M1CardUtils.isNfcAble(this);
        M1CardUtils.setPendingIntent(PendingIntent.getActivity(this, 0, new Intent(this,
                getClass()), 0));
        mTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mNfcAdapter != null) {
            mNfcAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mNfcAdapter != null) {
            mNfcAdapter.enableForegroundDispatch(this, M1CardUtils.getPendingIntent(),
                    null, null);
        }
    }


    private String getBalanceFromNFC(Tag tag) {
        if (tag == null) {
            return "请把卡挪开,重新刷卡";
        }
        try {
            byte[] bytes = M1CardUtils.readBlock(tag, 1, 0);
            String balance = getBalanceFromHexString(M1CardUtils.bytesToHexString(bytes));
            System.out.println("余额 = " + balance);
            return balance;
        } catch (IOException e) {
            return "获取余额失败";
        }
    }

    public static String getBalanceFromHexString(String hexString) {
        String balanceHexString = hexString.substring(0, 4);
        BigDecimal balanceDecimal = new BigDecimal(Long.parseLong(balanceHexString, 16));
        BigDecimal divisor = new BigDecimal(100);
        BigDecimal result = balanceDecimal.divide(divisor, 2, RoundingMode.HALF_UP);
        return result.toString();
    }


    private void doRecharge(Tag tag, String amount) {
        String str = convertAmountToHexString(Integer.parseInt(amount));
        System.out.println("转换后的hex = " + str);
        byte[] balanceByte = M1CardUtils.hexStringToByteArray(str);
        // 执行充值操作
        try {
            if (M1CardUtils.writeBlock(tag, 1, balanceByte)) {
                Toast.makeText(this, "充值成功：" + amount, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "充值失败", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(this, "充值失败", Toast.LENGTH_SHORT).show();
            throw new RuntimeException(e);
        }
        Toast.makeText(this, "充值成功：" + amount, Toast.LENGTH_SHORT).show();
    }

    public static String convertAmountToHexString(int amount) {
        String hexString = Integer.toHexString(amount * 100); // 转为十六进制字符串，并乘以100
        hexString = hexString.toUpperCase(Locale.getDefault()); // 转为大写字母
        hexString += "000000000000000000000000005A"; // 补充校验位
        return hexString;
    }

}