import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.util.TypeUtils;
import com.google.common.collect.ImmutableMap;
import com.neo.util.modifyinfo.BuilderModifyInfoHandle;
import com.neo.util.modifyinfo.DateTypeProcessHandle;


import java.util.Map;

public class BuilderModifyInfoHandleTest {


    public static void main(String[] args) {
        TestBean newObject = TestBean.builder()
                .string("String")
                .integer(1)
                .date(TypeUtils.castToDate("2019-11-22 21:29:34"))
                .bigDecimal(TypeUtils.castToBigDecimal("10"))
                .aBoolean(true)
                .build();


        Map<String, Object> oldObject = ImmutableMap.of("string", "string",
                "integer", 2,
                "date", TypeUtils.castToDate("2019-11-22 21:32:12"),
                "bigDecimal", TypeUtils.castToBigDecimal("20"),
                "aBoolean", false

        );
        String[] compareFields = {"string", "integer", "date", "bigDecimal", "aBoolean"};
        String[] ignoreFields = {"string", "integer"};


        JSONArray array = BuilderModifyInfoHandle.Builder(newObject, oldObject)
                .compareFields(compareFields)
                .ignoreCompareFields(ignoreFields)
                .fieldTitleKey("标题")
                .putFieldTitleMapping("string", "这是字符串")
                .registerProcessHandle(new DateTypeProcessHandle())
                .isBuilderNullValue(true)
                .build();

        System.out.println(array.toJSONString());
    }
}
