package faulks.david.falc;

import android.util.Log;

import java.text.DecimalFormatSymbols;
import java.util.regex.Matcher;

/* The task of manipulating the number in a calculator display turns out to be rather intricate,
especially since I use the Format store and Format symbols instead of fixed characters. The displayed
number is broken into parts for easier manipulation: use buildNumberString to get the output string.

Note that setFromNumberString assumes that the input number is build according to the rules of the
current FormatSet in FormatStore

*/
public class SplitNumberString {
    private boolean is_negative = false;
    private StringBuilder integer_part = new StringBuilder();
    private boolean zero_integer = true;
    private boolean has_decimal_separator;
    private StringBuilder fraction_part = new StringBuilder();
    private boolean fraction_zeros = true;
    private boolean negative_exponent = false;
    private StringBuilder exponent_part = new StringBuilder();
    private boolean zero_exponent = false;
    //-------------------------------------------------------
    // constructor, sets the display to be 0 (using the char defined in DecimalFormatSymbols)
    SplitNumberString(DecimalFormatSymbols symbols) {
        integer_part.append(symbols.getZeroDigit());
    }
    //-------------------------------------------------------
    // produces the full string from the parts
    String buildNumberString() {
        StringBuilder res_src = new StringBuilder();
        if (is_negative) res_src.append(FormatStore.getSymbols().getMinusSign());
        res_src.append(integer_part);
        if (has_decimal_separator) {
            res_src.append(FormatStore.getSymbols().getDecimalSeparator());
            res_src.append(fraction_part);
        }
        if (exponent_part.length()>0) {
            res_src.append(FormatStore.getSymbols().getExponentSeparator());
            if (negative_exponent) res_src.append(FormatStore.getSymbols().getMinusSign());
            res_src.append(exponent_part);
        }
        return res_src.toString();
    }
    //-------------------------------------------------------
    // Adds a digit. Does NOT check for a valid range
    boolean appendDigit(int which) {
        // adding it to the exponent
        if (exponent_part.length()>0) {
            if ((zero_exponent) && (which == 0)) return false;
            else if (zero_exponent) {
                exponent_part.setCharAt(0,FormatStore.getDigit(which));
                zero_exponent = false;
            }
            else exponent_part.append(FormatStore.getDigit(which));
        }
        // adding it when we are after the decimal separator
        else if (has_decimal_separator) {
            fraction_part.append(FormatStore.getDigit(which));
            fraction_zeros = (fraction_zeros && (which==0));
        }
        // adding it if the number is zero with no decimal
        else if (zero_integer) {
            if (which == 0) return false;
            integer_part.setCharAt(0,FormatStore.getDigit(which));
            zero_integer = false;
        }
        // otherwise, the integer is not 0
        else integer_part.append(FormatStore.getDigit(which));
        return true;
    }

    // add decimal separator
    boolean addDecimalSeparator() {
        if (exponent_part.length() > 0) return false;
        if (has_decimal_separator) return false;
        has_decimal_separator = true;
        return true;
    }
    // add exponent, the exponent will start at '0'
    boolean addExponent() {
        if (exponent_part.length() > 0) return false;
        if (zero_integer && fraction_zeros) return false;
        exponent_part.append(FormatStore.getDigit(0));
        zero_exponent = true;
        if (has_decimal_separator && (fraction_part.length() == 0)) {
            has_decimal_separator = false;
        }
        return true;
    }

    // toggle sign, if that makes sense
    boolean toggleSign() {
        if (exponent_part.length() > 0) {
            if (zero_exponent) return false;
            negative_exponent = !negative_exponent;
        }
        else {
            if (zero_integer && fraction_zeros) return false;
            is_negative = !is_negative;
        }
        return true;
    }

    // remove the last 'digit' (and perhaps entire section)
    boolean backspace() {
        int el = exponent_part.length();
        // getting rid of the exponent
        if (el == 1) {
            negative_exponent = false;
            exponent_part.deleteCharAt(0);
            zero_exponent = false;
        }
        // reducing the exponent
        else if (el > 1) {
            exponent_part.deleteCharAt(exponent_part.length()-1);
        }
        // decimal point deletion
        else if (has_decimal_separator) {
            int dl = fraction_part.length();
            // delete the entire fractional part
            if (dl < 2) {
                has_decimal_separator = false;
                fraction_zeros = true;
            }
            // delete only the last digit (extra code because I keep track of whether or not it is 000...)
            else {
                fraction_part.deleteCharAt(fraction_part.length()-1);
                boolean fz = true;
                for(int qx = 0;;qx++) {
                    fz = fraction_part.charAt(qx) == FormatStore.getDigit(0);
                    if (!fz) break;
                }
                fraction_zeros = fz;
            }
        }
        // we might end up deleting everything
        else if (integer_part.length() == 1) {
            is_negative = false;
            zero_integer = true;
            integer_part.setCharAt(0,FormatStore.getDigit(0));
        }
        else {
            integer_part.deleteCharAt(integer_part.length()-1);
        }
        return true;
    }
    //-------------------------------------------------------
    // set internal state from input number string
    void setFromNumberString(String numberString) throws NumberFormatException {
        if (numberString == null) throw new NumberFormatException("Input string is null!");
        if (numberString.isEmpty()) throw new NumberFormatException("Input string is empty!");
        // removing group separators
        String tempSource = FormatStore.stripGroupSeparators(numberString);
        // trying to split using a Matcher
        Log.i("NumDisplay",tempSource);
        Matcher splitter = FormatStore.getSplitMatcher(tempSource);
        boolean bmatch = splitter.matches();
        if (!bmatch) throw new NumberFormatException("Input string failed split matching!");
        // checking the integer part
        String intpart = splitter.group(1);
        if ((intpart == null) || (intpart.isEmpty())) {
            throw new NumberFormatException("Input String did not parse correctly!");
        }
        // clearing the old exponent value
        exponent_part.setLength(0);
        zero_exponent = false;
        negative_exponent = false;
        // setting the new exponent (if we have one)
        String epart = splitter.group(3);
        if ((epart != null) && (!epart.isEmpty())) {
            exponent_part.append(epart);
            if (exponent_part.charAt(0) == FormatStore.getSymbols().getMinusSign()) {
                negative_exponent = true;
                exponent_part.deleteCharAt(0);
            }
            else if (exponent_part.charAt(0) == FormatStore.getDigit(0)) {
                zero_exponent = true;
            }
        }
        // clearing the fraction parr
        fraction_part.setLength(0);
        fraction_zeros = true;
        has_decimal_separator = false;
        // setting the fractional part (if we have one)
        epart = splitter.group(2);
        if ((epart != null) && (!epart.isEmpty())) {
            has_decimal_separator = true;
            fraction_part.append(epart);
            boolean fz = true;
            for(int qx = 0 ; qx < fraction_part.length() ; qx++) {
                fz = fraction_part.charAt(qx) == FormatStore.getDigit(0);
                if (!fz) break;
            }
            fraction_zeros = fz;
        }
        // clearing the integer part
        integer_part.setLength(0);
        is_negative = false;
        zero_integer = true;
        // setting the integer part
        integer_part.append(intpart);
        if (integer_part.charAt(0) == FormatStore.getSymbols().getMinusSign()) {
            is_negative = true;
            integer_part.deleteCharAt(0);
        }
        else if (integer_part.length() == 1) {
            zero_integer = (integer_part.charAt(0) == FormatStore.getDigit(0));
        }
        // done!
    }
}
