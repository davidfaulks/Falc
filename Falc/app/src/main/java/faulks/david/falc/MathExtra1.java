package faulks.david.falc;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.math.BigDecimal;
import java.math.MathContext;

/* Big Decimal is lacking in some operations. Also, some constants.
This class will store MathContexts for operations. Maximum number of
digits will be 34 (DECIMAL 128)
 */

public class MathExtra1 {

    static final BigDecimal PI   = new BigDecimal("3.1415926535897932384626433832795028841971693993751058209749445923078"); // π
    static final BigDecimal EC   = new BigDecimal("2.7182818284590452353602874713526624977572470936999595749669676277241"); // e
    static final BigDecimal EMC  = new BigDecimal("0.5772156649015328606065120900824024310421593359399235988057672348849");
    static final BigDecimal LN10 = new BigDecimal("2.3025850929940456840179914546843642076011014886287729760333279009676"); // ln10
    static final BigDecimal TWO = new BigDecimal(2);
    static int max_pow_integer = 999999999;
    static final BigDecimal MAXPOW = new BigDecimal(max_pow_integer);
    static final BigDecimal MAXFLT = new BigDecimal(Double.MAX_VALUE);
    static final BigDecimal MINFLT = new BigDecimal(Double.MIN_NORMAL);
    //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    // math contexts
    private MathContext primary;
    private MathContext extra_digits;
    private MathContext double_digits;
    // approximation limit
    private BigDecimal eps;

    // getting and manipulating the math contexts
    public MathContext getCurrentContext() { return primary; }
    public BigDecimal getEpsilon() { return eps; }

    public boolean setCurrentContext(MathContext newContext) {
        if (newContext == null) return false;
        int prec = newContext.getPrecision();
        if (prec > 34) return false;
        primary = newContext;
        extra_digits = new MathContext(prec+5,newContext.getRoundingMode());
        double_digits = new MathContext(prec*2,newContext.getRoundingMode());
        eps = BigDecimal.ONE.scaleByPowerOfTen(-(prec + 3));
        return true;
    }

    // constructors
    public MathExtra1(MathContext mcontext) throws IllegalArgumentException {
        if (!setCurrentContext(mcontext)) throw new IllegalArgumentException("Math Context invalid!");
    }
    public MathExtra1() {
        setCurrentContext(MathContext.DECIMAL64);
    }


    // testing if the relative difference is smaller than epsilon
    private static boolean itestDiff(final BigDecimal approx1, final BigDecimal approx2, final MathContext mc, final BigDecimal ieps) {
        BigDecimal diff = approx2.subtract(approx1).divide(approx2,mc).abs();
        return (diff.compareTo(ieps) < 0);
    }

    public boolean testDifference(@NonNull BigDecimal approx1, @NonNull BigDecimal approx2) {
        return itestDiff(approx1,approx2,extra_digits,eps);
    }

    // calculating square root using Newton's method
    private static BigDecimal isqrt(final BigDecimal operand, MathContext mathc, BigDecimal ieps) {
        if (operand.signum() < 0) return null; // we don't do complex numbers here
        if (operand.signum() == 0) return BigDecimal.ZERO; // special case
        // approximation
        double dapp;
        // using doubles to get a good initial approximation (which might be a perfect one)
        if (MINFLT.compareTo(operand) >= 0) dapp = Math.sqrt(Double.MIN_NORMAL);
        else if (MAXFLT.compareTo(operand) <= 0) dapp = Math.sqrt(Double.MAX_VALUE);
        else dapp = Math.sqrt(operand.doubleValue());
        BigDecimal approx = new BigDecimal(dapp);
        BigDecimal old_approx; // empty for now
        // we set up a maximum iteration counter (just in case)
        int limcount = 0;
        // the new approximation loop
        while (limcount < 100) {
            limcount++;
            old_approx = approx;
            // calculating the (hopefully) better approximation is simple
            approx = old_approx.add(operand.divide(old_approx,mathc)).divide(TWO,mathc);
            /* Knowing where to stop is more complex: we break if the relative difference between
            old and new approximations is small enough (smaller than ε × approx) */
            if (itestDiff(old_approx,approx,mathc,ieps)) break;
        }
        // done
        return approx;
    }

    public @Nullable BigDecimal sqrt(@NonNull final BigDecimal operand) {
        BigDecimal res = isqrt(operand,extra_digits,eps);
        if (res == null) return null;
        return res.round(primary).stripTrailingZeros();
    }

    // stored factorials
    static final BigDecimal[] factorials = new BigDecimal[21];
    static {
        factorials[0] = BigDecimal.ONE;
        factorials[1] = BigDecimal.ONE;
        factorials[2] = TWO;
        if (factorials.length > 3) {
            for (int fdex = 3; fdex < factorials.length; fdex++) {
                factorials[fdex] = factorials[fdex-1].multiply(new BigDecimal(fdex));
            }
        }
    }

    // factorial function
    @Nullable static BigDecimal factorial(int n) {
        if (n < 0) return null; // undefined
        else if (n < factorials.length) return factorials[n];
        else {
            BigDecimal prod = factorials[factorials.length-1];
            for (int pdex = factorials.length; pdex <= n; pdex++) {
                prod = prod.multiply(new BigDecimal(pdex));
            }
            return prod;
        }
    }

    public @NonNull BigDecimal exp(@NonNull BigDecimal n) {
        BigDecimal res = iexp(n,extra_digits,eps);
        return res.round(primary).stripTrailingZeros();
    }

    // internal calculate eⁿ
    private static BigDecimal iexp(BigDecimal n, MathContext mathc, BigDecimal ieps) {
        if (n.signum() < 0) return BigDecimal.ONE.divide(iexp(n.negate(),mathc,ieps),mathc);
        else if (n.signum() == 0) return BigDecimal.ONE;  // trivial special case
        else if (n.equals(BigDecimal.ONE)) return EC;     // another special case
            // n is bigger than powInt can handle, we split it (kinda useless)
        else if (n.compareTo(MAXPOW) > 0) {
            n = n.subtract(MAXPOW);
            return EC.pow(max_pow_integer,mathc).multiply(iexp(n,mathc,ieps),mathc);
        }
        // we split n into integer and fraction parts
        else if (n.compareTo(BigDecimal.ONE) > 0) {
            int intpart = n.intValue();
            BigDecimal fracval = n.subtract(BigDecimal.valueOf(intpart));
            if (fracval.signum() <= 0) return EC.pow(intpart);
            else return EC.pow(intpart,mathc).multiply(expTaylor(fracval,mathc,ieps),mathc);
        }
        // finally, we calculate using the taylor series
        else return expTaylor(n,mathc,ieps);
    }

    // calculate eⁿ using Taylor series (Private, perhaps needs optimization)
    private static BigDecimal expTaylor(BigDecimal x, MathContext mathc, BigDecimal ieps) {
        // setting up the values and variables in preparation for the loop
        BigDecimal old_approx = BigDecimal.ONE.add(x);
        BigDecimal approx = null; // approx must be initialized to stop incorrect complaints (from android studio)
        BigDecimal q = x;
        int n = 2;
        while (n <= 102) {
            // calculating the numerator
            q = q.multiply(x,mathc);
            // computing the new approximation
            approx = old_approx.add((q.divide(factorial(n),mathc)));
            // checking if the approximation is good enough (start checking after 10 iterations)
            if ((n > 10) && itestDiff(approx, old_approx,mathc,ieps)) break;
            n++;
            old_approx = approx;
        }
        return approx;
    }

    /* x might be represented as a×10ⁿ, this function tries to find a good n. Positive x only! */
    static int findPowerTen(BigDecimal x) {
        if (x.signum() < 1) throw new ArithmeticException("findScale: x must be positive!");
        return (x.precision() - x.scale() - 1); // I'm not certain this always works
    }

    // natural log
    public @NonNull BigDecimal ln(@NonNull BigDecimal x) throws ArithmeticException {
        if (x.signum() < 1) throw new ArithmeticException("ln(x) is undefined for x < 0!");
        BigDecimal res = iln(x,extra_digits,eps);
        return res.round(primary).stripTrailingZeros();
    }

    // ln, with some tricks
    private static BigDecimal iln(BigDecimal x, final MathContext mathc, BigDecimal ieps) {
        final int nscale = findPowerTen(x);
        if (nscale == 0) return newtonLN(x,mathc,ieps);
        else {
            BigDecimal scaler = LN10.multiply(BigDecimal.valueOf(nscale));
            if (nscale < 0) x = x.movePointRight(-nscale);
            else x = x.movePointLeft(nscale);
            BigDecimal tres = newtonLN(x,mathc,ieps);
            return scaler.add(tres);
        }
    }

    // uses newton's method for calculating ln x in the range 1 to 10
    private static BigDecimal newtonLN(BigDecimal x, final MathContext mathc, final BigDecimal ieps) {
        // we get an approximation first
        double dapprox = Math.log(x.doubleValue());
        BigDecimal old_approx = BigDecimal.valueOf(dapprox);
        BigDecimal approx = null,exp_prox,interm;
        int loopc = 0;
        while (loopc < 100) {
            exp_prox = iexp(old_approx,mathc,ieps);
            interm = x.subtract(exp_prox).divide(x.add(exp_prox),mathc);
            approx = old_approx.add(TWO.multiply(interm));
            if (itestDiff(old_approx,approx,mathc,ieps)) break;
            loopc++;
        }
        return approx;
    }


}
