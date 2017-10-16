package com.databazoo.tools;

/*
 * Copyright (C) 2011 www.itcsolutions.eu
 *
 * This file is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 2.1, or (at your
 * option) any later version.
 *
 * This file is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 *
 */

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.ShortBufferException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.PBEParametersGenerator;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.generators.PKCS12ParametersGenerator;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

/**
 * Implementation of AES read / write.
 * Bouncy Castle API installed as a library (maven dependency).
 * CBC mode for encryption and decryption.
 *
 * @author Catalin Boja
 */
class BouncyCastleAESwithCBC {
    /**
     * Initiated ciphers
     */
    private PaddedBufferedBlockCipher encryptCipher;
    private PaddedBufferedBlockCipher decryptCipher;

    /**
     * Buffers used to transport the bytes from one stream to another
     */
    private final byte[] inputBuffer = new byte[16];
    private final byte[] outputBuffer = new byte[512];


    /**
     * Cipher parameters - will be derived from given password or encryption key
     */
    private final CipherParameters params;

    /**
     * The initialization vector needed by the CBC mode
     */
    private final byte[] IV;

    /**
     * The default block size
     */
    private static int blockSize = 16;

    /**
     * Constructor with fix-lenght key.
     *
     * @param keyBytes fix-lenght key
     */
    BouncyCastleAESwithCBC(byte[] keyBytes){
        // get the key
        byte[] key = new byte[keyBytes.length];
        System.arraycopy(keyBytes, 0 , key, 0, keyBytes.length);
        params = new KeyParameter(key);

        // random IV vector
        IV = new SecureRandom().generateSeed(blockSize);
    }

    /**
     * Constructor with user-provided password.
     *
     * @param password user-provided password
     */
    BouncyCastleAESwithCBC(String password) {
        PBEParametersGenerator keyGenerator = new PKCS12ParametersGenerator(new SHA256Digest());
        keyGenerator.init(PKCS12ParametersGenerator.PKCS12PasswordToBytes(password.toCharArray()), new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08}, 20);
        params = keyGenerator.generateDerivedParameters(256);

        // random IV vector
        IV = new SecureRandom().generateSeed(blockSize);
    }

    /**
     * Initiate the ciphers
     */
    void initCiphers(){
        // AES block cipher in CBC mode with padding
        encryptCipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()));
        decryptCipher =  new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()));

        //create the IV parameter
        ParametersWithIV parameterIV = new ParametersWithIV(params, IV);

        encryptCipher.init(true, parameterIV);
        decryptCipher.init(false, parameterIV);
    }

    /**
     * Reset the ciphers
     */
    void resetCiphers() {
        if (encryptCipher != null) {
            encryptCipher.reset();
        }
        if (decryptCipher != null) {
            decryptCipher.reset();
        }
    }

    /**
     * Bytes written to outputStream will be encrypted.
     *
     * Read the cleartext bytes from inputStream and write them encrypted to outputStream.
     */
    void cbcEncrypt(InputStream inputStream, OutputStream outputStream)
            throws ShortBufferException,
            IllegalBlockSizeException,
            BadPaddingException,
            DataLengthException,
            IllegalStateException,
            InvalidCipherTextException,
            IOException {
        //put the IV at the beggining of the cipher file
        outputStream.write(IV, 0, IV.length);

        int noBytesRead;        //number of bytes read from input
        int noBytesProcessed;   //number of bytes processed

        while ((noBytesRead = inputStream.read(inputBuffer)) >= 0) {
            noBytesProcessed = encryptCipher.processBytes(inputBuffer, 0, noBytesRead, outputBuffer, 0);
            outputStream.write(outputBuffer, 0, noBytesProcessed);
        }

        noBytesProcessed = encryptCipher.doFinal(outputBuffer, 0);

        outputStream.write(outputBuffer, 0, noBytesProcessed);
        outputStream.flush();

        inputStream.close();
        outputStream.close();
    }

    /**
     * Bytes read from inputStream will be decrypted.
     *
     * Read the encrypted bytes from inputStream and write them in cleartext to outputStream.
     */
    void cbcDecrypt(InputStream inputStream, OutputStream outputStream)
            throws ShortBufferException,
            IllegalBlockSizeException,
            BadPaddingException,
            DataLengthException,
            IllegalStateException,
            InvalidCipherTextException,
            IOException
    {
        // get the IV from the file and reinit the cipher with the IV
        inputStream.read(IV, 0, IV.length);
        this.initCiphers();

        int noBytesRead;        //number of bytes read from input
        int noBytesProcessed;   //number of bytes processed

        while ((noBytesRead = inputStream.read(inputBuffer)) >= 0) {
            noBytesProcessed = decryptCipher.processBytes(inputBuffer, 0, noBytesRead, outputBuffer, 0);
            outputStream.write(outputBuffer, 0, noBytesProcessed);
        }
        noBytesProcessed = decryptCipher.doFinal(outputBuffer, 0);

        outputStream.write(outputBuffer, 0, noBytesProcessed);
        outputStream.flush();

        inputStream.close();
        outputStream.close();
    }
}