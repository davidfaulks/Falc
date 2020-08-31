package faulks.david.falc;

import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Button;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.content.Intent;
import java.math.BigDecimal;

public class BasicCalc extends CalculatorBase {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_basic_calc);
        // linking the pending display
        pendingDisplay = (TextView) findViewById(R.id.BC_PENDING);
        // getting the fragments
        mainDisplay = (NumDisplay) getSupportFragmentManager().findFragmentById(R.id.BC_DISPLAY);
        numberPad = makeBasicNumpad();
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.BC_NUMPAD,numberPad);
        ft.commit();
        // linking some of the basic buttons
        setupBinaryButtons();
        setupMemStoreButton();
        setupBackButton();
        percentButton = (Button) findViewById(R.id.BC_PCT);
        percentButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                doEquals(true);
            }
        });
        memAddButton = (Button) findViewById(R.id.BC_MEMADD);
        memAddButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                mainDisplay.memorySum(true);
            }
        });
        memSubButton = (Button) findViewById(R.id.BC_MEMSUB);
        memSubButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                mainDisplay.memorySum(false);
            }
        });
        sqrtButton = (Button) findViewById(R.id.BC_SQRT);
        sqrtButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                doSquareRoot();
            }
        });
        clearButton = (Button) findViewById(R.id.BC_CLEAR);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                doClear();
            }
        });
    }

    @Override public void onResume() {
        super.onResume();
        if (current_type != FormatStore.getType()) {
            current_type = FormatStore.getType();
            doOnFormatChange();
        }
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }
    //------------------------------------------------------------------
    @Override public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.preferences) {
            Intent target = new Intent(BasicCalc.this,SettingsActivity.class);
            startActivity(target);
            return true;
        }
        else return super.onOptionsItemSelected(item);
    }

    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    // private widget pointers
    private Button percentButton;
    private Button memAddButton;
    private Button memSubButton;
    private Button sqrtButton;
    private Button clearButton;


    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    // for the basic calculator, the '3rd Button' on the NumPad is =
    @Override public void thirdButtonPressed() {
        doEquals(false);
    }
    //--------------------------------------------
    private void doEquals(boolean percent) {
        boolean done = DoBinaryOperation(percent);
        if (done) {
            mainDisplay.setDisplay(op_result,false);
            clearPending();
        }
        else pendingDisplay.setText(opfail_msg);
        binary_op = 'n';
        replace = true;
    }
    private void doSquareRoot() {
        BigDecimal mainOperand = mainDisplay.getDisplayValue();
        BigDecimal result = fancy.sqrt(mainOperand);
        if (result == null) {
            opfail_msg = "Square root of a negative number!";
            pendingDisplay.setText(opfail_msg);
            binary_op = 'n';
        }
        else {
            mainDisplay.setDisplay(result,false);
            replace = true;
        }
    }
    private boolean doClear() {
        clearPending();
        replace = false;
        mainDisplay.zeroDisplay();
        return true;
    }
    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    // creating the number pad
    private NumPad makeBasicNumpad() {
        final int layout = R.layout.fragment_num_pad;
        return NumPad.makeNumPad(layout,"=");
    }
}
