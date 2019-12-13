import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.fastjson.util.TypeUtils;
import com.google.common.collect.ImmutableMap;
import com.neo.util.diffinfo.BuilderDifferenceInfoHandle;
import com.neo.util.diffinfo.DateTypeProcessHandle;

import java.util.Map;

public class BuilderDifferenceInfoHandleTest {


    public static void main(String[] args) {
        Map<String, Object> oldObject = ImmutableMap.of("string", "string",
                "integer", 2,
                "date", TypeUtils.castToDate("2019-11-22 21:32:12"),
                "bigDecimal", TypeUtils.castToBigDecimal("20"),
                "aBoolean", false

        );
        TestBean newObject = TestBean.builder()
                .string("String")
                .integer(1)
                .date(TypeUtils.castToDate("2019-11-22 21:29:34"))
                .bigDecimal(TypeUtils.castToBigDecimal("10"))
                .aBoolean(true)
                .build();
        String[] compareFields = {"string", "integer", "date", "bigDecimal", "aBoolean"};
        String[] ignoreFields = {"string", "integer"};

        JSONArray array = BuilderDifferenceInfoHandle.Builder(oldObject, newObject)
                .compareFields(compareFields)
                .ignoreCompareFields(ignoreFields)
                .fieldTitleKey("标题")
                .putFieldTitleMapping("string", "这是字符串")
                .registerProcessHandle(new DateTypeProcessHandle())
                .putFieldNameProcessHandleMapping("date", new DateTypeProcessHandle())
                .configGlobalNameAnnotation(JSONField.class, "name")
                .isBuilderNullValue(true)
                .buildInfo();
        System.out.println(array.toJSONString());
        
        array = BuilderDifferenceInfoHandle.Builder(oldObject, newObject)
                .compareFields(compareFields)
                .ignoreCompareFields(ignoreFields)
                .fieldTitleKey("标题")
                .putFieldTitleMapping("string", "这是字符串")
                .registerProcessHandle(new DateTypeProcessHandle())
                .putFieldNameProcessHandleMapping("date", new DateTypeProcessHandle())
                .configGlobalNameAnnotation(JSONField.class, "name")
                .isBuilderNullValue(true)
                .buildInfo();

        System.out.println(array.toJSONString());
    }
}
