package com.gb.canibuythat.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;

import com.gb.canibuythat.R;
import com.gb.canibuythat.util.DialogUtils;

import java.util.Date;

public class BudgetItemDetailActivity extends ActionBarActivity {

    public static Intent getIntentForUpdate(Context context, int budgetItemId,
            Date startDate) {
        Intent i = getIntentForCreate(context, startDate);
        i.putExtra(BudgetItemDetailFragment.EXTRA_ITEM_ID, budgetItemId);
        return i;
    }

    /**
     * @param startDate we are displaying the actual spending occurrences for a
     *                  BudgetItem. We need the startDate for that.
     */
    public static Intent getIntentForCreate(Context context, Date startDate) {
        Intent i = new Intent(context, BudgetItemDetailActivity.class);
        if (startDate != null) {
            i.putExtra(BudgetItemDetailFragment.EXTRA_START_DATE, startDate.getTime());
        }
        return i;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget_item_detail);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            BudgetItemDetailFragment fragment = new BudgetItemDetailFragment();
            if (getIntent().hasExtra(BudgetItemDetailFragment.EXTRA_ITEM_ID)) {
                Bundle arguments = new Bundle();
                arguments.putInt(BudgetItemDetailFragment.EXTRA_ITEM_ID,
                        getIntent().getIntExtra(BudgetItemDetailFragment.EXTRA_ITEM_ID,
                                0));
                fragment.setArguments(arguments);
            }
            getFragmentManager().beginTransaction()
                    .add(R.id.budgetmodifier_detail_container, fragment)
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            if (!saveAndRun(new Runnable() {

                @Override
                public void run() {
                    finish();
                }
            })) {
                finish();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (!saveAndRun(new Runnable() {

            @Override
            public void run() {
                BudgetItemDetailActivity.super.onBackPressed();
            }
        })) {
            super.onBackPressed();
        }
    }

    /**
     * @param toRunAfterSave will be run either if the user input was successfully saved
     *                       or the user choose "Discard". Will not be run if the user
     *                       choose "Cancel" or ["Save" but the input is invalid].
     * @return true if user data was successfully saved and the activity will exit
     */
    private boolean saveAndRun(final Runnable toRunAfterSave) {
        final BudgetItemDetailFragment detailFragment =
                (BudgetItemDetailFragment) getFragmentManager().findFragmentById(
                        R.id.budgetmodifier_detail_container);
        if (detailFragment != null && detailFragment.isChanged()) {
            DialogUtils.getSaveOrDiscardDialog(this, new Runnable() {

                @Override
                public void run() {
                    if (detailFragment.saveUserDataOrShowError()) {
                        toRunAfterSave.run();
                    }
                }
            }, toRunAfterSave)
                    .show();
        } else {
            return false;
        }
        return true;
    }
}
