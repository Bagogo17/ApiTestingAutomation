package com.nets.sg.core.testPlan.port.out;

import com.nets.sg.core.testPlan.domain.TestPlan;
import java.util.Optional;

public interface QueryTestPlanPort {

    Optional<TestPlan> findById(String id);
}
