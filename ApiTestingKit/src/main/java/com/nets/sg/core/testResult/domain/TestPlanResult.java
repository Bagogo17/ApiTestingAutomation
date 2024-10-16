package com.nets.sg.core.testResult.domain;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TestPlanResult {

    private String testPlanId;
    private List<TestDataResult> testDataResultList;
}
