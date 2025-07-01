package lazydevs.mapper.utils.reflection;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;
import org.testng.Assert;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ReflectionUtilsTest {

    @Test
    void getInterfaceReference() {
        InitDTO initDTO = new InitDTO();
        initDTO.setFqcn(AB.class.getName());
        AB ab1 = new AB(1, "Abhijeet");
        System.setProperty("env.a", "1");
        System.setProperty("env.b", "Abhijeet");
        initDTO.setConstructorArgs(Arrays.asList(
                getArgDTOUsingSysProps("env.a", "int"),
                getArgDTOUsingSysProps("env.b", "java.lang.String")
        ));
        AB ab = ReflectionUtils.getInterfaceReference(initDTO, AB.class);
        Assert.assertEquals(ab, ab1);
    }

    private InitDTO.ArgDTO getArgDTOUsingSysProps(String key, String typeFqcn){
        InitDTO.ArgDTO argDTO =  new InitDTO.ArgDTO();
        argDTO.setSysPropKey(key);
        argDTO.setTypeFqcn(typeFqcn);
        return argDTO;
    }

    @AllArgsConstructor @EqualsAndHashCode
    private static class AB{
        private int a;
        private String B;
    }
}