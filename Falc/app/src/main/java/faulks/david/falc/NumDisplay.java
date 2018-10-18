package faulks.david.falc;


import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.io.Serializable;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.ArrayList;
import java.math.BigDecimal;
import java.util.regex.Matcher;

/* Includes a spinner, which displays the current number as well as the memory. Also has associated
external methods for manipulating the contents. */
public class NumDisplay extends Fragment implements AdapterView.OnItemSelectedListener {
    private static final String BD_ARRAY = "BigDecimalArray";
    private static final String TOP_STRING = "TopStringStored";

    public NumDisplay() {
        // Required empty public constructor
    }
    //--------------------------------------------------------------------------
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View mv = inflater.inflate(R.layout.fragment_num_display, container, false);
        mainDisplay = (Spinner) mv.findViewById(R.id.NUMDISPLAY_DisplaySpinner);
        // the store starts off with a single zero item
        dataStore.add(String.valueOf(FormatStore.getSymbols().getZeroDigit()));
        numberStore.add(BigDecimal.ZERO);
        topString = new SplitNumberString(FormatStore.getSymbols());

        spinAdapter = new ArrayAdapter<String>(getContext(),R.layout.calc_display_item,R.id.DV_NUMDISPLAY,dataStore);
        mainDisplay.setAdapter(spinAdapter);
        mainDisplay.setOnItemSelectedListener(this);
        return mv;
    }

    @Override public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            // getting the saved data (if there)
            String storedTopString = savedInstanceState.getString(TOP_STRING);
            ArrayList<BigDecimal> elist = (ArrayList<BigDecimal>) savedInstanceState.getSerializable(BD_ARRAY);
            if ((storedTopString == null) || (elist == null)) return;
            // moving it into the display
            numberStore = elist;
            dataStore.clear();
            for (BigDecimal cnum : numberStore) {
                dataStore.add(FormatStore.format(cnum));
            }
            // handling the top, which has special formatting
            topString.setFromNumberString(storedTopString);
            String formattedTop = FormatStore.insertGroupSeparators(storedTopString);
            dataStore.set(0,formattedTop);
            // done
            spinAdapter.notifyDataSetChanged();
        }
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(BD_ARRAY,(Serializable) numberStore);
        outState.putString(TOP_STRING,topString.buildNumberString());
    }
    //++++++++++++++++++++++++++++++++++++++++++++++++++++
    // if the format has changed, we must rebuild the strings
    public void rebuildFormattedStrings() {
        String cnum;
        for (int ndex = 0; ndex <= getCurrentMemorySize(); ndex++) {
            cnum = FormatStore.format(numberStore.get(ndex));
            dataStore.set(ndex,cnum);
        }
        topString.setFromNumberString(dataStore.get(0));
        spinAdapter.notifyDataSetChanged();
    }

    //++++++++++++++++++++++++++++++++++++++++++++++++++++
    // spinner and 'memory'
    private Spinner mainDisplay;
    private ArrayAdapter<String> spinAdapter;
    //--------------
    // stores the formatted string numbers for the 'display' and drop-down 'memory'
    private List<String> dataStore = new ArrayList<String>(6);
    // stores the actual numbers for the 'display' and drop-down 'memory'
    private List<BigDecimal> numberStore = new ArrayList<BigDecimal>(6);
    private int maxMemorySize = 6;
    // a 'split' string version of the currently displayed number is stored here, to edit it more easily
    private SplitNumberString topString = null;
    //---------------------------------
    // basic memory manipulations
    public int getCurrentMemorySize() { return (numberStore.size() - 1); }
    public int getMaxMemorySize() { return maxMemorySize; }

    public int setMaxMemorySize(int new_max_memory_size) throws IllegalArgumentException {
        if (new_max_memory_size < 0) throw new IllegalArgumentException("Negative memory size specified!");
        maxMemorySize = new_max_memory_size;
        int result;
        if ((new_max_memory_size+1) >= dataStore.size()) result = 0; // no need to delete anything
        else result = clearMemFrom(new_max_memory_size+1); // delete using clearMemFrom
        spinAdapter.notifyDataSetChanged(); // updating the display
        return result; // result is number of items deleted
    }
    //-----------------------------------------------------------
    /* When the user selects a memory item to use, does it get swapped with the display, or
    is the display value just pushed on top of the memory 'stack?' */
    public boolean swapSelected = false;
    //-----------------------------------------------------------
    // clears all memory items from index small_index onwards...
    private int clearMemFrom(int small_index) throws IllegalArgumentException {
        if (small_index < 1) throw new IllegalArgumentException("Smallest index must be 1 or more!");
        if (small_index >= dataStore.size()) return 0;
        final int ramount = (dataStore.size() - small_index);
        dataStore.subList(small_index,dataStore.size()).clear();
        numberStore.subList(small_index,numberStore.size()).clear();
        return ramount;
    }
    /* Shifts memory locations further in list, up to a certain index. Returns false if an item
       is disarded in doing do. */
    private boolean shiftMemoryDown(int to_index) throws IllegalArgumentException {
        if ((to_index < 1) || (to_index > dataStore.size())) {
            throw new IllegalArgumentException("Parameter to_index has an invalid value: " + to_index);
        }
        final boolean to_end = (to_index == dataStore.size());
        boolean no_discard = false;
        // in some cases, we will need to shift the last item into a non-existing position
        if (to_end && (maxMemorySize > getCurrentMemorySize()) ) {
            dataStore.add(dataStore.get(getCurrentMemorySize()));
            numberStore.add(numberStore.get(numberStore.size()-1));
            to_index--;
            no_discard = true;
        }
        // we then loop backwards from to_index to 1, copying pointers from the preceeding item
        for (int cpindex = to_index; cpindex >= 1; cpindex--) {
            dataStore.set(cpindex,dataStore.get(cpindex-1));
            numberStore.set(cpindex,numberStore.get(cpindex-1));
        }
        // done
        return no_discard;
    }
    /* Shifts memory locations up in the list, which will result in a value being discarded (if
       not saved beforehand). */
    private boolean shiftMemoryUp(int to_index) throws IllegalArgumentException {
        if ((to_index < 0) || (to_index >= (getCurrentMemorySize()))) {
            throw new IllegalArgumentException("Parameter to_index has an invalid value: " + to_index);
        }
        final boolean display_changed = (to_index == 0);
        // copying pointers to the previous list positions
        for (int cpindex = to_index; cpindex < getCurrentMemorySize(); cpindex++) {
            dataStore.set(cpindex,dataStore.get(cpindex+1));
            numberStore.set(cpindex,numberStore.get(cpindex+1));
        }
        // removing the last arraylist elements (which are now duplicats of
        dataStore.remove(dataStore.size()-1);
        numberStore.remove(numberStore.size()-1);
        // if the shift involves changing item 0 (the display), we also set the 'topString' object
        if (display_changed) topString.setFromNumberString(dataStore.get(0));
        // done
        return display_changed;
    }

    // Generic push onto memory (MS). Returns false if the memory is full, resulting in a deleted item
    public boolean memoryStore() {
        final boolean no_delete = shiftMemoryDown(dataStore.size());
        spinAdapter.notifyDataSetChanged();
        return no_delete;
    }
    // moves the first memory item into the display, erasing what was there before (MR)
    // since you can also select from the spinner, this is redundant
    public void memoryRetrieve() {
        shiftMemoryUp(0);
        spinAdapter.notifyDataSetChanged();
    }
    // zeroes out the display
    public void zeroDisplay() {
        numberStore.set(0,BigDecimal.ZERO);
        dataStore.set(0,String.valueOf(FormatStore.getDigit(0)));
        topString = new SplitNumberString(FormatStore.getSymbols());
        spinAdapter.notifyDataSetChanged();
    }
    // replaces the display with a digit
    public void replaceDisplay(int digit) throws IllegalArgumentException {
        if ((digit < 0) || (digit > 9)) throw new IllegalArgumentException("Paramater digit must be 0 to 9!");
        String strValue = String.valueOf(FormatStore.getDigit(digit));
        numberStore.set(0,BigDecimal.valueOf(digit));
        dataStore.set(0,strValue);
        topString.setFromNumberString(strValue);
        spinAdapter.notifyDataSetChanged();
    }
    /* For adding or subtracting the display value to memory (index 1). If there is nothing in
    memory, the calculation will use 0 as a standin (shortcut special cases). */
    public void memorySum(boolean addValues) {
        // add
        if (addValues) {
            if (getCurrentMemorySize() == 0) {
                numberStore.add(numberStore.get(0));
                dataStore.add(dataStore.get(0));
            }
            else {
                numberStore.set(1,numberStore.get(0).add(numberStore.get(1)));
                dataStore.set(1,FormatStore.format(numberStore.get(1)));
            }
        }
        // subtract
        else {
            if (getCurrentMemorySize() == 0) {
                numberStore.add(numberStore.get(0).negate());
                dataStore.add(FormatStore.format(numberStore.get(1)));
            }
            else {
                numberStore.set(1,numberStore.get(0).subtract(numberStore.get(1)));
                dataStore.set(1,FormatStore.format(numberStore.get(1)));
            }
        }
        spinAdapter.notifyDataSetChanged();
    }

    /* setting the display (position 0) to a BigDecimal value (presumably the result of some computation) */
    public void setDisplay(@NonNull BigDecimal newValue, boolean push){
        if (push) shiftMemoryDown(dataStore.size());
        numberStore.set(0,newValue);
        dataStore.set(0,FormatStore.format(numberStore.get(0)));
        topString.setFromNumberString(dataStore.get(0));
        spinAdapter.notifyDataSetChanged();
    }

    /* gets the Big Decimal in the display (position 0) */
    public BigDecimal getDisplayValue() {
        return numberStore.get(0);
    }
    public String getDisplayString() {
        return dataStore.get(0);
    }

    //--------------------------------------------------------------------
    // ** Manipulations of the currently displayed number

    /* Converts 'topString' to a Big Decimal, and then sticks it and the formatted version in the
    display-memory at index 0. Reloads the display. */
    private void commitTopString() {
        String builtString = topString.buildNumberString();
        String formattedTop = FormatStore.insertGroupSeparators(builtString);
        BigDecimal newTop = FormatStore.parse(formattedTop);
        numberStore.set(0,newTop);
        dataStore.set(0,formattedTop);
        spinAdapter.notifyDataSetChanged();
    }
    //-------------------------------------------

    // appending a decimal digit.
    public void addDigit(int which) throws IllegalArgumentException {
        if ((which < 0) || (which >= 10)) throw new IllegalArgumentException("Parameter which must be 0 to 9 inclusive!");
        if (!topString.appendDigit(which)) return;
        // load the new string
        commitTopString();
    }
    // backspace
    public boolean removeDigit() {
        final boolean res = topString.backspace();
        commitTopString();
        return res;
    }

    // add decimal separator. This method does nothing if there is already one
    public boolean addDecimalSeparator() {
        if (!topString.addDecimalSeparator()) return false;
        commitTopString();
        return true;
    }

    // adding the exponent separator. does nothing if the number is zero or we already have one
    public boolean addExponentSeparator() {
        if (!topString.addExponent()) return false;
        commitTopString();
        return true;
    }

    // toggle negative. applies to just the exponent if hasExponent is true
    public boolean toggleNegative() {
        if (!topString.toggleSign()) return false;
        commitTopString();
        return true;
    }
    //---------------------------------------------
    // since we use a spinner for memory, we select memory items using the OnItemSelectedListener
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        if (pos == 0) return;
        Log.i("Num Display","OnItemSelected A");
        // we get the selected item
        BigDecimal pick_num = numberStore.get(pos);
        String pick_disp = dataStore.get(pos);
        Log.i("Num Display","OnItemSelected B: " + pick_disp);
        // swap or push? if pos == 1, they are the same
        if ((pos==1) || swapSelected) {
            // the swap case
            numberStore.set(pos,numberStore.get(0));
            dataStore.set(pos,dataStore.get(0));
        }
        else {
            // the push case
            shiftMemoryDown(pos);
        }
        // sticking selected things at 0
        numberStore.set(0,pick_num);
        dataStore.set(0,pick_disp);
        topString.setFromNumberString(pick_disp);
        // done
        mainDisplay.setSelection(0);
        spinAdapter.notifyDataSetChanged();
    }

    public void onNothingSelected(AdapterView<?> parent) {

    }


}
