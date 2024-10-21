package lazydevs.persistence.writer.general;

import lazydevs.mapper.utils.SerDe;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.testng.annotations.Test;

import java.util.Map;

import static lazydevs.persistence.util.MapUtils.getMap;

/**
 * @author Abhijeet Rai
 */
public class TemplatisedWriteInstructionTest {

    @Test
    public void testProcess() {
        A a = new A("x${i}", "y${i}");
        a.setTemplate(true);
        String s = SerDe.JSON.serialize(a, true);
        System.out.println(s);
        a = SerDe.JSON.deserialize(s, A.class);
        Map<String, Object> map = getMap("i", 10, "j", 20);
        a = TemplatisedWriteInstruction.process(map, a);
        System.out.println(a);



    }

    @AllArgsConstructor @Getter @Setter @ToString @NoArgsConstructor
    public static class A extends TemplatisedWriteInstruction{
        private String x, y;
    }


}