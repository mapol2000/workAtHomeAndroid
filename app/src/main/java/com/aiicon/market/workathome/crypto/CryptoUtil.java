package com.aiicon.market.workathome.crypto;

import android.content.Context;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CryptoUtil {
    private Context mContext;

    public CryptoUtil(Context context) {
        this.mContext = context;
    }

    public String decryptSeed(byte[] data) throws Exception {
        byte[] pbszUserKey = getKey();

        return new String(KisaSeedECB.SEED_ECB_Decrypt(pbszUserKey, data, 0, data.length));
    }

    private byte[] getKey() throws NoSuchAlgorithmException {
        String packageName = mContext.getPackageName();
        String date = getDate();

        return getMessageDigest((packageName + date).getBytes());
    }

    private String getDate() {
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
        return simpleDateFormat.format(date);
    }

    private byte[] getMessageDigest(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        return digest.digest(data);
    }
}
