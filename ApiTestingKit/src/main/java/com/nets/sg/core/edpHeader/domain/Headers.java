package com.nets.sg.core.edpHeader.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Headers {

    private String testDataId;
    private String apiKey;
    private String requestId;
    private String requestDateTime;
    private String correlationId;
}
