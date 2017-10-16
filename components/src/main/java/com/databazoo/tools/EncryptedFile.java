package com.databazoo.tools;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.ShortBufferException;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.bouncycastle.crypto.InvalidCipherTextException;

/**
 * Properties implementation that writes and reads encrypted strings. Used for storing project configurations including passwords.
 * @author bobus
 */
public class EncryptedFile {
    private final BouncyCastleAESwithCBC cbcHandler;

    public static InputStream asInputStream(File file, String password) throws EncryptedFileException {
        try {
            return new EncryptedFile(password).asInputStream(file);

        } catch (InvalidCipherTextException e) {
            throw new EncryptedFilePasswordException(e.getMessage(), e);

        } catch (IOException | BadPaddingException | ShortBufferException | IllegalBlockSizeException e) {
            throw new EncryptedFileException(e.getMessage(), e);
        }
    }

    public static StreamResult asStreamResult(File file, String password) throws EncryptedFileException {
        try {
            return new StreamResult(new EncryptedFile(password).getEncryptedOutputStream(file));

        } catch (IOException | BadPaddingException | IllegalBlockSizeException e) {
            throw new EncryptedFileException(e.getMessage(), e);
        }
    }

    private EncryptedFile(String password) {
        cbcHandler = new BouncyCastleAESwithCBC(password);
    }

    private InputStream asInputStream(File file) throws IOException, InvalidCipherTextException, BadPaddingException, ShortBufferException, IllegalBlockSizeException {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        cbcHandler.initCiphers();
        cbcHandler.cbcDecrypt(new FileInputStream(file), outputStream);
        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    private EncryptedOutputStream getEncryptedOutputStream(File file) throws IOException, BadPaddingException, IllegalBlockSizeException {
        return new EncryptedOutputStream(file);
    }

    public static class EncryptedFilePasswordException extends EncryptedFileException {
        EncryptedFilePasswordException(String message, Exception e) {
            super(message, e);
        }
    }

    public static class EncryptedFileException extends Exception {
        EncryptedFileException(String message, Exception e) {
            super(message, e);
        }
    }

    private class EncryptedOutputStream extends OutputStream {

        private final StringBuilder sb = new StringBuilder();
        private final File file;
        private boolean isFlushed = false;
        private boolean isClosed = false;

        EncryptedOutputStream(File file) {
            this.file = file;
        }

        @Override
        public void write(int b) throws IOException {
            if(isClosed){
                throw new IllegalStateException("Stream already closed");
            }
            sb.append((char)b);
        }

        @Override
        public void flush() throws IOException {
            if(isClosed){
                throw new IllegalStateException("Stream already closed");
            }
            try {
                cbcHandler.initCiphers();
                cbcHandler.cbcEncrypt(new ByteArrayInputStream(sb.toString().getBytes()), new FileOutputStream(file));
            } catch (Exception e) {
                throw new IOException(e);
            }
            isFlushed = true;
        }

        @Override
        public void close() throws IOException {
            if(!isClosed) {
                if(!isFlushed){
                    flush();
                }
                isClosed = true;
            }
        }
    }
}
