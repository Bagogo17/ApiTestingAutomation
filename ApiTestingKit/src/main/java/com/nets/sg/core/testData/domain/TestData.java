package com.nets.sg.core.testData.domain;

import com.nets.sg.core.edpHeader.domain.Headers;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TestData {

    private String testDataId;
    private Headers headers;
    private String body;
    private Long testDuration;
    private int thread;
    private TestEndPoint endPointParty1;
    private TestEndPoint endPointParty2;

    @Getter
    @Setter
    public class TestEndPoint {
        private String url;
        private String port;

        public TestEndPoint(String url,String port) {
            this.url = url;
            this.port = port;
        }
    }
}
