package lazydevs.mapper.utils;

import lazydevs.mapper.utils.engine.TemplateEngine;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.*;

/**
 * @author Abhijeet Rai
 */
public class ValueExtractorTest {
    @Test
    public void testExtractValues() {
        String text = "Thanks, this is your value : 100 XX. And this is your account number : 219AD098 YY";
        String template = "Thanks, this is your value : ${abc} XX. And this is your account number : ${xyz} YY";
        Map<String, String> extracted = ValueExtractor.extractValues(text, template);
        Assert.assertEquals(extracted.size(), 2);
        Assert.assertEquals(extracted.get("abc"), "100");
        Assert.assertEquals(extracted.get("xyz"), "219AD098");
    }


    @Test
    public void test(){
        String template = "<glmSAPInboundData>\n" +
                "                <checkAmount>2397.00-</checkAmount>\n" +
                "                <checkDate>2021-04-29T00:00:00</checkDate>\n" +
                "                <checkNumber>0990426</checkNumber>\n" +
                "                <documentCurrencyAmount>2397.00-</documentCurrencyAmount>\n" +
                "                <glmInvoiceNumber>${paymentId}</glmInvoiceNumber>\n" +
                "                <paymentStatus>C</paymentStatus>\n" +
                "                <postDate>2021-04-29T00:00:00</postDate>\n" +
                "                <sapInvoiceNumber>10000034</sapInvoiceNumber>\n" +
                "                <sapTranId>GLMPMT</sapTranId>\n" +
                "              </glmSAPInboundData>";
        Map<String, Object> map = new HashMap<>();
        map.put("paymentId", "xxx1");
        String text = TemplateEngine.getInstance().generate(template, map);
        Map<String, String> extracted = ValueExtractor.extractValues(text, template);
        Assert.assertEquals(extracted.size(), 1);
        Assert.assertEquals(extracted.get("paymentId"), "xxx1");
    }
}