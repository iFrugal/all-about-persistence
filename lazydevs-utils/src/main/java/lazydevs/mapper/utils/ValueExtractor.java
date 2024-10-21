package lazydevs.mapper.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Abhijeet Rai
 */
public class ValueExtractor {


    public static Map<String, String> extractValues(String text, String template){
        if(null == text || null == template){
            return null;
        }
        String currentTemplate = template;
        Map<Integer, String> indexToKeyMap = new HashMap<>();
        int start = -1, end = -1; int index = 1;
        StringBuilder reconstructedTemplateBuilder = new StringBuilder();
        while((start = currentTemplate.indexOf("${")) >= 0 && (end = currentTemplate.indexOf("}")) > 0){
            indexToKeyMap.put(index++, currentTemplate.substring(start + 2, end));
            reconstructedTemplateBuilder.append(currentTemplate.substring(0, start) + "(\\w+)");
            currentTemplate = currentTemplate.substring(end + 1);
        }
        String reconstructedTemplate = reconstructedTemplateBuilder.append(currentTemplate).toString();
        Pattern pattern;
        if((pattern = templateToPatternMap.get(reconstructedTemplate)) == null){
            pattern = Pattern.compile(reconstructedTemplate);
            templateToPatternMap.put(reconstructedTemplate, pattern);
        }
        Matcher matcher = pattern.matcher(text);
        Map<String, String> output =  new HashMap<>();
        if(matcher.find()) {
            for(int i = 1; i <= matcher.groupCount(); i++){
                output.put(indexToKeyMap.get(i), matcher.group(i));
            }
        }
        return output;
    }

    private static final Map<String, Pattern> templateToPatternMap = new HashMap<>();
}
