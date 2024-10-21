package lazydevs.conman;

import lazydevs.mapper.utils.SerDe;
import lazydevs.mapper.utils.engine.TemplateEngine;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Abhijeet Rai
 */
@Test
public class ConmanTemplateTest {

    public void a(){
        String a = "<#assign seq = [\"K3\",\"CA\",\"K1\",\"FK\",\"IN\",\"GB\",\"US\",\"US-HF\",\"US-TX\"] >\n" +
                "              \"hidden\" : \"${seq?seq_contains(params.tenantId)?string('true', 'false')}\"";
        System.out.println(TemplateEngine.getInstance().generate(a, getMap()));
    }

    private Map<String, Object> getMap(){
        Map<String, Object> map = new HashMap<>();
        Map<String, Object> params = new HashMap<>();
        params.put("tenantId", "US1");
        map.put("params", params);
        return map;
    }

    public void test(){
        String s = "- request:\n" +
                "    uri: /mock/101\n" +
                "    httpMethod: GET\n" +
                "    validation:\n" +
                "      headers:\n" +
                "        \"a\" :\n" +
                "          required : true\n" +
                "      queryParams:\n" +
                "        \"b\" :\n" +
                "          required: true\n" +
                "  response:\n" +
                "    body: |-\n" +
                "      {\n" +
                "        \"a\" : \"a800\",\n" +
                "        \"b\" : \"b800\",\n" +
                "        \"xyz\" : {\n" +
                "          \"x\" : \"x800\",\n" +
                "          \"y\" : \"y800\",\n" +
                "          \"z\" : \"z800\"\n" +
                "        }\n" +
                "      }\n" +
                "    contentType: application/json\n" +
                "    statusCode: 200\n" +
                "    responseHeaders:\n" +
                "      \"foo\": \"bar\"\n" +
                "      \"x\": \"y\"";
        List<MockConfig> mockConfigList = SerDe.YAML.deserializeToList(s, MockConfig.class);
        System.out.println(mockConfigList);
    }
}
