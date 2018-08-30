package tw.imyz.template;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Template {
    private static final String template_regex = "\\$\\{([^}]+)}";
    private static final Pattern template_pattern = Pattern.compile(template_regex);

    /**
     * replace string with key value pair
     * ${key}` will be replaced by a value
     * @param template template string
     * @param replacement map of key value going to fill the template
     * @return key value replaced string
     */
    public static String make_str(String template, Map<String, Object> replacement) {
        StringBuffer buffer = new StringBuffer();

        Matcher matcher = template_pattern.matcher(template);

        while (matcher.find()) {
            String replacement_str = replacement.getOrDefault(matcher.group(1), "").toString();
            matcher.appendReplacement(buffer, replacement_str);
        }
        matcher.appendTail(buffer);

        return buffer.toString().replaceAll("\\\\\\{", "{");
    }
}
