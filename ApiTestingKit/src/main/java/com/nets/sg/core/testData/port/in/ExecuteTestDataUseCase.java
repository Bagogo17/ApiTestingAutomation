package com.nets.sg.core.testData.port.in;

import com.nets.sg.core.testResult.domain.TestDataResult;

import java.io.IOException;

public interface ExecuteTestDataUseCase {

    public TestDataResult executeTestData(String testDataId) throws IOException;
}
