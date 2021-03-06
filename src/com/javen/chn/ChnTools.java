package com.javen.chn;

import com.sun.deploy.net.URLEncoder;
import com.sun.xml.internal.stream.buffer.sax.Properties;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.*;
import java.net.URLDecoder;
import java.util.*;

public class ChnTools {

    @Option(name = "-apk", usage = "need to process template apkfile")
    private String inApk;

    @Option(name = "-base")
    private String base = "";

    @Option(name = "-apkpath", usage = "spec apk version path")
    private String inApkPath;

    @Option(name = "-iconPath")
    private String iconPath;

    @Option(name = "-provider", usage = "provider wnl simple default wnl")
    private String provider = "wnl";

    @Option(name = "-chnlist", usage = "chn batch list file")
    private String chnFile;

    @Option(name = "-chndata", usage = "channel data")
    private String chn;

    @Option(name = "-out", usage = "spec to outapk")
    private String outApk;

    @Option(name = "-splash", usage = "png file to show first screen", required = false)
    private String logo;

    /**
     * 用于处理参数
     *
     * @param args
     */
    public static void main(String[] args) throws Exception {
        new ChnTools().doMain(args);
    }


    /**
     * 实际的处理入口
     *
     * @param args
     */
    public void doMain(String[] args) throws Exception {
        CmdLineParser parser = new CmdLineParser(this);
        parser.parseArgument(args);

        File apkpath = new File(base + inApkPath);
        if (apkpath.exists() && apkpath.isDirectory()) {
            File chnList = new File(chnFile);
            if (chnList.exists() && chnList.isFile() && chnList.canRead()) {
                handleBatchChannel(apkpath, chnList);
                return;
            }
            File chnList2 = new File(chn);
            if (chnList2.exists() && chnList2.isFile() && chnList2.canRead()) {
                handleBatchChannel(apkpath, chnList2);
                return;
            }
        }

        throw new RuntimeException("   不知道你要干嘛");
    }


    static HashMap<String, String> mapFile = new HashMap<String, String>();

    static {
        mapFile.put("通用", "release-万年历-所有广告-");
        mapFile.put("通用-日历", "release-万年历日历-所有广告-");
        mapFile.put("广点通", "release-万年历-广点通广告-");
        mapFile.put("广点通-日历", "release-万年历日历-广点通广告-");
        mapFile.put("无广告", "release-万年历-无广告-");
        mapFile.put("无广告-日历", "release-万年历日历-无广告-");
        mapFile.put("天气换量", "release-万年历-天气渠道换量-所有广告-");
        mapFile.put("CM市场", "release-万年历-手助市场-所有广告-");
        mapFile.put("小米预装", "release-日历-小米手机预装-所有广告-换图标-");
        mapFile.put("乐视预装", "release-日历-乐视手机预装-所有广告-换图标-");
        mapFile.put("联想", "release-万年历-联想市场-所有广告-");
        try {
            java.util.Properties properties = new java.util.Properties();
            String path = ChnTools.class.getProtectionDomain().getCodeSource().getLocation().getFile();
            path = URLDecoder.decode(path, "utf-8");
            System.out.println("Path:" + path);
            properties.load(new FileInputStream(new File(new File(path).getParentFile(), "a.prop")));
            Set<Map.Entry<Object, Object>> entries = properties.entrySet();
            for (Map.Entry<Object, Object> entry : entries) {
                mapFile.put(new String(entry.getKey().toString().getBytes("ISO-8859-1"),"UTF-8"), new String(entry.getValue().toString().getBytes("ISO-8859-1"),"UTF-8"));
                System.out.println("add:" + new String(entry.getKey().toString().getBytes("ISO-8859-1"),"UTF-8") + ":" + new String(entry.getValue().toString().getBytes("ISO-8859-1"),"UTF-8"));
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void handleBatchChannelData(File apkpath, String chn) throws Exception {
        String[] lines = chn.split("\r\n");
        HashMap<String, ArrayList<String>> pkMap = new HashMap<String, ArrayList<String>>();
        ArrayList<String> sectionList = null;
        for (String line : lines) {
            if (line.startsWith("/")) {
                continue;
            }
            if (StringUtils.isEmpty(line)) {
                continue;
            }
            sectionList = handleLine(pkMap, sectionList, line);
        }
        HashMap<String, File> apkFileMap = new HashMap<String, File>();
        findAllApk(pkMap.keySet(), apkFileMap, apkpath);

        for (String s : apkFileMap.keySet()) {
            ArrayList<String> strings = pkMap.get(s);
            for (String string : strings) {
                genChannel(apkFileMap.get(s), s, string);
            }
        }
    }

    private ArrayList<String> handleLine(HashMap<String, ArrayList<String>> pkMap, ArrayList<String> sectionList, String line) {
        if (line.trim().startsWith("[")) {
            String section = line.replaceAll("\\[", "").replaceAll("]", "");
            sectionList = new ArrayList<String>();
            pkMap.put(section, sectionList);
        } else {
            sectionList.add(line);
        }
        return sectionList;
    }

    /**
     * 处理批量渠道打包
     *
     * @param apkpath
     * @param chnList
     */
    private void handleBatchChannel(File apkpath, File chnList) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(chnList)));
        String line = null;
        HashMap<String, ArrayList<String>> pkMap = new HashMap<String, ArrayList<String>>();
        ArrayList<String> sectionList = null;
        while ((line = br.readLine()) != null) {
            if (line.startsWith("/")) {
                continue;
            }
            if (StringUtils.isEmpty(line)) {
                continue;
            }
            sectionList = handleLine(pkMap, sectionList, line);
        }
        HashMap<String, File> apkFileMap = new HashMap<String, File>();
        findAllApk(pkMap.keySet(), apkFileMap, apkpath);

        for (String s : apkFileMap.keySet()) {
            ArrayList<String> strings = pkMap.get(s);
            for (String string : strings) {
                genChannel(apkFileMap.get(s), s, string);
            }
        }
    }


    /**
     * 生成渠道号
     *
     * @param file
     * @param s
     * @param chnData
     */
    private void genChannel(File file, String s, String chnData) throws Exception {
        File logoFile = null;
        if (chnData.contains(",")) {
            String[] parts = chnData.split("[,]+");
            if (parts.length == 2) {
                chnData = parts[0];
                if (!StringUtils.isEmpty(parts[1])) {
                    if (StringUtils.isEmpty(iconPath)) {
                        this.iconPath = base;
                    }
                    logoFile = new File(new File(iconPath), "logo/" + parts[1]);
                    if (!logoFile.exists()) {
                        logoFile = null;
                    }
                }
            }
        }
        File rootDir = new File("output");
        File outDir = new File(rootDir, s);
        outDir.mkdirs();

        System.out.println("处理渠道:" + chnData);

        File outFile = new File(outDir, file.getName().replace(".apk", "") + "_" + chnData + ".apk_unaligned");

        File retFile = new File(outDir, file.getName().replace(".apk", "") + "_" + chnData + ".apk");

        File outFile2 = new File(outFile.getAbsolutePath() + "_");
        if (!outFile.getParentFile().exists()) {
            outFile.getParentFile().mkdirs();
        }
        FileUtils.forceDeleteOnExit(outFile2);
        FileUtils.copyFile(file, outFile2);
        HashMap<String, File> stringFileHashMap = new HashMap<>();
        if ("wnl".equals(provider)) {

            File cn = File.createTempFile(UUID.randomUUID().toString(), ".chndata");
            File cn2 = File.createTempFile(UUID.randomUUID().toString(), ".chndata");
            FileUtils.writeStringToFile(cn, chnData);
            FileUtils.writeStringToFile(cn2, "0");

            stringFileHashMap.put("assets/cn", cn);
            stringFileHashMap.put("assets/cn2", cn2);
            if (logoFile != null && logoFile.exists() && logoFile.canRead()) {
                stringFileHashMap.put("assets/channel_ad.png", logoFile);
            }
            ApkUtils.signApk(outFile2, outFile, stringFileHashMap);
            ApkUtils.zipAlign(outFile, retFile);

        } else if ("simple".equals(provider)) {
            MCPTool.write(outFile2, chnData, "wnl.channel");
            if (logoFile != null && logoFile.exists() && logoFile.canRead()) {
                stringFileHashMap.put("assets/splash_logo.png", logoFile);
                ApkUtils.signApk(outFile2, outFile, stringFileHashMap);
                ApkUtils.zipAlign(outFile, retFile);
            } else {
                FileUtils.moveFile(outFile2, retFile);
            }
        } else {
            throw new RuntimeException("unknow provider for package channel!");
        }

        /**
         * 如果添加了闪屏都需要签名和ZipAlign
         *
         *
         *
         * 如果不添加的话则简版不需要ZipAlign
         */

    }


    /**
     * 查询所有Apk
     *
     * @param strings
     * @param apkFileMap
     * @param apkpath
     */
    private void findAllApk(Set<String> strings, HashMap<String, File> apkFileMap, File apkpath) {
        for (String string : strings) {
            final String s = mapFile.get(string);
            if (!mapFile.containsKey(string)) {//如果不存在这个预置组
                File apkFile = new File(apkpath, string);
                if (apkFile.exists() && apkFile.getAbsolutePath().endsWith(".apk")) {
                    apkFileMap.put(string, apkFile);
                }
                continue;
            }
            File[] apkResult = apkpath.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    if (!StringUtils.isEmpty(name) && name.endsWith(".apk")) {
                        return name.startsWith(s);
                    }
                    return false;
                }
            });
            if (apkResult != null && apkResult.length == 1) {
                apkFileMap.put(string, apkResult[0]);
            }
        }

    }


}
