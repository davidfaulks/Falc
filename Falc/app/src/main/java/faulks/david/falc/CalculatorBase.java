package faulks.david.falc;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Button;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.MathContext;

public abstract class CalculatorBase extends AppCompatActivity implements NumPad.NumPadCallback {



    //++++++++++++++++++++++++++++++++++++++++++++++++++++
    // pointers to common display elements
    protected TextView pendingDisplay;
    protected NumDisplay mainDisplay;
    protected NumPad numberPad;

    protected Button addButton;
    protected Button subButton;
    protected Button divButton;
    protected Button mulButton;
    protected Button memStoreButton;
    protected Button backspaceButton;
    //+++++++++++++++++++++++++++++++++++++++++++++++++++
    // private working data
    protected MathContext drmode = MathContext.DECIMAL64;
    protected MathExtra1 fancy = new MathExtra1(drmode);
    // binary operations are '+', '-', '*', and '/'. 'n' means nothing
    protected char binary_op = 'n';
    protected BigDecimal first_operand;
    protected BigDecimal op_result;
    protected String opfail_msg;
    protected boolean replace = false; // set to true when the number is a result, convention
    // also
    protected FormatStore.FormatTypes current_type;
    //--------------------------------------------------
    // saving and restoring member variables
    private final String DMTAG = "drmode_tag";
    private final String BINOPTAG = "binaryop_tag";
    private final String FOPTAG = "firstoperand_tag";
    private final String FAILTAG = "opfailmsg_tag";
    private final String REPTAG = "replace_tag";
    private final String FTTAG = "formattype_tag";

    @Override protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(DMTAG,(Serializable) drmode);
        outState.putBoolean(REPTAG,replace);
        outState.putSerializable(FTTAG,current_type);
        if (binary_op != 'n') {
            outState.putChar(BINOPTAG,binary_op);
            outState.putSerializable(FOPTAG,first_operand);
        }
        else if ((opfail_msg != null) && (!opfail_msg.isEmpty())) {
            outState.putString(FAILTAG,opfail_msg);
        }
    }

    @Override protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        MathContext mcmode = (MathContext) savedInstanceState.getSerializable(DMTAG);
        if (mcmode == null) return;
        FormatStore.FormatTypes fctemp = (FormatStore.FormatTypes) savedInstanceState.getSerializable(FTTAG);
        if (fctemp == null) return;
        drmode = mcmode;
        current_type = fctemp;
        replace = savedInstanceState.getBoolean(REPTAG,false);
        char qbinop = savedInstanceState.getChar(BINOPTAG,'n');
        if (qbinop != 'n') {
            BigDecimal testfop = (BigDecimal) savedInstanceState.getSerializable(FOPTAG);
            if (testfop == null) return;
            binary_op = qbinop;
            first_operand = testfop;
        }
        else {
            String tfail_msg = savedInstanceState.getString(FAILTAG);
            if (tfail_msg != null) opfail_msg = tfail_msg;
        }
    }

    //--------------------------------------------------
    // some GUI setup methods that assume layout IDs are the same
    // setting up the + - ÷ × buttons
    protected void setupBinaryButtons() {
        addButton = (Button) findViewById(R.id.BC_ADD);
        subButton = (Button) findViewById(R.id.BC_SUBT);
        mulButton = (Button) findViewById(R.id.BC_MULT);
        divButton = (Button) findViewById(R.id.BC_DIV);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                BinaryButtonPressed('+');
            }
        });
        subButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                BinaryButtonPressed('-');
            }
        });
        mulButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                BinaryButtonPressed('*');
            }
        });
        divButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                BinaryButtonPressed('/');
            }
        });
    }
    // memory store is a common function
    protected void setupMemStoreButton() {
        memStoreButton = (Button) findViewById(R.id.BC_MEMSTORE);
        memStoreButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                mainDisplay.memoryStore();
            }
        });
    }
    // backspace display is also common
    protected void setupBackButton() {
        backspaceButton = (Button) findViewById(R.id.BC_BACK);
        backspaceButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                if (!replace) mainDisplay.removeDigit();
            }
        });
    }
    //+++++++++++++++++++++++++++++++++++++++++++++++++++
    // sets the stored decimal and the op, also sets the pending display
    protected void SetPending(BigDecimal newPending, String pendingString, char newop) {
        // I don't check the validilty of the parameters
        first_operand = newPending;
        binary_op = newop;
        // converting to String
        char xnop = newop;
        if (xnop == '-') xnop = '−';
        else if (xnop == '/') xnop = '÷';
        else if (xnop == '*') xnop = '×';
        pendingDisplay.setText(pendingString + " " + xnop);
    }
    protected void clearPending() {
        binary_op = 'n';
        pendingDisplay.setText("");
        first_operand = null;
    }

    // handles calculating a binary operation
    protected boolean DoBinaryOperation(boolean percent) throws ArithmeticException {
        // if this ever happens, it is a programmer error
        if (binary_op == 'n') {
            opfail_msg = "No Binary operation Specified!";
            return false;
        }
        // the usual case...
        else {
            BigDecimal secondOperand = mainDisplay.getDisplayValue();
            if (percent) {
                secondOperand = secondOperand.scaleByPowerOfTen(-2);
                secondOperand = first_operand.multiply(secondOperand,drmode);
            }
            try {
                if (binary_op == '+') {
                    op_result = first_operand.add(secondOperand, drmode);
                } else if (binary_op == '-') {
                    op_result = first_operand.subtract(secondOperand, drmode);
                } else if (binary_op == '*') {
                    op_result = first_operand.multiply(secondOperand, drmode);
                } else if (binary_op == '/') {
                    if (0 == (secondOperand.compareTo(BigDecimal.ZERO))) {
                        opfail_msg = "Divide by Zero Error!";
                        return false;
                    } else {
                        op_result = first_operand.divide(secondOperand, drmode);
                    }
                } else throw new ArithmeticException("Unknown binary operation!");
            }
            catch (ArithmeticException e){
                opfail_msg = e.getLocalizedMessage();
                return false;

            }
            // if we get here, things are okay
            return true;
        }
    }

    // Generic 'Binary Operator Button Pressed'
    protected boolean BinaryButtonPressed(char newop) {
        boolean opres = true;
        // np pending operation
        if (binary_op == 'n') {
            BigDecimal secondOperand = mainDisplay.getDisplayValue();
            String secondString = mainDisplay.getDisplayString();
            SetPending(secondOperand,secondString,newop);
        }
        // there is a pending operation
        else {
            opres = DoBinaryOperation(false);
            String opresStr = FormatStore.format(op_result);
            if (opres)  SetPending(op_result,opresStr,newop);
            else {
                // I've decided for now, on error, everything is cancelled
                pendingDisplay.setText(opfail_msg);
                binary_op = 'n';
            }
        }
        mainDisplay.zeroDisplay();
        return opres;
    }
    //+++++++++++++++++++++++++++++++++++++++++++++++++++
    // if the format has changed, we rebuild various values
    protected void doOnFormatChange() {
        if (binary_op != 'n') {
            String pendingString = FormatStore.format(first_operand);
            SetPending(first_operand,pendingString,binary_op);
        }
        numberPad.setLabelsFromFormatStore();
        mainDisplay.rebuildFormattedStrings();
    }
    //++++++++++++++++++++++++++++++++++++++++++++++++++++
    // number pad callback routines
    public void nzDigitPressed(int digit) {
        if (replace) mainDisplay.replaceDisplay(digit);
        else mainDisplay.addDigit(digit);
        replace = false;
    }
    public void zeroPressed() {
        if (replace) mainDisplay.zeroDisplay();
        else mainDisplay.addDigit(0);
        replace = false;
    }
    public void decimalSeparatorPressed() {
        if (!replace) mainDisplay.addDecimalSeparator();
    }
    abstract public void thirdButtonPressed();

}
