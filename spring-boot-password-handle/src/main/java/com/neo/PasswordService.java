package com.neo;

import com.google.common.base.Stopwatch;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnel;
import com.neo.mapper.PasswordMapper;
import com.neo.model.entity.Password;
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
import java.util.HashSet;
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

    private static ThreadPoolExecutor threadPool = new ThreadPoolExecutor(3, 5, 60, TimeUnit.SECONDS, new LinkedBlockingQueue());


    public static final int TEMP_PASSWORD_CAPACITY = 500;
    private Set<String> TEMP_PASSWORD_PLAINTEXT_SET = new HashSet<>(TEMP_PASSWORD_CAPACITY);
    private List<Password> TEMP_PASSWORD_LIST = new ArrayList<>(TEMP_PASSWORD_CAPACITY);


    @Autowired
    private PasswordMapper passwordMapper;

    public Password getByCiphertext(String ciphertext) {
        return passwordMapper.getByCiphertext(ciphertext);
    }

    public Password getByPlaintext(String plaintext) {
        return passwordMapper.getByPlaintext(plaintext);
    }

    /**
     * 保存密码，如果已存在则不保存
     *
     * @Author: Neo
     * @Date: 2019/11/13 15:38
     * @Version: 1.0
     */
    public int savePassword(String pwd) {
        if (checkExist(pwd)) {
            System.out.println("重复：" + pwd);
            return 0;
        }
        PLAINTEXT_PASSWORD_BLOOM_FILTER.put(pwd);
        passwordMapper.savePassword(builderPassword(pwd));
        System.out.println("成功过插入一条");
        return 1;
    }


    /**
     * 加入缓存
     *
     * @Author: Neo
     * @Date: 2019/11/13 16:59
     * @Version: 1.0
     */
    public int savePasswordToCache(String pwd) {
        if (checkExist(pwd)) {
            System.out.println("重复：" + pwd);
            return 0;
        }
        PLAINTEXT_PASSWORD_BLOOM_FILTER.put(pwd);
        if (TEMP_PASSWORD_PLAINTEXT_SET.add(pwd)) {
            TEMP_PASSWORD_LIST.add(builderPassword(pwd));
        }

        if (CollectionUtils.size(TEMP_PASSWORD_PLAINTEXT_SET) > TEMP_PASSWORD_CAPACITY) {
            int count = passwordMapper.batchSavePassword(TEMP_PASSWORD_LIST);
            TEMP_PASSWORD_LIST.clear();
            TEMP_PASSWORD_PLAINTEXT_SET.clear();

            return count;
        }
        return 0;
    }

    /**
     * 批量保存
     *
     * @Author: Neo
     * @Date: 2019/11/13 16:59
     * @Version: 1.0
     */
    private int batchSavePassword() {
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
     * 判断密码是否村子
     *
     * @Author: Neo
     * @Date: 2019/11/13 15:36
     * @Version: 1.0
     */
    public boolean checkExist(String pwd) {
        if (StringUtils.isBlank(pwd)) {
            return true;
        }
        boolean mightContain = PLAINTEXT_PASSWORD_BLOOM_FILTER.mightContain(pwd);
        if (mightContain) {
            if (TEMP_PASSWORD_PLAINTEXT_SET.contains(pwd)) {
                return true;
            }
            Password p = getByPlaintext(pwd);
            // 判断是否存在
            if (!Objects.isNull(p)) {
                return true;
            }
        }
        return false;
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


    @Override
    public void afterPropertiesSet() throws Exception {
        threadPool.execute(() -> initPlaintextPasswordBloomFilter());
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
        System.out.println("初始化明文密码布隆过滤器耗时：" + stopwatch.elapsed(TimeUnit.SECONDS));
    }

    public int loadFromFile(String filePath) {
        File file = new File(filePath);
        int count = 0;
        try {
            List<String> readLines = FileUtils.readLines(file, Charset.defaultCharset());
            for (String readLine : readLines) {
                count += savePasswordToCache(readLine);
            }
            count += batchSavePassword();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return count;
    }
}
