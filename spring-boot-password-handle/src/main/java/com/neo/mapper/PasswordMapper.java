package com.neo.mapper;

import com.neo.model.entity.Password;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

public interface PasswordMapper {


    Password getByCiphertext(@Param("ciphertext") String ciphertext);

    Password getByPlaintext(@Param("plaintext") String plaintext);

    int savePassword(Password password);

    List<Password> queryForPage(@Param("lastMaxId") Integer lastMaxId, @Param("limit") Integer limit);

    int batchSavePassword(List<Password> passwords);
    
    List<String> queryPlaintextForPage(@Param("lastMaxId") Integer lastMaxId, @Param("limit") Integer limit);
    
    Integer queryLastMaxId(@Param("lastMaxId") Integer lastMaxId, @Param("limit") Integer limit);
    
    Integer queryMaxId();
    
    int checkPlaintextExist(@Param("plaintext") String plaintext);
    
    Set<String> queryPlaintexts(@Param("plaintexts") Set<String> plaintexts);
}
