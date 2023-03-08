package com.example.nfc;

import android.app.Activity;
import android.app.PendingIntent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * @author
 * @description MifareClassic卡片读写工具类
 */
public class M1CardUtils {

    private static String KEY_A = "FFFFFFFFFFFF";
    private static String KEY_B = "FFFFFFFFFFFF";

    private static PendingIntent pendingIntent;

    public static PendingIntent getPendingIntent() {
        return pendingIntent;
    }

    public static void setPendingIntent(PendingIntent pendingIntent) {
        M1CardUtils.pendingIntent = pendingIntent;
    }

    /**
     * 判断是否支持NFC
     *
     * @return
     */
    public static NfcAdapter isNfcAble(Activity mContext) {
        NfcAdapter mNfcAdapter = NfcAdapter.getDefaultAdapter(mContext);
        if (mNfcAdapter == null) {
            Toast.makeText(mContext, "设备不支持NFC！", Toast.LENGTH_LONG).show();
        }
        if (!mNfcAdapter.isEnabled()) {
            Toast.makeText(mContext, "请在系统设置中先启用NFC功能！", Toast.LENGTH_LONG).show();
        }

        return mNfcAdapter;
    }

    /**
     * 读指定扇区的指定块
     *
     * @param sectorIndex 扇区索引 0~15(16个扇区)
     * @param blockIndex  块索引 0~3
     * @return
     */
    public static byte[] readBlock(Tag tag, int sectorIndex, int blockIndex) throws IOException {
        MifareClassic mifareClassic = MifareClassic.get(tag);
        try {
            String metaInfo = "";
            mifareClassic.connect();
            int type = mifareClassic.getType();//获取TAG的类型
            int sectorCount = mifareClassic.getSectorCount();//获取TAG中包含的扇区数
            String typeS = getMifareClassicType(type);
            metaInfo += "卡片类型：" + typeS + "\n共" + sectorCount + "个扇区\n共" + mifareClassic.getBlockCount() + "个块\n存储空间: " + mifareClassic.getSize() + "B\n";
            Log.d("readCard", metaInfo);
            byte[] data = null;
            String hexString = null;
            if (m1Auth(mifareClassic, sectorIndex)) {
                int bCount;
                int bIndex;
                bCount = mifareClassic.getBlockCountInSector(sectorIndex);//获得当前扇区的所包含块的数量；
                bIndex = mifareClassic.sectorToBlock(sectorIndex);//当前扇区的第1块的块号
                for (int i = 0; i < bCount; i++) {
                    data = mifareClassic.readBlock(bIndex);
                    hexString = bytesToHexString(data);
                    Log.d("readCard", sectorIndex + "扇区" + bIndex + "块：" + hexString);
                    if (blockIndex == i) {
                        break;
                    }
                    bIndex++;
                }
            } else {
                Log.d("readCard", "密码校验失败,扇区：" + sectorIndex);
            }
            return data;
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            try {
                if (mifareClassic != null) {
                    mifareClassic.close();
                }
            } catch (IOException e) {
                throw new IOException(e);
            }
        }
    }

    /**
     * 往指定扇区的指定块写数据
     *
     * @param tag
     * @param sectorIndex 扇区索引 0~15(16个扇区)
     * @param blockIndex  块索引 0~63
     * @param blockByte   写入数据必须是16字节
     * @return
     * @throws IOException
     */
    public static boolean writeBlock(Tag tag, int sectorIndex, byte[] blockByte) throws IOException {
        MifareClassic mifareClassic = MifareClassic.get(tag);
        try {
            mifareClassic.connect();
            if (m1Auth(mifareClassic, sectorIndex)) {
                mifareClassic.writeBlock(4, blockByte);
                mifareClassic.writeBlock(5, blockByte);
            } else {
                return false;
            }
        } catch (IOException e) {
            throw new IOException(e);
        } finally {
            try {
                mifareClassic.close();
            } catch (IOException e) {
                throw new IOException(e);
            }
        }
        return true;

    }


    /**
     * 读指定扇区的所有块
     *
     * @param tag
     * @param sectorIndex 扇区索引 0~15(16个扇区)
     * @return
     */
    public static List<byte[]> readBlock(Tag tag, int sectorIndex) throws IOException {
        MifareClassic mifareClassic = MifareClassic.get(tag);
        List<byte[]> dataList = new ArrayList<byte[]>();
        try {
            String metaInfo = "";
            mifareClassic.connect();
            int type = mifareClassic.getType();//获取TAG的类型
            int sectorCount = mifareClassic.getSectorCount();//获取TAG中包含的扇区数
            String typeS = getMifareClassicType(type);
            metaInfo += "卡片类型：" + typeS + "\n共" + sectorCount + "个扇区\n共" + mifareClassic.getBlockCount() + "个块\n存储空间: " + mifareClassic.getSize() + "B\n";
            Log.d("readCard", metaInfo);
            if (m1Auth(mifareClassic, sectorIndex)) {
                int bCount;
                int bIndex;
                bCount = mifareClassic.getBlockCountInSector(sectorIndex);//获得当前扇区的所包含块的数量；
                bIndex = mifareClassic.sectorToBlock(sectorIndex);//当前扇区的第1块的块号
                for (int i = 0; i < bCount; i++) {
                    byte[] data = mifareClassic.readBlock(bIndex);
                    String hexString = bytesToHexString(data);
                    Log.d("readCard", sectorIndex + "扇区" + bIndex + "块：" + hexString);
                    dataList.add(data);
                    bIndex++;
                }
            } else {
                Log.d("readCard", "密码校验失败,扇区：" + sectorIndex);
            }
            return dataList;
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            try {
                if (mifareClassic != null) {
                    mifareClassic.close();
                }
            } catch (IOException e) {
                throw new IOException(e);
            }
        }
    }

    /**
     * 获取m1卡类型
     *
     * @param type
     * @return
     */
    private static String getMifareClassicType(int type) {
        String str = null;
        switch (type) {
            case MifareClassic.TYPE_CLASSIC:
                str = "TYPE_CLASSIC";
                break;
            case MifareClassic.TYPE_PLUS:
                str = "TYPE_PLUS";
                break;
            case MifareClassic.TYPE_PRO:
                str = "TYPE_PRO";
                break;
            case MifareClassic.TYPE_UNKNOWN:
                str = "TYPE_UNKNOWN";
                break;
        }
        return str;
    }

    /**
     * 密码校验
     *
     * @param mTag
     * @param position
     * @return
     * @throws IOException
     */
    public static boolean m1Auth(MifareClassic mTag, int position) throws IOException {
        if (mTag.authenticateSectorWithKeyA(position, hexStringToByteArray(KEY_A))) {
            return true;
        } else if (mTag.authenticateSectorWithKeyB(position, hexStringToByteArray(KEY_B))) {
            return true;
        }
        return false;
    }

    public static byte[] hexStringToByteArray(String hexString) {
        int len = hexString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16));
        }
        return data;
    }


    public static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder();
        if (src == null || src.length <= 0) {
            return null;
        }
        char[] buffer = new char[2];
        for (int i = 0; i < src.length; i++) {
            buffer[0] = Character.forDigit((src[i] >>> 4) & 0x0F, 16);
            buffer[1] = Character.forDigit(src[i] & 0x0F, 16);
            System.out.println(buffer);
            stringBuilder.append(buffer);
        }
        return stringBuilder.toString();
    }
}
