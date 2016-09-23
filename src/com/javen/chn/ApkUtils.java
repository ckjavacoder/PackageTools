package com.javen.chn;


import brut.androlib.AndrolibException;
import brut.androlib.res.AndrolibResources;
import brut.common.BrutException;
import brut.util.Jar;
import brut.util.OSDetection;
import com.android.sdklib.internal.build.SignedJarBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;


/**
 * APK工具类
 * <p>
 * Created by coder on 16/9/20.
 */
public class ApkUtils {


    /**
     * 添加渠道信息到APk文件中
     *
     * @param apkFile
     * @param chn
     * @param chn2
     */
    public static void addChnInfoToApk(File apkFile, String chn, String chn2) throws IOException, AndrolibException, InterruptedException {
        File cn = File.createTempFile(UUID.randomUUID().toString(), ".chndata");
        File cn2 = File.createTempFile(UUID.randomUUID().toString(), ".chndata");
        FileUtils.writeStringToFile(cn, chn);
        FileUtils.writeStringToFile(cn2, chn2);
        insertFileToApk(apkFile, "assets/cn", cn);
        insertFileToApk(apkFile, "assets/cn2", cn2);
    }

    /**
     * 添加闪屏Logo到Apk中
     *
     * @param apkFile
     * @param logoFile
     */
    public static void addChnLogoToApk(File apkFile, File logoFile) throws InterruptedException, IOException, AndrolibException {
        insertFileToApk(apkFile, "assets/channel_ad.png", logoFile);
    }

    /**
     * 添加文件到APK
     *
     * @param apkFile
     * @param targetPath
     * @param dataFile
     */
    public static void insertFileToApk(File apkFile, String targetPath, File dataFile) throws IOException, AndrolibException, InterruptedException {
        File rootDir = new File("temp");
        FileUtils.forceDeleteOnExit(rootDir);
        File workDir = new File(rootDir, UUID.randomUUID().toString());
        File insertFile = new File(workDir, targetPath);
        FileUtils.copyFile(dataFile, insertFile);
        //执行AAPT命令添加
        AndrolibResources androlibResources = new AndrolibResources();
        File aaptBinaryFile = androlibResources.getAaptBinaryFile();
        List<String> cmd = new ArrayList<String>();
        cmd.add(aaptBinaryFile.getAbsolutePath());
        cmd.add("a");
        cmd.add(apkFile.getAbsolutePath());
        cmd.add(targetPath);

        Logger.logD("InsertFileToApk args:" + Arrays.toString(cmd.toArray()));

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(workDir);
        Process ps = pb.start();
        InputStream inputStream = ps.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        String line = null;
        while ((line = br.readLine()) != null) {
            Logger.logD(line);
        }
        ps.waitFor();
        FileUtils.forceDeleteOnExit(workDir);
    }

    public static String SIGN_FILE = "com/javen/chn/codesign.keystore";
    public static String SIGN_ALIAS = "Owen";
    public static String SIGN_PWD = "killers8Y";

    /**
     * 对APk进行签名
     */
    public static void signApk(File apkFile, File signedApk) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        InputStream resourceAsStream = ApkUtils.class.getClassLoader().getResourceAsStream("com/javen/chn/codesign.keystore");
        keyStore.load(resourceAsStream,
                SIGN_PWD.toCharArray());
        KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(
                SIGN_ALIAS,
                new KeyStore.PasswordProtection(SIGN_PWD.toCharArray()));
        SignedJarBuilder sb = new SignedJarBuilder(new FileOutputStream(
                signedApk), entry.getPrivateKey(),
                (X509Certificate) entry.getCertificate());
        sb.writeZip(new FileInputStream(apkFile), new SignedJarBuilder.IZipEntryFilter() {
            public boolean checkEntry(String arg0) throws SignedJarBuilder.IZipEntryFilter.ZipAbortException {
                return true;
            }
        });
        FileUtils.forceDeleteOnExit(apkFile);
        sb.close();
        sb.cleanUp();
    }


    /**
     * zip对齐
     *
     * @param singedFile
     * @param alignedFile
     */
    public static void zipAlign(File singedFile, File alignedFile) throws Exception {
        File zipAlignFile = getZipAlignFile();
        List<String> cmd = new ArrayList<String>();
        cmd.add(zipAlignFile.getAbsolutePath());
        cmd.add("-v");
        cmd.add("4");
        cmd.add(singedFile.getAbsolutePath());
        cmd.add(alignedFile.getAbsolutePath());
        FileUtils.forceDeleteOnExit(singedFile);
        ProcessBuilder pb = new ProcessBuilder(cmd);
        Process ps = pb.start();
        String line = null;
        InputStream inputStream = ps.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        while ((line = br.readLine()) != null) {
            Logger.logD(line);
        }
        InputStream errorStream = ps.getErrorStream();
        String s = IOUtils.toString(errorStream);
        if (!StringUtils.isEmpty(s)) {
            System.out.println("Error:" + s);
            throw new RuntimeException("Align:" + s);
        }
        int retValue = ps.waitFor();
        if (retValue != 0) {
            throw new RuntimeException("zipAlign Failed");
        }
    }

    /**
     * 获取ZipAlign文件
     *
     * @return
     * @throws AndrolibException
     */
    private static File getZipAlignFile() throws AndrolibException {
        File aaptBinary;
        try {
            if (OSDetection.isMacOSX()) {
                aaptBinary = Jar.getResourceAsFile("/prebuilt/zipalign/macosx/zipalign");
            } else if (OSDetection.isUnix()) {
                aaptBinary = Jar.getResourceAsFile("/prebuilt/zipalign/linux/zipalign");
            } else {
                if (!OSDetection.isWindows()) {
                    System.err.println("Unknown Operating System: " + OSDetection.returnOS());
                    return null;
                }

                aaptBinary = Jar.getResourceAsFile("/prebuilt/zipalign/windows/zipalign.exe");
            }
        } catch (BrutException var3) {
            throw new AndrolibException(var3);
        }

        if (aaptBinary.setExecutable(true)) {
            return aaptBinary;
        } else {
            System.err.println("Can\'t set aapt binary as executable");
            throw new AndrolibException("Can\'t set aapt binary as executable");
        }
    }

}
