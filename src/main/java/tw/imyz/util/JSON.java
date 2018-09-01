package tw.imyz.util;


import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class JSON {

    private static final JSONParser parser = new JSONParser();

    public static JSONObject parse(String json_string) throws ParseException { return (JSONObject) parser.parse(json_string); }

}
