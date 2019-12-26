import com.neo.util.idcard.IdCardGenerator;
import com.neo.util.district.DistrictPicker;

import java.util.Set;

public class IdCardGeneratorTest {

    public static void main(String[] args) {


        Set<String> idcards = IdCardGenerator.Builder(DistrictPicker.上海市_上海市市辖区_嘉定区, 2018, 1000).generator();
    }
}
