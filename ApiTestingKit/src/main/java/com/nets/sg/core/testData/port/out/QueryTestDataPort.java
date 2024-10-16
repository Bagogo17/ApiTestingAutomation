package com.nets.sg.core.testData.port.out;

import com.nets.sg.core.testData.domain.TestData;

import java.util.Optional;

public interface QueryTestDataPort {

    Optional<TestData> findById(String id);
}
