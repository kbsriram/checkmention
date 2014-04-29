package com.kbsriram.checkmention.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public final class CUtils
{
    public final static String nullIfEmpty(String s)
    {
        if ((s == null) || (s.length() == 0)) {
            return null;
        }
        return s;
    }

    public final static void copy(InputStream in, OutputStream out)
        throws IOException
    {
        byte buf[] = new byte[8192];
        int nread;
        while ((nread = in.read(buf)) > 0) {
            out.write(buf, 0, nread);
        }
    }

    public static String makeNonce()
    {
        byte[] buf = new byte[12];
        synchronized (s_random) {
            s_random.nextBytes(buf);
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            return byte2str(md.digest(buf));
        }
        catch (NoSuchAlgorithmException nse) {
            throw new RuntimeException(nse);
        }
    }

    public static <T> String makeLogTag(Class<T> cls)
    {
        String str = cls.getSimpleName();
        if (str.length() > MAX_LOG_TAG_LENGTH - LOG_PREFIX_LENGTH) {
            return LOG_PREFIX + str.substring
                (0, MAX_LOG_TAG_LENGTH - LOG_PREFIX_LENGTH - 1);
        }
        return LOG_PREFIX + str;
    }

    public final static String byte2str(byte[] bytes)
    {
        StringBuilder sb = new StringBuilder();
        for (byte b: bytes) {
            int cur = (b & 0xff);
            if (cur <= 0xf) {
                sb.append("0");
            }
            sb.append(Integer.toHexString(cur));
        }
        return sb.toString();
    }

    private static final String LOG_PREFIX = "opg_";
    private static final int LOG_PREFIX_LENGTH = LOG_PREFIX.length();
    private static final int MAX_LOG_TAG_LENGTH = 23;
    private final static SecureRandom s_random = new SecureRandom();
}
