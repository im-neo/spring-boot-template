package com.neo.service;

import com.google.common.base.Stopwatch;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnel;
import com.neo.mapper.PasswordMapper;
import com.neo.model.entity.Password;
import com.neo.util.PasswordCracking;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Service
public class PasswordService implements InitializingBean {

    /**
     * 预计插入的数量
     */
    private static final int SIZE = 1000000;

    /**
     * 允许的错误率，错误率越低，所需内存空间就越大
     * fpp 范围：0.0 < fpp < 1
     */
    private static final double FPP = 0.5;
    private static BloomFilter<String> PLAINTEXT_PASSWORD_BLOOM_FILTER = BloomFilter.create((Funnel<String>) (string, primitiveSink) -> primitiveSink.putString(string, Charset.defaultCharset()), SIZE, FPP);
    private static boolean PLAINTEXT_PASSWORD_BLOOM_FILTER_INITIALIZED = false;


    private static ThreadPoolExecutor threadPool = new ThreadPoolExecutor(3, 5, 60, TimeUnit.SECONDS, new LinkedBlockingQueue());


    public static final int TEMP_PASSWORD_CAPACITY = 500;
    private Set<String> WAIT_CONFIRM_PASSWORD_PLAINTEXT_SET = new HashSet<>(TEMP_PASSWORD_CAPACITY);
    private Set<String> TEMP_PASSWORD_PLAINTEXT_SET = new HashSet<>(TEMP_PASSWORD_CAPACITY);
    private List<Password> TEMP_PASSWORD_LIST = new ArrayList<>(TEMP_PASSWORD_CAPACITY);


    @Autowired
    private PasswordMapper passwordMapper;


    public int loadFromFile(String filePath) {
        File file = new File(filePath);
        int count = 0;
        try {
            Stopwatch stopwatch = Stopwatch.createStarted();
            List<String> readLines = FileUtils.readLines(file, Charset.defaultCharset());
            System.out.println("读取文件耗时/ms：" + stopwatch.elapsed(TimeUnit.MILLISECONDS));
            for (String readLine : readLines) {
                count += savePasswordToWaitConfirm(readLine);
            }
            count += syncAndFlushCache(true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return count;
    }


    /**
     * 加入缓存
     *
     * @Author: Neo
     * @Date: 2019/11/13 16:59
     * @Version: 1.0
     */
    public int savePasswordToWaitConfirm(String pwd) {
        if (checkMightExist(pwd)) {
            // 可能存在则加入待确认
            WAIT_CONFIRM_PASSWORD_PLAINTEXT_SET.add(pwd);
        } else {
            return savePasswordToCache(pwd);
        }

        if (CollectionUtils.size(WAIT_CONFIRM_PASSWORD_PLAINTEXT_SET) < TEMP_PASSWORD_CAPACITY) {
            return 0;
        }

        return syncAndFlushCache(false);
    }


    /**
     * 同步并且刷新缓存
     *
     * @param focus 是否强制刷新
     * @Author: Neo
     * @Date: 2019/11/13 22:19
     * @Version: 1.0
     */
    public int syncAndFlushCache(boolean focus) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        Set<String> confirmedPlaintext = batchCheckExist();
        System.out.println("批量重复校验耗时/ms：" + stopwatch.elapsed(TimeUnit.MILLISECONDS) );
        if (CollectionUtils.isEmpty(confirmedPlaintext)) {
            return 0;
        }

        int count = 0;
        for (String plaintext : confirmedPlaintext) {
            count += savePasswordToCache(plaintext);
        }

        if (focus) {
            count += batchSavePassword();
        }

        WAIT_CONFIRM_PASSWORD_PLAINTEXT_SET.clear();
        return count;
    }


    /**
     * 保存密码，如果已存在则不保存
     *
     * @Author: Neo
     * @Date: 2019/11/13 15:38
     * @Version: 1.0
     */
    public int savePassword(String pwd) {
        if (checkMightExist(pwd) && checkPlaintextExist(pwd)) {
            System.out.println("重复：" + pwd);
            return 0;
        }
        PLAINTEXT_PASSWORD_BLOOM_FILTER.put(pwd);
        passwordMapper.savePassword(builderPassword(pwd));
        System.out.println("成功过插入一条");
        return 1;
    }


    /**
     * 写入缓存
     *
     * @param pwd 当前密码值
     * @Author: Neo
     * @Date: 2019/11/13 22:16
     * @Version: 1.0
     */
    public int savePasswordToCache(String pwd) {
        if (StringUtils.isBlank(pwd)) {
            return 0;
        }
        PLAINTEXT_PASSWORD_BLOOM_FILTER.put(pwd);
        if (TEMP_PASSWORD_PLAINTEXT_SET.add(pwd)) {
            TEMP_PASSWORD_LIST.add(builderPassword(pwd));
        }

        if (CollectionUtils.size(TEMP_PASSWORD_PLAINTEXT_SET) < TEMP_PASSWORD_CAPACITY) {
            return 0;
        }
        return batchSavePassword();
    }


    public Set<String> batchCheckExist() {
        Set<String> existPlaintexts = queryPlaintexts(WAIT_CONFIRM_PASSWORD_PLAINTEXT_SET);
        Iterator<String> iterator = WAIT_CONFIRM_PASSWORD_PLAINTEXT_SET.iterator();
        while (iterator.hasNext()) {
            String pwd = iterator.next();
            if (existPlaintexts.contains(pwd) || TEMP_PASSWORD_PLAINTEXT_SET.contains(pwd)) {
                iterator.remove();
            }
        }
        return WAIT_CONFIRM_PASSWORD_PLAINTEXT_SET;
    }


    /**
     * 批量保存
     *
     * @Author: Neo
     * @Date: 2019/11/13 16:59
     * @Version: 1.0
     */
    private int batchSavePassword() {
        if (CollectionUtils.isEmpty(TEMP_PASSWORD_LIST)) {
            return 0;
        }
        int count = passwordMapper.batchSavePassword(TEMP_PASSWORD_LIST);
        TEMP_PASSWORD_LIST.clear();
        TEMP_PASSWORD_PLAINTEXT_SET.clear();
        return count;
    }

    private int batchSavePassword(List<Password> list) {
        return passwordMapper.batchSavePassword(list);
    }


    private Password builderPassword(String pwd) {
        String ciphertext = DigestUtils.md5DigestAsHex(pwd.getBytes());
        return Password.builder().plaintext(pwd).ciphertext(ciphertext).build();
    }

    /**
     * 判断密码是否可能存在
     *
     * @Author: Neo
     * @Date: 2019/11/13 15:36
     * @Version: 1.0
     */
    public boolean checkMightExist(String pwd) {
        if (StringUtils.isBlank(pwd)) {
            return true;
        }
        if (!PLAINTEXT_PASSWORD_BLOOM_FILTER_INITIALIZED) {
            return true;
        }
        return PLAINTEXT_PASSWORD_BLOOM_FILTER.mightContain(pwd);
    }

    public Set<String> queryPlaintexts(Set<String> plaintexts) {
        if (CollectionUtils.isEmpty(plaintexts)) {
            return Collections.EMPTY_SET;
        }
        return passwordMapper.queryPlaintexts(plaintexts);
    }


    @Override
    public void afterPropertiesSet() throws Exception {
        threadPool.execute(() -> initPlaintextPasswordBloomFilter());
        // initPlaintextPasswordBloomFilter();
    }

    /**
     * 初始化明文密码布隆过滤器
     *
     * @Author: Neo
     * @Date: 2019/11/13 15:34
     * @Version: 1.0
     */
    private void initPlaintextPasswordBloomFilter() {
        Stopwatch stopwatch = Stopwatch.createStarted();
        int limit = 10000;
        Integer maxId = queryMaxId();
        Integer lastMaxId = 0;
        List<String> passwordList;
        do {
            passwordList = queryPlaintextForPage(lastMaxId, limit);
            if (CollectionUtils.isEmpty(passwordList)) {
                break;
            }
            passwordList.forEach(i -> PLAINTEXT_PASSWORD_BLOOM_FILTER.put(i));
            lastMaxId = queryLastMaxId(lastMaxId, limit);
            if (lastMaxId >= maxId) {
                break;
            }
            System.out.println("初始化明文密码布隆过滤器...");
        } while (CollectionUtils.size(passwordList) >= limit);
        PLAINTEXT_PASSWORD_BLOOM_FILTER_INITIALIZED = true;
        System.out.println("初始化明文密码布隆过滤器耗时/s：" + stopwatch.elapsed(TimeUnit.SECONDS));
    }

    /**
     * 通过字典破解
     *
     * @Author: Neo
     * @Date: 2019/11/20 17:31
     * @Version: 1.0
     */
    public String crackPassword(String ciphertext){
        if(StringUtils.isBlank(ciphertext)){
            return StringUtils.EMPTY;
        }
        Password password = passwordMapper.getByCiphertext(ciphertext);
        if(!Objects.isNull(password)){
            return password.getPlaintext();
        }
        return StringUtils.EMPTY;
    }
    
    /**
     * 先通过字典破解，再暴力破解
     *
     * @Author: Neo
     * @Date: 2019/11/20 17:31
     * @Version: 1.0
     */
    public String forceCrackPassword(String ciphertext){
        String plaintext = crackPassword(ciphertext);
        if(StringUtils.isNotBlank(plaintext)){
            return plaintext;
        }
        PasswordCracking passwordCracking = new PasswordCracking(6, 9, ciphertext);
        try {
            passwordCracking.crack();
        } catch (RuntimeException e) {}
        return passwordCracking.getPlaintext();
    }

    public List<Password> queryForPage(Integer lastMaxId, Integer limit) {
        return passwordMapper.queryForPage(lastMaxId, limit);
    }

    public List<String> queryPlaintextForPage(Integer lastMaxId, Integer limit) {
        return passwordMapper.queryPlaintextForPage(lastMaxId, limit);
    }

    public Integer queryLastMaxId(Integer lastMaxId, Integer limit) {
        return passwordMapper.queryLastMaxId(lastMaxId, limit);
    }

    public Integer queryMaxId() {
        return passwordMapper.queryMaxId();
    }

    public Password getByCiphertext(String ciphertext) {
        return passwordMapper.getByCiphertext(ciphertext);
    }

    public Password getByPlaintext(String plaintext) {
        return passwordMapper.getByPlaintext(plaintext);
    }

    public boolean checkPlaintextExist(String plaintext) {
        return passwordMapper.checkPlaintextExist(plaintext) > 0;
    }
}
