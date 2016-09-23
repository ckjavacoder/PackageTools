package com.javen.chn;

import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 日志记录类
 * Created by coder on 16/9/20.
 */
public class Logger {

    public static void logD(String msg, Object... args) {
        log("DEBUG", String.format(msg, args));
    }

    public static void logE(String msg, Object... args) {
        log("ERROR", String.format(msg, args));
    }

    /**
     * 记录日志
     *
     * @param tag
     * @param format
     */
    private static void log(String tag, String format) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream("chn.log", true);
            fos.write(("[" + tag + "] " + format).getBytes("utf-8"));
            fos.write("\r\n".getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
