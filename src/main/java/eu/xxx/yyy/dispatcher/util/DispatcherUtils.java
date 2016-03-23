package eu.xxx.yyy.dispatcher.util;

public class DispatcherUtils {
    
    public static String nnnnnn(String number) throws IllegalArgumentException {
        if (!isInteger(number)) {
            throw new IllegalArgumentException("String '" + number + "' is not an integer.");
        }
        return nnnnnn(Integer.parseInt(number));
    }
    
    public static String nnnnnn(int number) {
        int maxDigits = 6;
        String retVal = "";
        for (int i = maxDigits; i > Integer.toString(number).length(); i--) {
            retVal += "0";
        }
        retVal += number;
        return retVal;
    }
    
    private static boolean isInteger(String value) {
        return value != null && value.matches("\\d+");
    }
    
//    public static void main(String[] args) {
//        System.out.println("***** " + nnnnnn(10));
//        System.out.println("***** " + nnnnnn(1));
//        System.out.println("***** " + nnnnnn("10"));
//        System.out.println("***** " + nnnnnn("1"));
//        System.out.println("***** " + nnnnnn("asb"));
//    }
}
