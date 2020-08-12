package com.neo.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterfaceConfig {

    private Long id;

    private String name;

    private String url;

    private String method;

    private String requestHeader;

    private String queryParam;

    private String requestBody;
    
    private Integer valid;

}
