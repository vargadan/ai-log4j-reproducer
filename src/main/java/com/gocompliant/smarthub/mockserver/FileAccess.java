package com.gocompliant.smarthub.mockserver;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

@Value
@Slf4j
public class FileAccess implements ResponseReader {

    String root;

    @Override
    public Response read(String path) throws IOException {
        var file = new File(root, path + "/response.json");
        if (!file.exists() || !file.canRead()) {
            return Response.builder().statusCode(500).body("Cannot read file : " + path).build();
        }
        var fis = new FileInputStream(file);
        return Response.builder().statusCode(200).body(new String(fis.readAllBytes())).build();
    }

}
