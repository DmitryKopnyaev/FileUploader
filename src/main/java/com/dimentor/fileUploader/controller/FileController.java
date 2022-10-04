package com.dimentor.fileUploader.controller;

import com.dimentor.fileUploader.util.Constants;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

@RestController
@RequestMapping("/file")
public class FileController {

    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE}, produces = "application/json")
    public ResponseEntity addFile(@RequestParam String uri, @RequestPart MultipartFile fileFromClient) {
        if (fileFromClient == null)
            return new ResponseEntity(null, HttpStatus.BAD_REQUEST);
        File dirOnServer = new File(Constants.SRC_URL, uri);
        if (!dirOnServer.exists())
            return new ResponseEntity("this uri directory not found", HttpStatus.BAD_REQUEST);
        File fileOnServer = new File(dirOnServer, fileFromClient.getOriginalFilename());
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
    public ResponseEntity getHexByFile(@RequestParam String diruri) {
        File file = new File(Constants.SRC_URL, diruri);
        if (!file.exists())
            return new ResponseEntity("file not found", HttpStatus.BAD_REQUEST);

        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file.toString()))) {
            String fHex = DigestUtils.md5Hex(in);
            return new ResponseEntity(fHex, HttpStatus.OK);
        } catch (IOException e) {
            return new ResponseEntity(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
//https://www.baeldung.com/java-md5
//3. MD5 Using Apache Commons
/*@Test
public void givenPassword_whenHashingUsingCommons_thenVerifying()  {
    String hash = "35454B055CC325EA1AF2126E27707052";
    String password = "ILoveJava";

    String md5Hex = DigestUtils
      .md5Hex(password).toUpperCase();

    assertThat(md5Hex.equals(hash)).isTrue();
}*/