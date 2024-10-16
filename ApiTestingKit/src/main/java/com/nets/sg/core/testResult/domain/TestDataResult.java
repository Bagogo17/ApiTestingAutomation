package com.nets.sg.core.testResult.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.util.Map;

@Getter
@Setter
public class TestDataResult {

    private String testDataId;
    private Map <String,Duration> timeTakenEachCorrId;
    private Map <String,String> statusEachCorrId;
    private int samples;
    private Duration avgTimeTaken;
    private Duration maxTimeTaken;
    private Duration minTimeTaken;
}
