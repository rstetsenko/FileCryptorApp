package com.rst;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.spec.KeySpec;

public class Main {

    private static final String VERSION_NAME = "1.0.0";

    private static final String ENCRYPT = "-e";
    private static final String DECRYPT = "-d";
    private static final String VERSION = "-v";

    private static final String HELP_DESCRIPTION = "Help: \n" + VERSION + " to get app version;\nUsage:" +
            "\nFirst parameter: " + ENCRYPT + " (encrypt) or " + DECRYPT +
            " (decrypt);\nSecond parameter: your password;\nThird parameter: path to file." +
            "\n\nExample: \n>java -jar JNCryptorApp.jar -e MyPassword path/to/file/example.txt" +
            "\n\nResulted file will be created at the same directory as the source file";

    private static final int KEY_LENGTH = 256;
    private static final int ITERATION_COUNT = 10000;
    /** Salt length is equals to {@link #KEY_LENGTH} / 8 */
    private static final byte[] SALT = {67, -12, -59, -78, 98, -43, 35, -107, 104, 117, 33, 24, -28, -113, 38, -39, -36, -94, 72, -67, -94, 4, 101, -52, 75, 39, -32, 100, -53, -3, -47, -10};
    private static final byte[] IV = {83, -27, 56, -1, 15, 65, -26, 80, -12, 121, -20, 98, -14, -95, 64, 31};

    private static String userPassword;

    private static Cipher encryptCipher;
    private static Cipher decryptCipher;
    private static SecretKey key;

    public static void main(String[] args) {
        try {
            if (args.length == 1 && args[0].equals(VERSION)) {
                System.out.println("Current version: " + VERSION_NAME);
            } else if (args.length == 3) {
                File file = getFileFromPath(args[2]);
                userPassword = args[1];
                if (userPassword == null || userPassword.length() < 1) {
                    System.out.println("Password is empty!");
                    return;
                }
                if (file != null) {
                    if (args[0].equals(ENCRYPT)) {
                        encryptFile(file);
                        System.out.println("Success, file encrypted!");
                    } else if (args[0].equals(DECRYPT)) {
                        decryptFile(file);
                        System.out.println("Success, file decrypted!");
                    }
                }
            } else {
                System.out.println(HELP_DESCRIPTION);
            }
        } catch (Exception e) {
            printException(e);
        }
    }

    private static void encryptFile(File fileWithData) throws IOException {
        File encryptedFile = createEncryptedFile(fileWithData);
        byte[] bytes = getBytesFromFile(fileWithData);

        CipherOutputStream cipherOutputStream = null;
        try {
            cipherOutputStream = new CipherOutputStream(new FileOutputStream(encryptedFile), getEncryptCipher());
            cipherOutputStream.write(bytes);
        } finally {
            safeClose(cipherOutputStream);
        }
    }

    private static void decryptFile(File encryptedFile) throws IOException {
        File decryptedFile = createDecryptedFile(encryptedFile);

        CipherInputStream cis = null;
        ByteArrayOutputStream out = null;
        FileOutputStream fos = null;
        try {
            cis = new CipherInputStream(new FileInputStream(encryptedFile), getDecryptCipher());
            out = new ByteArrayOutputStream();

            int read;
            byte[] buffer = new byte[1024];
            while ((read = cis.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            fos = new FileOutputStream(decryptedFile);
            fos.write(out.toByteArray());
        } finally {
            safeClose(cis);
            safeClose(fos);
            safeClose(out);
        }
    }

    private static File getFileFromPath(String pathToFile) {
        File file = new File(pathToFile);
        if (file.exists()) {
            if (file.isDirectory()) {
                System.out.println("This is a directory, provide path to file");
            } else {
                return file;
            }
        } else {
            System.out.println("File does not exist");
        }
        return null;
    }

    private static byte[] getBytesFromFile(File file) throws IOException {
        Path path = Paths.get(file.getPath());
        return Files.readAllBytes(path);
    }

    private static void bytesToFile(byte[] bytes, File file) throws IOException {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            fos.write(bytes);
        } finally {
            safeClose(fos);
        }
    }

    private static File createEncryptedFile(File sourceFile) {
        String name = sourceFile.getName();
        String dir = sourceFile.getParent();
        File encryptedFile = new File(dir, name + "ENC");
        encryptedFile.delete();
        return encryptedFile;
    }

    private static File createDecryptedFile(File file) {
        String name = file.getName();
        name = name.replaceAll("ENC$", "");
        String dir = file.getParent();
        File decryptedFile = new File(dir, name);
        decryptedFile.delete();
        return decryptedFile;
    }

    private static void safeClose(Closeable closable) {
        if (closable != null) {
            try {
                closable.close();
            } catch (IOException e) {
                printException(e);
            }
        }
    }

    private static void printException(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        System.out.println(sw.toString());
    }

    private static SecretKey getKey() {
        if (key == null) {
            try {
                KeySpec keySpec = new PBEKeySpec(userPassword.toCharArray(), SALT, ITERATION_COUNT, KEY_LENGTH);
                SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
                byte[] keyBytes = keyFactory.generateSecret(keySpec).getEncoded();
                key = new SecretKeySpec(keyBytes, "AES");
            } catch (Exception e) {
                printException(e);
            }
        }
        return key;
    }

    private static Cipher getCipher(int cipherMode) {
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            IvParameterSpec ivParams = new IvParameterSpec(IV);
            cipher.init(cipherMode, getKey(), ivParams);
        } catch (Exception e) {
            printException(e);
        }
        return cipher;
    }

    private static synchronized Cipher getEncryptCipher() {
        if (encryptCipher == null) {
            encryptCipher = getCipher(Cipher.ENCRYPT_MODE);
        }
        return encryptCipher;
    }

    private static synchronized Cipher getDecryptCipher() {
        if (decryptCipher == null) {
            decryptCipher = getCipher(Cipher.DECRYPT_MODE);
        }
        return decryptCipher;
    }
}
