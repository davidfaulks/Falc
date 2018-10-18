package faulks.david.falc;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.math.BigDecimal;
import java.math.MathContext;

/* Big Decimal is lacking in some operations. Also, some constants */

public class MathExtra1 {

    // calculating square root using Newton's method
    @Nullable static BigDecimal sqrt(@NonNull final BigDecimal operand, @NonNull final MathContext ccont) {
        if (operand.signum() < 0) return null; // we don't do complex numbers here
        if (operand.signum() == 0) return BigDecimal.ZERO; // special case
        // using doubles to get a good initial approximation (which might be a perfect one)
        final double dapp = Math.sqrt(operand.doubleValue());
        BigDecimal approx = new BigDecimal(dapp,ccont);
        BigDecimal old_approx;
        // setting up a new math context with more digits
        final MathContext econt = new MathContext(ccont.getPrecision()+3,ccont.getRoundingMode());
        // precision difference limit
        final BigDecimal ε = new BigDecimal(Math.pow(10,-ccont.getPrecision()+1),econt);
        // some quick helpers
        final BigDecimal TWO = BigDecimal.valueOf(2);
        int compx,limcount = 0;
        BigDecimal difference,meps;
        // the new approximation loop, with a maximum limit just in case
        while (limcount < 50) {
            limcount++;
            old_approx = approx;
            // calculating the (hopefully) better approximation is simple
            approx = approx.add(operand.divide(approx, econt), econt).divide(TWO, econt);
            /* Knowing where to stop is more complex: we break if the relative difference between
            old and new approximations is small enough (smaller than ε × approx) */
            difference = approx.subtract(old_approx).abs();
            compx = difference.compareTo(approx.multiply(ε,econt));
            if (compx < 0) break;
        }
        // done
        return approx.round(ccont);
    }


}
