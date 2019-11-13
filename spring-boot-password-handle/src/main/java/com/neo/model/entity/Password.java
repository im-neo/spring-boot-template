package com.neo.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Password {
    
    private Integer id;
    
    private String plaintext;
    
    private String ciphertext;
}
