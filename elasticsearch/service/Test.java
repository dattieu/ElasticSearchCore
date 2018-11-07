package com.wse.common.elasticsearch.service;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class Test {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(Test.class);

    public static void main(String[] args) throws IOException {
        scheduleFixedDelayTask();

    }
    
    public static void scheduleFixedDelayTask() {
        LOGGER.info("Fixed delay task - " + System.currentTimeMillis() / 1000);
        System.out.println("Fixed delay task - " + System.currentTimeMillis() / 1000);
    }

}
