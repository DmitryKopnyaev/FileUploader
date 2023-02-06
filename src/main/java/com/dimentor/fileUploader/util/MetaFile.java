package com.dimentor.fileUploader.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetaFile {
    private String name; //clearName
    private int version;
    private String hex;
}
