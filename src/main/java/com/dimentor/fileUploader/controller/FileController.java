package com.dimentor.fileUploader.controller;

import com.dimentor.fileUploader.util.Constants;
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

@RestController
@RequestMapping("/file")
public class FileController {

    @GetMapping(value = "/obj", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public @ResponseBody byte[] getUploadingFileByUri(@RequestParam String uri) throws IOException {
        try (BufferedInputStream stream = new BufferedInputStream(new FileInputStream(new File(Constants.SRC_URL, uri)))) {
            return stream.readAllBytes();
        }
    }

    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE}, produces = "application/json")
    public ResponseEntity addFile(@RequestParam String uri, @RequestPart MultipartFile fileFromClient, @RequestParam String filename) {
        if (fileFromClient == null)
            return new ResponseEntity(null, HttpStatus.BAD_REQUEST);
        File dirOnServer = new File(Constants.SRC_URL, uri);
        if (!dirOnServer.exists())
            return new ResponseEntity("this uri directory not found", HttpStatus.BAD_REQUEST);
        File fileOnServer = new File(dirOnServer, filename);
        try {
            fileFromClient.transferTo(fileOnServer);
        } catch (IOException e) {
            return new ResponseEntity(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity(fileOnServer, HttpStatus.OK);
    }

    @PostMapping(value = "dir", produces = "application/json")
    public ResponseEntity addDirectory(@RequestParam String uri) {
        File file = new File(Constants.SRC_URL, uri);
        if (file.exists())
            return new ResponseEntity("such a directory already exists", HttpStatus.BAD_REQUEST);
        if (file.mkdirs())
            return new ResponseEntity(file, HttpStatus.OK);
        else
            return new ResponseEntity("directory not created", HttpStatus.BAD_REQUEST);
    }

    //Если ничего не передать в параметр или передать /, то вернет список корневой директории
    @GetMapping(value = "/list", produces = "application/json")
    public ResponseEntity getListFilesByDirUri(@RequestParam String uri) {
        File file = new File(Constants.SRC_URL, uri);
        if (!file.exists())
            return new ResponseEntity("dir not found", HttpStatus.BAD_REQUEST);
        if (file.isFile())
            return new ResponseEntity("this is not directory uri", HttpStatus.BAD_REQUEST);
        String[] list = file.list();

        return new ResponseEntity(list, HttpStatus.OK);
    }

    @GetMapping(value = "/tree", produces = "application/json")
    public ResponseEntity getTreeFilesByDirUri(@RequestParam String uri) {
        File fileSrc = new File(Constants.SRC_URL, uri);
        if (!fileSrc.exists())
            return new ResponseEntity("dir not found", HttpStatus.BAD_REQUEST);
        if (fileSrc.isFile())
            return new ResponseEntity("this is not directory uri", HttpStatus.BAD_REQUEST);

        List<String> list = new LinkedList<>();
        try {
            Files.walkFileTree(fileSrc.toPath(), new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    list.add(Path.of(Constants.SRC_URL).relativize(dir).toString());
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    list.add(Path.of(Constants.SRC_URL).relativize(file).toString());
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

    @GetMapping(value = "/files", produces = "application/json")
    public ResponseEntity getFilesByDirUri(@RequestParam String uri) {
        File file = new File(Constants.SRC_URL, uri);
        if (!file.exists())
            return new ResponseEntity("dir not found", HttpStatus.BAD_REQUEST);
        if (file.isFile())
            return new ResponseEntity("this is not directory uri", HttpStatus.BAD_REQUEST);
        File[] files = file.listFiles();
        return new ResponseEntity(files, HttpStatus.OK);
    }

    @GetMapping(value = "/hex", produces = "application/json")
    public ResponseEntity getFileByHex(@RequestParam String diruri, @RequestParam String hex) {
        if (hex.isEmpty())
            return new ResponseEntity("incorrect hex", HttpStatus.BAD_REQUEST);
        File file = new File(Constants.SRC_URL, diruri);
        if (!file.exists())
            return new ResponseEntity("file not found", HttpStatus.BAD_REQUEST);
        File[] files = file.listFiles();
        for (File f : files) {
            try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(f.toString()))) {
                String fHex = DigestUtils.md5Hex(in);
                if (fHex.equalsIgnoreCase(hex))
                    return new ResponseEntity(f, HttpStatus.OK);
            } catch (IOException e) {
                return new ResponseEntity(e.getMessage(), HttpStatus.BAD_REQUEST);
            }
        }
        return new ResponseEntity(null, HttpStatus.BAD_REQUEST);
    }

    @GetMapping(value = "/byhex", produces = "application/json")
    public ResponseEntity getHexByFile(@RequestParam String uri) {
        File file = new File(Constants.SRC_URL, uri);
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
    public ResponseEntity deleteDirByUri(@RequestParam String uri) {
        File file = new File(Constants.SRC_URL, uri);
        if (!file.exists())
            return new ResponseEntity("file not found", HttpStatus.BAD_REQUEST);
        try {
            FileUtils.deleteDirectory(file);
            return new ResponseEntity(file, HttpStatus.OK);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return new ResponseEntity(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}