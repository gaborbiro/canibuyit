package com.gb.canibuythat.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;

import com.gb.canibuythat.App;
import com.gb.canibuythat.R;

public class BudgetEditorActivity extends ActionBarActivity {

    private BudgetEditorFragment budgetEditorFragment;

    public static Intent getIntentForUpdate(int budgetItemId) {
        Intent i = getIntentForCreate();
        i.putExtra(BudgetEditorFragment.EXTRA_ITEM_ID, budgetItemId);
        return i;
    }

    public static Intent getIntentForCreate() {
        return new Intent(App.getAppContext(), BudgetEditorActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget_editor);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            budgetEditorFragment = new BudgetEditorFragment();
            if (getIntent().hasExtra(BudgetEditorFragment.EXTRA_ITEM_ID)) {
                Bundle arguments = new Bundle();
                arguments.putInt(BudgetEditorFragment.EXTRA_ITEM_ID, getIntent().getIntExtra(BudgetEditorFragment.EXTRA_ITEM_ID, 0));
                budgetEditorFragment.setArguments(arguments);
            }
            getFragmentManager().beginTransaction().add(R.id.budgetmodifier_detail_container, budgetEditorFragment).commit();
        } else {
            budgetEditorFragment = (BudgetEditorFragment) getFragmentManager().findFragmentById(R.id.budgetmodifier_detail_container);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            if (budgetEditorFragment != null) {
                budgetEditorFragment.saveAndRun(this::finish);
            } else {
                finish();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (budgetEditorFragment != null) {
            budgetEditorFragment.saveAndRun(BudgetEditorActivity.super::onBackPressed);
        } else {
            BudgetEditorActivity.super.onBackPressed();
        }
    }
}
