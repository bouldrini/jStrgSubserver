package com.company.network_management.core;

import com.company.Main;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;

public class Cryptor {
    private static final byte[] SALT = {
            (byte) 0xde, (byte) 0x33, (byte) 0x10, (byte) 0x12,
            (byte) 0xde, (byte) 0x33, (byte) 0x10, (byte) 0x12,
    };

    // ENCRYPTION

    /**
     * encrypts a given string
     *
     * @param _text        string is the text you want to encrypt
     * @return encrypted string
     * @throws GeneralSecurityException     internal
     * @throws UnsupportedEncodingException internal
     */
    public static String encrypt(String _secret1, String _secret2, String _text) throws GeneralSecurityException, UnsupportedEncodingException {
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(_secret1);
        SecretKey key = keyFactory.generateSecret(new PBEKeySpec(_secret2.toCharArray()));
        Cipher pbeCipher = Cipher.getInstance(_secret1);
        pbeCipher.init(Cipher.ENCRYPT_MODE, key, new PBEParameterSpec(SALT, 20));
        return base64Encode(pbeCipher.doFinal(_text.getBytes("UTF-8")));
    }

    private static String base64Encode(byte[] bytes) {
        return new BASE64Encoder().encode(bytes);
    }

    private static byte[] base64Decode(String property) throws IOException {
        return new BASE64Decoder().decodeBuffer(property);
    }

    /**
     * decrypt a given string
     *
     * @param _text        string is the text you want to decrypt
     * @return decrypted string
     * @throws GeneralSecurityException internal
     * @throws IOException              internal
     */
    public static String decrypt(String _secret1, String _secret2,  String _text) throws GeneralSecurityException, IOException {
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(_secret1);
        SecretKey key = keyFactory.generateSecret(new PBEKeySpec(_secret2.toCharArray()));
        Cipher pbeCipher = Cipher.getInstance(_secret1);
        pbeCipher.init(Cipher.DECRYPT_MODE, key, new PBEParameterSpec(SALT, 20));
        return new String(pbeCipher.doFinal(base64Decode(_text)), "UTF-8");
    }
}
