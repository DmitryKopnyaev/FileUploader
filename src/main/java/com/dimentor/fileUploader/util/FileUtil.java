package com.dimentor.fileUploader.util;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileUtil {

    public static String getHex(File file) throws IOException {
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file.toString()))) {
            return DigestUtils.md5Hex(bis);
        }
    }

    //на сервере не может быть ЗИП файлов
    //по имени папки на сервере передаем её структуру на клиент
    public static Map<String, List<MetaFile>> getDirStructure(String dir) throws IOException {
        //dir абсолютный путь на сервере
        HashMap<String, List<MetaFile>> map = new HashMap<>();
        Path dirPath = Path.of(dir);

        Files.walkFileTree(dirPath, new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path relativize = dirPath.relativize(file.getParent());
                map.putIfAbsent(relativize.toString(), new ArrayList<MetaFile>());
                String clearName = VersionUtil.getClearFileName(file.getFileName().toString());
                int version = VersionUtil.getVersion(file.getFileName().toString());
                String hex = FileUtil.getHex(file.toFile());
                map.get(relativize.toString()).add(new MetaFile(clearName, version, hex));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }
        });

        return map;
    }
}
