import com.alibaba.fastjson.util.TypeUtils;
import com.neo.util.modifyinfo.TypeProcessHandle;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Objects;

/**
 * 时间类型处理器
 * 
 * @Author: Neo
 * @Date: 2019/11/24 17:40
 * @Version: 1.0
 */
public class DateTypeProcessHandle implements TypeProcessHandle<Date> {

    public static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    public boolean isDifferent(Object o1, Object o2) {
        if (Objects.isNull(o1) && Objects.isNull(o2)) {
            return false;
        }
        if ((Objects.isNull(o1) && !Objects.isNull(o2)) || (!Objects.isNull(o1) && Objects.isNull(o2))) {
            return true;
        }
        Date n = TypeUtils.castToDate(o1);
        Date o = TypeUtils.castToDate(o2);
        n = clearTimeTail(n);
        o = clearTimeTail(o);

        return n.compareTo(o) != 0;
    }

    @Override
    public Class<?> supportTypeKey() {
        return Date.class;
    }


    @Override
    public Object format(Object o) {
        Date date = TypeUtils.castToDate(o);
        if(Objects.isNull(date)){
           return null; 
        }
        return sdf.format(date);
    }

    /**
     * 清除毫秒
     *
     * @Author: Neo
     * @Date: 2019/11/22 21:23
     * @Version: 1.0
     */
    public static Date clearTimeTail(Date date) {
        Calendar cal = new GregorianCalendar();
        cal.setTime(date);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
}
