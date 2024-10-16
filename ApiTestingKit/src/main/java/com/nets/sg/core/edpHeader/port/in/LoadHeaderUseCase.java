package com.nets.sg.core.edpHeader.port.in;

import com.nets.sg.core.edpHeader.domain.Headers;

public interface LoadHeaderUseCase {

    public Headers loadHeader(String json);
}
