package com.gb.canibuythat.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;

import com.gb.canibuythat.App;
import com.gb.canibuythat.R;

public class BudgetItemEditorActivity extends ActionBarActivity {

    private BudgetItemEditorFragment mDetailFragment;

    public static Intent getIntentForUpdate(int budgetItemId) {
        Intent i = getIntentForCreate();
        i.putExtra(BudgetItemEditorFragment.EXTRA_ITEM_ID, budgetItemId);
        return i;
    }

    public static Intent getIntentForCreate() {
        return new Intent(App.getAppContext(), BudgetItemEditorActivity.class);
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget_item_detail);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            mDetailFragment = new BudgetItemEditorFragment();
            if (getIntent().hasExtra(BudgetItemEditorFragment.EXTRA_ITEM_ID)) {
                Bundle arguments = new Bundle();
                arguments.putInt(BudgetItemEditorFragment.EXTRA_ITEM_ID,
                        getIntent().getIntExtra(BudgetItemEditorFragment.EXTRA_ITEM_ID,
                                0));
                mDetailFragment.setArguments(arguments);
            }
            getFragmentManager().beginTransaction()
                    .add(R.id.budgetmodifier_detail_container, mDetailFragment)
                    .commit();
        }
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            if (mDetailFragment != null) {
                mDetailFragment.saveAndRun(new Runnable() {

                    @Override public void run() {
                        finish();
                    }
                });
            } else {
                finish();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override public void onBackPressed() {
        if (mDetailFragment != null) {
            mDetailFragment.saveAndRun(new Runnable() {

                @Override public void run() {
                    BudgetItemEditorActivity.super.onBackPressed();
                }
            });
        } else {
            BudgetItemEditorActivity.super.onBackPressed();
        }
    }
}