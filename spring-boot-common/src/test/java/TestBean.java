import com.alibaba.fastjson.annotation.JSONField;
import com.neo.util.modifyinfo.Property;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestBean {

    @JSONField(name = "字符串")
    private String string;

    @JSONField(name = "整形")
    private Integer integer;

    @JSONField(name = "时间")
    @Property(nameAnnotationClass = JSONField.class , nameAnnotationClassField = "name")
    private Date date;

    @JSONField(name = "数字")
    private BigDecimal bigDecimal;

    @JSONField(name = "布尔")
    private Boolean aBoolean;
}