package lazydevs.mapper.rest;

import lazydevs.mapper.rest.multipart.Body;
import lazydevs.mapper.utils.SerDe;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class RestInputTest {

    @Test
    public void testRestInputNoArgsCons(){
        RestInput restInput = new RestInput();
        restInput.setUrl("u");
        restInput.setHttpMethod(RestInput.HttpMethod.GET);
        restInput.setPayload("p");
        restInput.setHeaders(getMap("a", "a1", "b", "b1"));
        check(restInput);
    }

    @Test
    public void testRestInputBuilder(){
        RestInput restInput = RestInput.multipartBuilder()
                .httpMethod(RestInput.HttpMethod.GET)
                .addHeader("a", "a1")
                .addHeader("b", "b1")
                .url("u")
                .addPart(Body.getInstance("a", new File("")))
                .addPart(Body.getInstance("b", "ABCD"))
                .addPart(Body.getInstance("c", new ByteArrayInputStream(new byte[]{})))
                .addPart(Body.getInstance("d", new byte[]{}))
                .build();
        check(restInput);

         restInput = RestInput.builder()
                .httpMethod(RestInput.HttpMethod.GET)
                .addHeader("a", "a1")
                .addHeader("b", "b1")
                .url("u")
                .payload("p")
                .build();
        check(restInput);
    }

    @Test
    public void testJsonSerDe(){
        RestInput restInput = RestInput.builder()
                .httpMethod(RestInput.HttpMethod.GET)
                .addHeader("a", "a1")
                .addHeader("b", "b1")
                .url("u")
                .payload("p")
                .build();
        check(restInput);
        String s = SerDe.JSON.serialize(restInput, true);
        System.out.println(s);
        check(SerDe.JSON.deserialize(s, RestInput.class));

    }

    private void check(RestInput restInput){
        assertEquals(restInput.getHttpMethod(), RestInput.HttpMethod.GET);
        assertEquals(restInput.getUrl(), "u");
        assertTrue(restInput.getHeaders().containsKey("a"));
        assertEquals(restInput.getHeaders().get("a"), "a1");
        assertTrue(restInput.getHeaders().containsKey("b"));
        assertEquals(restInput.getHeaders().get("b"), "b1");
        if(restInput.isMultipart()){
            assertEquals(restInput.getBodies().get(0).getType(), Body.Type.FILE);
            assertTrue(restInput.getBodies().get(0).getBody() instanceof File);

            assertEquals(restInput.getBodies().get(1).getType(), Body.Type.STRING);
            assertTrue(restInput.getBodies().get(1).getBody() instanceof String);

            assertEquals(restInput.getBodies().get(2).getType(), Body.Type.INPUT_STREAM);
            assertTrue(restInput.getBodies().get(2).getBody() instanceof InputStream);

            assertEquals(restInput.getBodies().get(3).getType(), Body.Type.BYTE_ARRAY);
            assertTrue(restInput.getBodies().get(3).getBody() instanceof byte[]);
        }else {
            assertEquals(restInput.getPayload(), "p");
        }
    }

    private Map<String, String> getMap(String k1, String v1, String k2, String v2){
        Map<String, String> map =  new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        return map;
    }

}