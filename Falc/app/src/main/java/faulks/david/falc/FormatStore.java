package faulks.david.falc;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import android.util.Log;
import android.support.annotation.Nullable;
import android.support.annotation.NonNull;

/* A singleton of this class will be used to hold pre-defined number formats and utilities.
* The default Java number formatters are lacking, but I target pre-api-24, so icu is not available
* unless I use imported 3rd party libraries. Therefore, all characters are BMP only.  This is
* supposed to be a simple calculator, after all. */
public class FormatStore {

    enum DigitGrouping {
        THOUSANDS, MYRIADS, INDIC, NONE
    }


    //-------------------------------------------------
    /* Since I want to be able to have optional scientific notation, we need more than one
    Decimal format used at a time per set of symbols. We use an exponent formatter and a non-exponent
    formatter based on the expected number of digits in the number. */
    private class FormatSet {
        final DecimalFormatSymbols symbols;
        final DigitGrouping gtype;
        private int maxdigits;
        private DecimalFormat nonExponentFormatter;
        private DecimalFormat exponentFormatter;
        private Pattern splitPattern;
        // inits the FormatSet object based on symbols, group size, and an initial digit with
        FormatSet(DecimalFormatSymbols symbol, DigitGrouping groupType, int digitcount ) throws IllegalArgumentException {
            // some sanity checks
            if (symbol == null) throw new IllegalArgumentException("Parameter symbol is NULL!");
            if (digitcount < 1) throw new IllegalArgumentException("Initial Digit count is too small!");
            // setting up initial variables
            symbols = symbol;
            gtype = groupType;
            maxdigits = digitcount;
            // creating the formatters
            setupFormatters();
        }
        // gets the number of digits
        int getDigitWidth() { return maxdigits; }
        // changes the number of digits, this rebuilds the formatters
        void setDigitWidth(int newDigitWidth) throws IllegalArgumentException {
            if (newDigitWidth < 1) throw new IllegalArgumentException("newDigitWidth is too small!");
            maxdigits = newDigitWidth;
            setupFormatters();
        }
        // returns a Pattern used to split a numeric string (without thousands separators) into components
        Pattern getSplitPattern() { return splitPattern; }
        // converts BigDecimal to a string based on size and the current format
        String formatNumber(BigDecimal numberIn) {
            Log.i("FormatStore","formatNumber A: " + numberIn.toPlainString());
            BigDecimal numberx = numberIn.stripTrailingZeros();
            Log.i("FormatStore","formatNumber B: " + numberx.toPlainString());
            int numdigits = numberx.precision() - numberx.scale();
            Log.i("FormatStore","formatNumber C: " + numdigits);
            if (numdigits > maxdigits) return exponentFormatter.format(numberx);
            else {
                String res = nonExponentFormatter.format(numberx);
                Log.i("FormatStore","formatNumber D: " + res);
                // DecimalFormat cannot do indic group separation, so we use a custom function for that
                if (gtype == DigitGrouping.INDIC) return insertDigitSeparators(res);
                // otherwise, we let the formatter insert any group separators
                else return res;
            }
        }

        // converts a styring to a big decimal using the internal formatters
        BigDecimal parseNumberString(String source) throws NumberFormatException {
            Number parsedx;
            if (source.contains(symbols.getExponentSeparator())) {
                try {
                    parsedx = exponentFormatter.parse(source);
                } catch (ParseException pex1) {
                    throw new NumberFormatException("Parse Failure (Exponent): " + pex1.getLocalizedMessage());
                }
            }
            else {
                try {
                    parsedx = nonExponentFormatter.parse(source);
                } catch (ParseException pex2) {
                    throw new NumberFormatException("Parse Failure (Non-Exponent): " + pex2.getLocalizedMessage());
                }
            }
            return (BigDecimal) parsedx;
        }
        // creates the number splitting pattern
        private void setupPattern() {
            // a pattern for only digits specified by this formatter
            // this assumes that no digits are regex special characters
            StringBuilder digitNum = new StringBuilder("[");
            for (int digdex = 0; digdex < 10; digdex++) {
                digitNum.append((char)((int)symbols.getZeroDigit() + digdex));
            }
            digitNum.append("]+");
            // the first integer part (required)
            String str_minus = Pattern.quote(String.valueOf(symbols.getMinusSign()));
            StringBuilder opattern = new StringBuilder("(");
            opattern.append(str_minus).append('?').append(digitNum.toString()).append(')');
            // the second fraction part (optional)
            String str_dec = Pattern.quote(String.valueOf(symbols.getDecimalSeparator()));
            opattern.append("(?:").append(str_dec).append('(');
            opattern.append(digitNum.toString()).append(')').append(")?");
            // the third exponent part (optional)
            String str_exp = Pattern.quote(symbols.getExponentSeparator());
            opattern.append("(?:").append(str_exp).append('(');
            opattern.append(str_minus).append('?').append(digitNum.toString());
            opattern.append(')').append(")?");
            Log.i("FormatSet","pattern :  " + opattern);
            // finally, setting the pattern
            splitPattern = Pattern.compile(opattern.toString());
        }

        private void setupFormatters() {
            // basic, non-exponent formatter
            nonExponentFormatter = new DecimalFormat();
            nonExponentFormatter.setDecimalFormatSymbols(symbols);
            // grouping size
            int gsize;
            if (gtype == DigitGrouping.THOUSANDS) gsize = 3;
            else if (gtype == DigitGrouping.MYRIADS) gsize = 4;
            else gsize = 0;
            nonExponentFormatter.setGroupingSize(gsize);
            nonExponentFormatter.setGroupingUsed(gsize > 0);
            // other options
            nonExponentFormatter.setDecimalSeparatorAlwaysShown(false);
            nonExponentFormatter.setMaximumFractionDigits(maxdigits);
            nonExponentFormatter.setMaximumIntegerDigits(maxdigits);
            nonExponentFormatter.setRoundingMode(RoundingMode.HALF_EVEN);
            nonExponentFormatter.setParseBigDecimal(true);
            // exponent formatter
            final String patcore = new String(new char[maxdigits-5]).replace('\0','#');
            String pattern = "0.0" + patcore + "E0";
            Log.i("setupFormatters",pattern);
            exponentFormatter = new DecimalFormat(pattern,symbols);
            exponentFormatter.setRoundingMode(RoundingMode.HALF_EVEN);
            exponentFormatter.setParseBigDecimal(true);
            exponentFormatter.setMaximumFractionDigits(maxdigits);
            // setup the number split pattern
            setupPattern();
        }
        // tests if the char is a digit using the symbols for this set
        public boolean isDigit(char testThis) {
            if (testThis < symbols.getZeroDigit()) return false;
            return (testThis <= (symbols.getZeroDigit()+9));
        }

        /* When entering a number, the digit group separator should be handled automatically, but
        nothing else should be touched (except excess zeroes) */
        public String insertDigitSeparators(String sourceString) {
            // some trivial cases
            if (sourceString == null) return null;
            if (gtype == DigitGrouping.NONE) return sourceString; // not using digit separators at all!
            if (sourceString.length() < 3) return sourceString;  // too short to insert digit separators
            // finding the end point where we start checking (decimal or exponent separator, or the end of the string)
            int dpos = sourceString.indexOf(symbols.getDecimalSeparator());
            int epos = sourceString.indexOf(symbols.getExponentSeparator());
            if (dpos < 0) dpos = sourceString.length();
            if (epos < 0) epos = sourceString.length();
            int lastpos = Math.min(dpos,epos);
            // quick check to see if what is left is too short, nothing should be inserted
            if (lastpos <= 3) return sourceString;
            else if ((gtype == DigitGrouping.MYRIADS) && (lastpos <= 4)) return sourceString;
            // starting to build an output string. resCore will be "" if lastpos is at the end (after the last char)
            StringBuilder resCore = new StringBuilder(sourceString.substring(lastpos));
            // the prefix, which might be nothing, a minus, sign, currency symbol, etc
            int ppos;
            // we look for the first digit via a loop
            for (ppos = 0; ppos < lastpos; ppos++) {
                if (isDigit(sourceString.charAt(ppos))) break;
            }
            if (ppos == lastpos) return null; // input cannot be a valid number in this case!
            // setting up the initial group size
            int gsize = 3;
            if (gtype == DigitGrouping.MYRIADS) gsize = 4;
            int startpos;
            // the loop, we move backwards, getting groups
            do {
                // getting the digit group and prepending it to the result
                startpos = Math.max(ppos,lastpos-gsize);
                resCore.insert(0,sourceString.substring(startpos,lastpos));
                // stuff we needto do if we are not at the end
                if (startpos != ppos) {
                    resCore.insert(0,symbols.getGroupingSeparator());
                    if (gtype == DigitGrouping.INDIC) gsize = 2; // only needed once
                    lastpos = startpos;
                }
            } while (startpos != ppos);
            if (ppos != 0) resCore.insert(0,sourceString.substring(0,ppos));
            // done
            return resCore.toString();
        }

    }
    //-------------------------------------------------
    // we use an enum as an identifier for the different format sets
    public enum FormatTypes {
        SIDOT,    // decimal sep is '.' , thousands sep is thin space
        SICOMMA,  // decimal sep is ',' , thousands sep is thin space
        TCDOT,    // decimal sep is '.' , thousands sep is ','
        TDCOMMA,  // decimal sep is ',' , thousands sep is '.'
        EARABIC,  // eastern arabic
        DEVANAG   // Devanaragri
    }

    // maps strings (preference string) to type enum
    private static Map<String,FormatTypes> stringTypeMap = new HashMap<>(6);

    private static void setupStringTypeMap() {
        stringTypeMap.put("SIDOT",FormatTypes.SIDOT);
        stringTypeMap.put("SICOMMA",FormatTypes.SICOMMA);
        stringTypeMap.put("TCDOT",FormatTypes.TCDOT);
        stringTypeMap.put("TDCOMMA",FormatTypes.TDCOMMA);
        stringTypeMap.put("EARABIC",FormatTypes.EARABIC);
        stringTypeMap.put("DEVANAG",FormatTypes.DEVANAG);
    }
    static {
        setupStringTypeMap();
    }

    //-------------------------------------------------
    private static FormatStore instance;

    public static FormatStore getInstance() {
        if (instance == null) instance = new FormatStore();
        return instance;
    }


    public FormatStore() {
        // my default format symbols
        DecimalFormatSymbols mysym = new DecimalFormatSymbols();
        mysym.setZeroDigit('0');
        mysym.setDecimalSeparator('.');
        mysym.setMinusSign('−');
        mysym.setGroupingSeparator('\u2009');
        mysym.setExponentSeparator("ᴇ");

        final int gsize = 17;

        // making the formatter
        FormatSet newSet = new FormatSet(mysym,DigitGrouping.THOUSANDS,gsize);
        typemap.put(FormatTypes.SIDOT,newSet);
        current = newSet;
        currentType = FormatTypes.SIDOT;

        // additional formatters
        DecimalFormatSymbols cpsym = (DecimalFormatSymbols) mysym.clone();
        cpsym.setDecimalSeparator(',');
        newSet = new FormatSet(cpsym,DigitGrouping.THOUSANDS,gsize);
        typemap.put(FormatTypes.SICOMMA,newSet);
        mysym = (DecimalFormatSymbols) mysym.clone();
        mysym.setGroupingSeparator(',');
        newSet = new FormatSet(mysym,DigitGrouping.THOUSANDS,gsize);
        typemap.put(FormatTypes.TCDOT,newSet);
        cpsym = (DecimalFormatSymbols) cpsym.clone();
        cpsym.setGroupingSeparator('.');
        newSet = new FormatSet(cpsym,DigitGrouping.THOUSANDS,gsize);
        typemap.put(FormatTypes.TDCOMMA,newSet);
        // eastern arabic digits
        DecimalFormatSymbols earab = new DecimalFormatSymbols();
        earab.setZeroDigit('\u0660');
        earab.setDecimalSeparator('\u066B');
        earab.setGroupingSeparator('\u066C');
        newSet = new FormatSet(earab,DigitGrouping.THOUSANDS,gsize);
        typemap.put(FormatTypes.EARABIC,newSet);
        // devanagari
        DecimalFormatSymbols devasym = new DecimalFormatSymbols();
        devasym.setZeroDigit('०');
        devasym.setDecimalSeparator('.');
        devasym.setGroupingSeparator(',');
        newSet = new FormatSet(devasym,DigitGrouping.INDIC,gsize);
        typemap.put(FormatTypes.DEVANAG,newSet);
        // done
        instance = this;
    }
    //------------------------------------------------
    // the current format set
    private FormatSet current;
    private FormatTypes currentType;

    public static FormatTypes getType() {
        return instance.currentType;
    }

    public static boolean changeFormatSet(@NonNull String formatKey) throws IllegalArgumentException {
        FormatTypes destType = stringTypeMap.get(formatKey);
        if (destType == null) throw new IllegalArgumentException("Unrecognized FormatType string");
        if (destType == instance.currentType) return false;
        else {
            instance.current = instance.typemap.get(destType);
            instance.currentType = destType;
            return true;
        }
    }

    //--------------------------------------------------
    // where the format sets are stores after creation
    private final HashMap<FormatTypes,FormatSet> typemap = new HashMap<>(1);

    // static methods for using the current format set
    // returns the decimal format symbols object
    public static DecimalFormatSymbols getSymbols() {
        return (DecimalFormatSymbols) instance.current.symbols.clone();
    }
    // returns the corresponding digit for which ( which must be 0 to 9)
    public static char getDigit(int which) throws IllegalArgumentException {
        if ((which < 0) || (which > 9)) throw new IllegalArgumentException("Parameter 'which' must be 0 to 9!");
        char zd = instance.current.symbols.getZeroDigit();
        if (which == 0) return zd;
        else return (char)(((int)zd) + which);
    }
    // number to string, using the formatters in the current FormatSet
    public static String format(BigDecimal numberIn) {
        return instance.current.formatNumber(numberIn);
    }
    // number to string, using the formatters in the current FormatSet
    public static BigDecimal parse(String strNumber) {
        return instance.current.parseNumberString(strNumber);
    }
    // inserts group separators in the inputted number string
    public static String insertGroupSeparators(String strNumber) {
        return instance.current.insertDigitSeparators(strNumber);
    }
    // inserts group separators in the inputted number StringBuilder
    public static String insertGroupSeparators(StringBuilder strNumber) {
        return instance.current.insertDigitSeparators(strNumber.toString());
    }
    // produces a Matcher object for splitting the string into it's components
    public static Matcher getSplitMatcher(String strNumber) {
        return instance.current.splitPattern.matcher(strNumber);
    }
    // strips group separators from the input string
    public static String stripGroupSeparators(String source) {
        Log.i("FormatStore","source: " + source);
        String tsep = String.valueOf(instance.current.symbols.getGroupingSeparator());
        return source.replace(tsep,"");
    }
    //------------------------------------------------------------------

}
