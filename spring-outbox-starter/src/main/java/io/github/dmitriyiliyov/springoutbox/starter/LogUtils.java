package io.github.dmitriyiliyov.springoutbox.starter;

public final class LogUtils {

    private LogUtils() {}

    public static String prettyPrint(Object sourceObj) {
        String sourceStr = sourceObj.toString();
        StringBuilder sb = new StringBuilder();
        String indent = "\t";
        int count = 0;
        for (char c : sourceStr.toCharArray()) {
            if (c == '{') {
                sb.append("{\n");
                count++;
                sb.append(indent.repeat(count)).append(" ");
            } else if (c == '}') {
                count--;
                sb.append("\n").append(indent.repeat(count)).append(" }");
            } else if (c == ',') {
                sb.append(",\n").append(indent.repeat(count));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
