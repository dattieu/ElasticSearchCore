package com.wse.common.elasticsearch.helper;

import java.io.IOException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

public final class FileReader {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(FileReader.class);
    
    public static final String readFileAsString(String path) {
        URL url = Resources.getResource(path);
        String text = "";
        try {
            text = Resources.toString(url, Charsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("Unable to read from this file: " + path);
        }

        return text;
    }

}
