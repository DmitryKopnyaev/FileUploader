package com.dimentor.fileUploader.util;


public class VersionUtil {

    public static boolean nameContainVersion(String name) {
        return name.matches(".+\\(\\d+\\)\\..+");
    }

    public static int getVersion(String name) {
        int v = 1;
        if (nameContainVersion(name)) {
            String s = name.replaceAll("\\)\\..+", "");
            s = s.replaceAll(".+\\(", "");
            try {
                v = Integer.parseInt(s);
            } catch (NumberFormatException e) {
                return v;
            }
        }
        return v;
    }

    public static String getFileNameWithVersion(String name, int v) {
        String[] split = name.split("\\.");
        String suffix = "." + split[split.length - 1];
        int i = name.lastIndexOf(suffix);
        String body = name.substring(0, i);
        if (nameContainVersion(name)) {
            int j = body.lastIndexOf("(");
            body = name.substring(0, j);
        }
        return body + (v > 1 ? "(" + v + ")" : "") + suffix;
    }

    public static String getClearFileName(String name){
        if(nameContainVersion(name)){
            String[] split = name.split("\\.");
            String suffix = "." + split[split.length - 1];
            int i = name.lastIndexOf("(");
            return name.substring(0, i) + suffix;
        }
        return name;
    }
}
