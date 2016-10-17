package com.javen;

import java.io.FileInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Created by coder on 2016/9/29.
 */
public class FF {
    public static void main(String[] args) throws Exception {
        ZipInputStream zis = new ZipInputStream(new FileInputStream("/Users/coder/jenkins/ChnTools/output/通用/release-万年历-所有广告-4.5.1-92_10002.apk_unaligned_"));
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
            System.out.println(entry.getName());
        }

        ZipFile zipFile = new ZipFile("/Users/coder/jenkins/ChnTools/output/通用/release-万年历-所有广告-4.5.1-92_10002.apk_unaligned_");
        ZipEntry entry1 = zipFile.getEntry("assets/cn");

    }
}
