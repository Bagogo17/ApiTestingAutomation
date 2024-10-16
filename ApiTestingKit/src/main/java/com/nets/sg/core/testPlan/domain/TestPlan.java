package com.nets.sg.core.testPlan.domain;

import com.nets.sg.core.testData.domain.TestData;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class TestPlan {

    private String testPlanId;
    private List<TestData> testDataList;
}
