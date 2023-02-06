package com.dimentor.fileUploader.controller;

import com.dimentor.fileUploader.util.Constants;
import com.dimentor.fileUploader.util.FileUtil;
import com.dimentor.fileUploader.util.MetaFile;
import net.lingala.zip4j.ZipFile;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/file")
public class FileController {

    //передача файла на клиент
    @GetMapping(value = "/upload", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public @ResponseBody byte[] getUploadingFileByUri(@RequestParam String uri) throws IOException {
        try (BufferedInputStream stream = new BufferedInputStream(new FileInputStream(new File(Constants.SRC_URI_ABS, uri)))) {
            return stream.readAllBytes();
        }
    }

    //получение файла(ов) на сервер
    @PostMapping(value = "/load",consumes = {MediaType.MULTIPART_FORM_DATA_VALUE}, produces = "application/json")
    public ResponseEntity addFile(@RequestParam String uri, @RequestPart MultipartFile fileFromClient, @RequestParam String filename) {
        if (fileFromClient == null)
            return new ResponseEntity(null, HttpStatus.BAD_REQUEST);
        File dirOnServer = new File(Constants.SRC_URI_ABS, uri);
        if (!dirOnServer.exists())
            return new ResponseEntity("this uri directory not found", HttpStatus.BAD_REQUEST);
        File fileOnServer = new File(dirOnServer, filename);
        try {
            fileFromClient.transferTo(fileOnServer);

            if (filename.endsWith(".zip")) {
                try (ZipFile zipFile = new ZipFile(fileOnServer)) {
                    zipFile.extractAll(String.valueOf(dirOnServer));
                    fileOnServer.delete();
                }
            }
        } catch (IOException e) {
            return new ResponseEntity(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity(fileOnServer, HttpStatus.OK);
    }

    //создание директории на сервере по uri
    @PostMapping(value = "/dir", produces = "application/json")
    public ResponseEntity addDirectory(@RequestParam String uri) {
        File file = new File(Constants.SRC_URI_ABS, uri);
        if (file.exists())
            return new ResponseEntity("such a directory already exists", HttpStatus.BAD_REQUEST);
        if (file.mkdirs())
            return new ResponseEntity(file, HttpStatus.OK);
        else
            return new ResponseEntity("directory not created", HttpStatus.BAD_REQUEST);
    }

    //список имен файлов на сервере по uri
    @GetMapping(value = "/list", produces = "application/json")
    public ResponseEntity getListFilesByDirUri(@RequestParam String uri) {
        File file = new File(Constants.SRC_URI_ABS, uri);
        if (!file.exists())
            return new ResponseEntity("dir not found", HttpStatus.BAD_REQUEST);
        if (file.isFile())
            return new ResponseEntity("this is not directory uri", HttpStatus.BAD_REQUEST);
        String[] list = file.list();

        return new ResponseEntity(list, HttpStatus.OK);
    }

    @GetMapping(value = "/tree", produces = "application/json")
    public ResponseEntity getTreeFilesByDirUri(@RequestParam String uri) {
        File fileSrc = new File(Constants.SRC_URI_ABS, uri);
        if (!fileSrc.exists())
            return new ResponseEntity("dir not found", HttpStatus.BAD_REQUEST);
        if (fileSrc.isFile())
            return new ResponseEntity("this is not directory uri", HttpStatus.BAD_REQUEST);

        List<String> list = new LinkedList<>();
        try {
            Files.walkFileTree(fileSrc.toPath(), new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    list.add(Path.of(Constants.SRC_URI_ABS).relativize(dir).toString());
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    list.add(Path.of(Constants.SRC_URI_ABS).relativize(file).toString());
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
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ResponseEntity(list, HttpStatus.OK);
    }

    @GetMapping(value = "/hex", produces = "application/json")
    public ResponseEntity getHexByFile(@RequestParam String uri) {
        File file = new File(Constants.SRC_URI_ABS, uri);
        if (!file.exists())
            return new ResponseEntity("file not found", HttpStatus.BAD_REQUEST);
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file.toString()))) {
            String fHex = DigestUtils.md5Hex(in);
            return new ResponseEntity(fHex, HttpStatus.OK);
        } catch (IOException e) {
            return new ResponseEntity(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping(produces = "application/json")
    public ResponseEntity deleteByUri(@RequestParam String uri) {
        File file = new File(Constants.SRC_URI_ABS, uri);
        if (!file.exists())
            return new ResponseEntity("file not found", HttpStatus.BAD_REQUEST);
        try {
            if (file.isDirectory())
                FileUtils.deleteDirectory(file);
            else file.delete();
            return new ResponseEntity(file, HttpStatus.OK);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return new ResponseEntity(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping(value = "/structure", produces = "application/json")
    public ResponseEntity getDirStructure(@RequestParam String uri) {
        String uriOnServer = Constants.SRC_URI_ABS + "\\" + uri;
        if (!new File(uriOnServer).exists()) {
            return new ResponseEntity("This directory didn't find", HttpStatus.BAD_REQUEST);
        }
        try {
            Map<String, List<MetaFile>> map = FileUtil.getDirStructure(uriOnServer);
            return new ResponseEntity(map, HttpStatus.OK);
        } catch (IOException e) {
            return new ResponseEntity(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}