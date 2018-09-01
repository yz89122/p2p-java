package tw.imyz.asciiart;

import java.util.Random;

public class Faces {

    private static final Random rand = new Random();

    private static final String[] BAD_THINGS_HAPPENED = {";(", ";C", "QQ", "QAQ", "( ; _ ; )/~~~"};
    private static final String[] GOOD_THINGS_HAPPENED = {":)", ":D", "XD", "(´･ω･`)", "(´･ω･`)"};

    public static String bad() {
        return BAD_THINGS_HAPPENED[rand.nextInt(BAD_THINGS_HAPPENED.length)];
    }
    public static String good() {
        return GOOD_THINGS_HAPPENED[rand.nextInt(GOOD_THINGS_HAPPENED.length)];
    }

}
