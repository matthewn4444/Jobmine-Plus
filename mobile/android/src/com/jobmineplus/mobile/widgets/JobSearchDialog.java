package com.jobmineplus.mobile.widgets;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import com.jobmineplus.mobile.R;
import com.jobmineplus.mobile.exceptions.JbmnplsParsingException;

public class JobSearchDialog extends Builder implements
                    android.content.DialogInterface.OnClickListener, OnItemSelectedListener, OnShowListener {

    private final AlertDialog dialog;

    // =======================
    //  Variable Declaration
    // =======================
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy", Locale.getDefault());

    public static List<String> DISCIPLINES;
    public static List<String> DISCIPLINES_CODES;

    OnJobSearchListener listener;
    JobSearchProperties properties;

    Context ctx;

    Spinner disciplines1Spinner;
    Spinner disciplines2Spinner;
    Spinner disciplines3Spinner;

    Spinner termSpinner;

    Spinner locationSpinner;
    Spinner typeSpinner;
    Spinner filterSpinner;

    EditText employerText;
    EditText jobTitleText;

    CheckBox juniorChkbx;
    CheckBox intermediateChkbx;
    CheckBox seniorChkbx;
    CheckBox bachelorChkbx;
    CheckBox mastersChkbx;
    CheckBox phdChkbx;

    public JobSearchDialog(Context context) {
        super(context);
        ctx = context;

        setPositiveButton("Search", null);
        setNegativeButton(android.R.string.cancel, this);

        dialog = create();
        dialog.setOnShowListener(this);
        LayoutInflater inflater = dialog.getLayoutInflater();
        View view = inflater.inflate(R.layout.job_search_dialog, null);
        dialog.setView(view);

        // Sets the dialog with initial data
        disciplines1Spinner = (Spinner)view.findViewById(R.id.job_search_disciples1);
        disciplines2Spinner = (Spinner)view.findViewById(R.id.job_search_disciples2);
        disciplines3Spinner = (Spinner)view.findViewById(R.id.job_search_disciples3);
        termSpinner = (Spinner)view.findViewById(R.id.job_search_term);
        locationSpinner = (Spinner)view.findViewById(R.id.job_search_location);
        filterSpinner = (Spinner)view.findViewById(R.id.job_search_filter);
        typeSpinner = (Spinner)view.findViewById(R.id.job_search_type);

        employerText = (EditText)view.findViewById(R.id.job_search_employer);
        jobTitleText = (EditText)view.findViewById(R.id.job_search_title);

        juniorChkbx = (CheckBox)view.findViewById(R.id.job_search_junior);
        intermediateChkbx = (CheckBox)view.findViewById(R.id.job_search_intermediate);
        seniorChkbx = (CheckBox)view.findViewById(R.id.job_search_senior);
        bachelorChkbx = (CheckBox)view.findViewById(R.id.job_search_bachelor);
        mastersChkbx = (CheckBox)view.findViewById(R.id.job_search_masters);
        phdChkbx = (CheckBox)view.findViewById(R.id.job_search_phD);

        // Attach the events
        disciplines1Spinner.setOnItemSelectedListener(this);
        disciplines2Spinner.setOnItemSelectedListener(this);
        disciplines3Spinner.setOnItemSelectedListener(this);
        termSpinner.setOnItemSelectedListener(this);
        locationSpinner.setOnItemSelectedListener(this);
        filterSpinner.setOnItemSelectedListener(this);
        typeSpinner.setOnItemSelectedListener(this);

        if (DISCIPLINES == null) {
            DISCIPLINES = Arrays.asList(ctx.getResources().getStringArray(R.array.job_search_disciplines));
            DISCIPLINES_CODES = Arrays.asList(ctx.getResources()
                    .getStringArray(R.array.job_search_disciplines_values));
        }
    }

    public static String getDisciplineCodeFromName(String name) {
        int index = name.equals("") ? 0 : DISCIPLINES.indexOf(name);
        return DISCIPLINES_CODES.get(index);
    }

    public boolean isShowing() {
        return dialog.isShowing();
    }

    @Override
    public AlertDialog show() {
        dialog.show();
        return dialog;
    }

    public void setProperties(JobSearchProperties prop) {
        properties = prop;
        setSpinnerDefaultIfEmptyOrName(disciplines1Spinner, prop.disciplines1.get());
        setSpinnerDefaultIfEmptyOrName(disciplines2Spinner, prop.disciplines2.get());
        setSpinnerDefaultIfEmptyOrName(disciplines3Spinner, prop.disciplines3.get());

        setSpinnerDefaultIfEmptyOrName(locationSpinner, prop.location.get());
        setSpinnerSelectionByName(filterSpinner, prop.filter.get().toString());
        setJobTypeSpinner(prop.jobType.get());
        employerText.setText(prop.employer.get());
        jobTitleText.setText(prop.title.get());
        juniorChkbx.setChecked(prop.levelJunior.get());
        intermediateChkbx.setChecked(prop.levelIntermediate.get());
        seniorChkbx.setChecked(prop.levelSenior.get());
        bachelorChkbx.setChecked(prop.levelBachelors.get());
        mastersChkbx.setChecked(prop.levelMasters.get());
        phdChkbx.setChecked(prop.levelPhD.get());

        populateTermSpinner(prop.term.get());
    }

    public void setJobTypeSpinner(JobSearchProperties.JOBTYPE type) {
        setSpinnerSelectionByName(typeSpinner, type.toString());
    }

    public void setOnJobSearchListener(OnJobSearchListener jobListener) {
        listener = jobListener;
    }

    private void setSpinnerDefaultIfEmptyOrName(Spinner spinner, String name) {
        if (name.equals("")) {
            spinner.setSelection(0);
        } else {
            setSpinnerSelectionByName(spinner, name);
        }
    }

    private void setSpinnerSelectionByName(Spinner spinner, String name) {
        String findName = name.replaceAll(" ", "");
        for (int i = 0; i < spinner.getCount(); i++) {
            String posName = spinner.getItemAtPosition(i).toString().replaceAll(" ", "");
            if (posName.equalsIgnoreCase(findName)) {
                spinner.setSelection(i);
                return;
            }
        }
        throw new JbmnplsParsingException("Cannot find the name " + name + " in the spinner");
    }

    public void dismiss() {
        dialog.dismiss();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == Dialog.BUTTON_NEGATIVE) {
            listener.onCancel();

            // Reject any changes to the job type and set it back
            properties.jobType.rejectChange();
            setJobTypeSpinner(properties.jobType.get());
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String newText = null;
        switch(parent.getId()) {
        case R.id.job_search_type:
            newText = typeSpinner.getSelectedItem().toString();
            JobSearchProperties.JOBTYPE newType = JobSearchProperties.JOBTYPE.fromString(newText);
            properties.jobType.set(newType);
            if (properties.jobType.hasChanged()) {
                listener.onJobTypeChange(typeSpinner, newType);

                // Depending on which type is set, it will affect the checkboxes
                switch(newType) {
                case GRADUATING:
                case ALUMNI:
                case SUMMER:
                    juniorChkbx.setChecked(false);
                    intermediateChkbx.setChecked(false);
                    seniorChkbx.setChecked(false);
                    if (newType == JobSearchProperties.JOBTYPE.SUMMER) {
                        bachelorChkbx.setChecked(false);
                        mastersChkbx.setChecked(false);
                        phdChkbx.setChecked(false);
                    }
                    break;
                default:
                    break;
                }
            }
            break;
        case R.id.job_search_location:
            newText = position == 0 ? "" : locationSpinner.getSelectedItem().toString();
            properties.location.set(newText);
            break;
        case R.id.job_search_disciples1:
            newText = position == 0 ? "" : disciplines1Spinner.getSelectedItem().toString();
            properties.disciplines1.set(newText);
            break;
        case R.id.job_search_disciples2:
            newText = position == 0 ? "" : disciplines2Spinner.getSelectedItem().toString();
            properties.disciplines2.set(newText);
            break;
        case R.id.job_search_disciples3:
            newText = position == 0 ? "" : disciplines3Spinner.getSelectedItem().toString();
            properties.disciplines3.set(newText);
            break;
        case R.id.job_search_term:
            newText = termSpinner.getSelectedItem().toString();
            newText = newText.substring(newText.lastIndexOf("(") + 1, newText.lastIndexOf(")"));
            properties.term.set(newText);
            break;
        case R.id.job_search_filter:
            newText = filterSpinner.getSelectedItem().toString();
            JobSearchProperties.FILTER newFilter = JobSearchProperties.FILTER.fromString(newText);
            properties.filter.set(newFilter);
            break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    // Sets the search button to not close after clicking it
    @Override
    public void onShow(DialogInterface dialogInterface) {
        Button posButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        posButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                sendSearch();
            }
        });

    }

    public void sendSearch() {
        // Update the edittext, if changed they will be marked automatically
        properties.employer.set(employerText.getText().toString());
        properties.title.set(jobTitleText.getText().toString());

        // Update the checkboxes
        properties.levelJunior.set(juniorChkbx.isChecked());
        properties.levelIntermediate.set(intermediateChkbx.isChecked());
        properties.levelSenior.set(seniorChkbx.isChecked());
        properties.levelBachelors.set(bachelorChkbx.isChecked());
        properties.levelMasters.set(mastersChkbx.isChecked());
        properties.levelPhD.set(phdChkbx.isChecked());

        listener.onSearch(properties);
    }

    public static String stringifyTerm(Calendar date) {
        int month = date.get(Calendar.MONTH);
        int year = date.get(Calendar.YEAR);
        int termType = (int)Math.floor(month / 4) * 4 + 1;

        String term = Integer.toString(year - 1000).substring(0, 1);     // Get first digit of year subtract 1
        term += Integer.toString(year).substring(2);                     // Get 3rd digit of the year
        term += Integer.toString(termType);
        return term;
    }

    public static String readableTerm(Calendar date) {
        String term = stringifyTerm(date);
        char termType = term.charAt(3);
        switch(termType) {
        case '5':
            return "Spring " + date.get(Calendar.YEAR) + "\t(" + term + ")";
        case '1':
            return "Winter " + date.get(Calendar.YEAR) + "\t(" + term + ")";
        case '9':
            return "Fall " + date.get(Calendar.YEAR) + "\t(" + term + ")";
        default:
            throw new JbmnplsParsingException("Not possible to get this term code.");
        }
    }

    public static Calendar getTimeFromTerm(String term) {
        Calendar then = Calendar.getInstance();
        try {
            String year = Integer.toString(Integer.parseInt(term.substring(0, 1)) + 1) + '0';
            year += term.substring(1, 3);
            then.setTime(sdf.parse(year));
            char termType = term.charAt(3);
            switch(termType) {
            case '5':
                then.set(Calendar.MONTH, Calendar.MAY);
                break;
            case '1':
                then.set(Calendar.MONTH, Calendar.JANUARY);
                break;
            case '9':
                then.set(Calendar.MONTH, Calendar.SEPTEMBER);
                break;
            default:
                throw new JbmnplsParsingException("Not possible to get this term code.");
            }
        } catch (ParseException e) {
            throw new JbmnplsParsingException("Invalid term code, cannot parse");
        }
        return then;
    }

    private void populateTermSpinner(String term) {
        Calendar then;
        if (term.equals("")) {
            then = Calendar.getInstance();
            then.add(Calendar.MONTH, 4);
        } else {
            then = getTimeFromTerm(term);
        }

        // Get the last two terms and the next two terms
        long original = then.getTimeInMillis();
        ArrayList<String> termList = new ArrayList<String>();
        int currentIndex = -1;
        then.add(Calendar.MONTH, -8);   // 2 terms ago
        for (int i = 0; i < 5; i++) {
            if (then.getTimeInMillis() == original) {
                currentIndex = i;
            }
            termList.add(readableTerm(then));
            then.add(Calendar.MONTH, 4);
        }

        // Attach the values to the spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(ctx, android.R.layout.simple_spinner_item, termList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        termSpinner.setAdapter(adapter);
        termSpinner.setSelection(currentIndex);
    }

    public interface OnJobSearchListener {
        public void onJobTypeChange(Spinner spinner, JobSearchProperties.JOBTYPE type);
        public void onSearch(JobSearchProperties properties);
        public void onCancel();
    }
}
