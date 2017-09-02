package com.gb.canibuythat.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.gb.canibuythat.R;

public class SpendingEditorActivity extends AppCompatActivity {

    private SpendingEditorFragment spendingEditorFragment;

    public static Intent getIntentForUpdate(Context context, int spendingId) {
        Intent i = getIntentForCreate(context);
        i.putExtra(SpendingEditorFragment.EXTRA_SPENDING_ID, spendingId);
        return i;
    }

    public static Intent getIntentForCreate(Context context) {
        return new Intent(context, SpendingEditorActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spending_editor);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            spendingEditorFragment = new SpendingEditorFragment();
            if (getIntent().hasExtra(SpendingEditorFragment.EXTRA_SPENDING_ID)) {
                Bundle arguments = new Bundle();
                arguments.putInt(SpendingEditorFragment.EXTRA_SPENDING_ID, getIntent().getIntExtra(SpendingEditorFragment.EXTRA_SPENDING_ID, 0));
                spendingEditorFragment.setArguments(arguments);
            }
            getSupportFragmentManager().beginTransaction().add(R.id.spending_editor_container, spendingEditorFragment).commit();
        } else {
            spendingEditorFragment = (SpendingEditorFragment) getSupportFragmentManager().findFragmentById(R.id.spending_editor_container);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            if (spendingEditorFragment != null) {
                spendingEditorFragment.saveAndRun(this::finish);
            } else {
                finish();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (spendingEditorFragment != null) {
            spendingEditorFragment.saveAndRun(SpendingEditorActivity.super::onBackPressed);
        } else {
            SpendingEditorActivity.super.onBackPressed();
        }
    }
}
