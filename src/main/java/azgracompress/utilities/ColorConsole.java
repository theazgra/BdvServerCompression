package azgracompress.utilities;

public final class ColorConsole {
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BLACK = "\u001B[30m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_PURPLE = "\u001B[35m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_WHITE = "\u001B[37m";

    public enum Color {Black, Red, Green, Yellow, Blue, Purple, Cyan, White}

    public enum Target {stdout, stderr}

    private static String getColor(final Color color) {
        switch (color) {
            case Black:
                return ANSI_BLACK;
            case Red:
                return ANSI_RED;
            case Green:
                return ANSI_GREEN;
            case Yellow:
                return ANSI_YELLOW;
            case Blue:
                return ANSI_BLUE;
            case Purple:
                return ANSI_PURPLE;
            case Cyan:
                return ANSI_CYAN;
            default:
                return ANSI_WHITE;
        }
    }

    public static void printf(final Color color, final String format, final Object... args) {
        fprintf(Target.stdout, color, String.format(format, args));
    }

    public static void fprintf(final Target target, final Color color, final String format, final Object... args) {
        fprintf(target, color, String.format(format, args));
    }

    public static void fprintf(final Target target, final Color color, final String string) {

        switch (target) {
            case stdout:
                System.out.println(getColor(color) + string + ANSI_RESET);
                break;
            case stderr:
                System.err.println(getColor(color) + string + ANSI_RESET);
                break;
        }
    }
}
