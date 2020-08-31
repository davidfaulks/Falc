package faulks.david.falc;


import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.util.Log;

/* A number pad is the most standardised part of a calculator layout, so I separate it here, where
it can be plugged into a variety of calculator activities.
This num pad assumes digits 0 to 9 as single characters (which ones can vary based on script)
A decimal point must also exit. The 'third' button (generally found on the bottom row) may be defined
with a label.
*/
public class NumPad extends Fragment {

    // the callback target will be whatever embeds this NumPanel
    interface NumPadCallback {
        // used to pass button presses back to the activity
        void nzDigitPressed(int digit); // non-zero digit pressed
        void zeroPressed();  // zero usually requres special handling
        void decimalSeparatorPressed();
        void thirdButtonPressed();
    }

    private NumPadCallback callbackTarget = null;
    // when the panel is embedded in an activity, we make the activity the callback target
    @Override public void onAttach(Context context) {
        super.onAttach(context);
        Log.i("NumPad","onAttach A");
        if (context instanceof NumPadCallback) {
            Log.i("NumPad","onAttach B");
            callbackTarget = (NumPadCallback) context;
        }
        else {
            Log.i("NumPad","onAttach C");
            throw new RuntimeException(context.toString() + " has to implement NumPadCallback");
        }
    }

    @Override public void onDetach() {
        super.onDetach();
        callbackTarget = null;
    }
    //-------------------------------------------------------

    public NumPad() {
        // Required empty public constructor
    }
    // string keys used for bundle data storage
    static final String THIRDLABEL_TAG = "ThirdOpLabel";
    static final String LAYOUT_TAG = "Layout";

    // factory method.
    // I want to allow the button labels to be changable, so they are something that can be passed as a parmeter
    // array positions are 0 to 9, for digits 0 to 9
    public static NumPad makeNumPad(int layout_rid, @Nullable String third_label) {
        NumPad newPad = new NumPad();
        Bundle args = new Bundle();
        args.putInt(LAYOUT_TAG,layout_rid);
        if (third_label != null) args.putString(THIRDLABEL_TAG,third_label);
        newPad.setArguments(args);
        return newPad;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // we get the bundle arguments and the resource is for the layout
        Bundle args = getArguments();
        int rlid = args.getInt(LAYOUT_TAG,-1);
        if (rlid < 0) throw new IllegalArgumentException("NumPad layout id not found!");
        // Inflate the layout for this fragment
        View mv = inflater.inflate(rlid, container, false);
        // getting the Digit Button pointers
        digitButtons[0] = (Button) mv.findViewById(R.id.NBUTN0);
        digitButtons[1] = (Button) mv.findViewById(R.id.NBUTN1);
        digitButtons[2] = (Button) mv.findViewById(R.id.NBUTN2);
        digitButtons[3] = (Button) mv.findViewById(R.id.NBUTN3);
        digitButtons[4] = (Button) mv.findViewById(R.id.NBUTN4);
        digitButtons[5] = (Button) mv.findViewById(R.id.NBUTN5);
        digitButtons[6] = (Button) mv.findViewById(R.id.NBUTN6);
        digitButtons[7] = (Button) mv.findViewById(R.id.NBUTN7);
        digitButtons[8] = (Button) mv.findViewById(R.id.NBUTN8);
        digitButtons[9] = (Button) mv.findViewById(R.id.NBUTN9);
        decimalSeparatorButton = (Button) mv.findViewById(R.id.NBUTNDEC);
        thirdOpButton = (Button) mv.findViewById(R.id.NBUTNT); // okay if null
        // setting up the labels and listeners for digit buttons
        for (int ldex = 0; ldex < 10; ldex++) {
            digitButtons[ldex].setText(String.valueOf(FormatStore.getDigit(ldex))); // null pointer won't happen
            if (ldex != 0) digitButtons[ldex].setOnClickListener(new BtnClickListen(ldex));
        }
        digitButtons[0].setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                NumPad.this.callbackTarget.zeroPressed();
            }
        });
        // setting up the label and listener for the decimal separator button
        decimalSeparatorButton.setText(String.valueOf(FormatStore.getSymbols().getDecimalSeparator()));
        decimalSeparatorButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                NumPad.this.callbackTarget.decimalSeparatorPressed();
            }
        });
        // finally, there might be a third button
        if (thirdOpButton != null) {
            third_button_label = args.getString(THIRDLABEL_TAG,null);
            if (third_button_label == null) throw new IllegalArgumentException("Third Op Label not found!");
            thirdOpButton.setText(third_button_label);
            thirdOpButton.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    NumPad.this.callbackTarget.thirdButtonPressed();
                }
            });
        }

        return mv;
    }
    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    // sets the digit labels and decimal separator label from the current format store
    public void setLabelsFromFormatStore() {
        // setting up labels for digit buttons
        for (int ldex = 0; ldex < 10; ldex++) {
            digitButtons[ldex].setText(String.valueOf(FormatStore.getDigit(ldex))); // null pointer won't happen
        }
        // setting up the label and listener for the decimal separator button
        decimalSeparatorButton.setText(String.valueOf(FormatStore.getSymbols().getDecimalSeparator()));
    }
    //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    // private button pointers
    private Button[] digitButtons = new Button[10];
    private Button decimalSeparatorButton;
    private @Nullable Button thirdOpButton = null;
    // button labels
    private @Nullable String third_button_label = null;

    // a click listener type for the button presses
    private class BtnClickListen implements View.OnClickListener {
        final int which;
        public BtnClickListen(int buttonIndex) {
            which = buttonIndex;
        }
        @Override public void onClick(View view) {
            NumPad.this.callbackTarget.nzDigitPressed(which);
        }
    }

    //-----------------------------------------------------------------
    // for adjusting font size programmatically for the digits
    public void setDigitFontSize(int unit, float size) {
        for (Button current : digitButtons) {
            current.setTextSize(unit,size);
        }
    }
    // adjusting background and text color programmatically for the digits
    public void setDigitBackgroundTextColor(int resource_id, int color_int) {
        for (Button current : digitButtons) {
            current.setBackgroundResource(resource_id);
            current.setTextColor(color_int);
        }
    }
    // adjusting font size programmatically for the other two buttons
    // if dec is true, the decimal separator button is the target, otherwise it is the 'ThirdOp' button
    public boolean setOtherFontSize(boolean dec,int unit, float size) {
        if (dec) decimalSeparatorButton.setTextSize(unit,size);
        else {
            if (thirdOpButton != null) thirdOpButton.setTextSize(unit,size);
            else return false;
        }
        return true;
    }


}
