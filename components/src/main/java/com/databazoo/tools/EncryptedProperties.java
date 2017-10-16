package com.databazoo.tools;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEParameterSpec;
import java.io.IOException;
import java.util.Properties;

/**
 * Properties implementation that writes and reads encrypted strings. Used for storing project configurations including passwords.
 * @author bobus
 */
public class EncryptedProperties extends Properties {
    private static final byte[] salt = { (byte) 0x03, (byte) 0x07, (byte) 0x0B, (byte) 0x0D, (byte) 0x17, (byte) 0x1D, (byte) 0x1F, (byte) 0x25 };
    private transient Cipher encrypter;
    private transient Cipher decrypter;

    public EncryptedProperties(String password) {
        try {
            PBEParameterSpec ps = new javax.crypto.spec.PBEParameterSpec(salt, 20);
            SecretKeyFactory kf = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
            SecretKey k = kf.generateSecret(new javax.crypto.spec.PBEKeySpec(password.toCharArray()));
            encrypter = Cipher.getInstance("PBEWithMD5AndDES/CBC/PKCS5Padding");
            decrypter = Cipher.getInstance("PBEWithMD5AndDES/CBC/PKCS5Padding");
            encrypter.init(Cipher.ENCRYPT_MODE, k, ps);
            decrypter.init(Cipher.DECRYPT_MODE, k, ps);
        } catch (Exception e) {
            Dbg.notImportant("Encryption failure will be handled later by get* methods", e);
        }
    }

    @Override
    public String getProperty(String key) {
        try {
            String data = super.getProperty(key);
            if (data == null) {
                data = super.getProperty(encrypt(key));
            }
            return data != null ? decrypt(data) : null;

        } catch (Exception e) {
            throw new IllegalStateException("Couldn't decrypt property", e);
        }
    }

    public int getInt(String key) {
        String val = getProperty(key);
        return val == null || val.equals("null") ? 0 : Integer.parseInt(val);
    }

    public long getLong(String key) {
        String val = getProperty(key);
        return val == null || val.equals("null") ? 0 : Long.parseLong(val);
    }

    public String getStr(String key) {
        return getProperty(key);
    }

    public boolean getBool(String key) {
        String val = getProperty(key);
        return val != null && (val.equals("yes") || val.equals("true") || val.equals("1"));
    }

    String[] getStrVector(String key) {
        return getProperty(key).split(" ");
    }

    int[] getIntVector(String key) {
        String prop = getProperty(key);
        if (prop != null && !prop.isEmpty()) {
            int[] ret;
            String[] retStr;
            retStr = prop.split(" ");
            ret = new int[retStr.length];
            for (int j = 0; j < retStr.length; j++) {
                ret[j] = Integer.parseInt(retStr[j]);
            }
            return ret;
        } else {
            return new int[] {};
        }
    }

    @Override
    public synchronized Object setProperty(String key, String value) {
        if (value == null) {
            value = "";
        }
        try {
            return super.setProperty(encrypt(key), encrypt(value));
        } catch (Exception e) {
            Dbg.fixme("setting " + key + " failed", e);
            throw new IllegalStateException("Couldn't encrypt property", e);
        }
    }

    public synchronized void clear(String key) {
        try {
            remove(encrypt(key));
        } catch (Exception e) {
            Dbg.fixme("clearing " + key + " failed", e);
        }
    }

    public synchronized Object setProperty(String key, int value) {
        return setProperty(key, String.valueOf(value));
    }

    public synchronized Object setProperty(String key, long value) {
        return setProperty(key, String.valueOf(value));
    }

    public synchronized Object setProperty(String key, boolean value) {
        return setProperty(key, value ? "true" : "");
    }

    private synchronized String decrypt(String str) throws IOException, IllegalBlockSizeException, BadPaddingException {
        byte[] dec = Base64.decode(str);
		if (dec != null) {
			byte[] utf8 = decrypter.doFinal(dec);
			return new String(utf8, "UTF-8");
		} else {
			return null;
		}
    }

    private synchronized String encrypt(String str) throws IOException, IllegalBlockSizeException, BadPaddingException {
        byte[] utf8 = str.getBytes("UTF-8");
        byte[] enc = encrypter.doFinal(utf8);
        return Base64.encode(enc, true);
    }
}
