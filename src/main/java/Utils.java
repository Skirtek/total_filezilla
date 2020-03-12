import java.util.Arrays;

public class Utils {
    private static String[] forbiddenSigns = {"\\", "/", "?", "%", "*", ":", "|", "\"", "<", ">"};

    public static boolean isNullOrWhitespace(String value) {
        return value == null || value.trim().length() == 0;
    }

    public static boolean isDirectoryNameValid(String value){
        return !isNullOrWhitespace(value) && Arrays.stream(forbiddenSigns).noneMatch(value::contains);
    }
}
