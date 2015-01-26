package com.gb.canibuythat.ui;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;

import com.gb.canibuythat.R;
import com.gb.canibuythat.util.DialogUtils;


public class BudgetModifierDetailActivity extends ActionBarActivity {

	public static Intent getIntentForUpdate(Context context, int id) {
		Intent detailIntent = getIntentForCreate(context);
		detailIntent.putExtra(BudgetModifierDetailFragment.EXTRA_ITEM_ID, id);
		return detailIntent;
	}


	public static Intent getIntentForCreate(Context context) {
		return new Intent(context, BudgetModifierDetailActivity.class);
	}


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_budgetmodifier_detail);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		if (savedInstanceState == null) {
			Bundle arguments = new Bundle();
			arguments.putInt(BudgetModifierDetailFragment.EXTRA_ITEM_ID,
					getIntent().getIntExtra(BudgetModifierDetailFragment.EXTRA_ITEM_ID, 0));
			BudgetModifierDetailFragment fragment = new BudgetModifierDetailFragment();
			fragment.setArguments(arguments);
			getFragmentManager().beginTransaction().add(R.id.budgetmodifier_detail_container, fragment).commit();
		}
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == android.R.id.home) {
			navigateUpTo(new Intent(this, BudgetModifierListActivity.class));
			return true;
		}
		return super.onOptionsItemSelected(item);
	}


	@Override
	public void onBackPressed() {
		final BudgetModifierDetailFragment detailFragment = (BudgetModifierDetailFragment) getFragmentManager()
				.findFragmentById(R.id.budgetmodifier_detail_container);
		if (detailFragment != null && detailFragment.isChanged()) {
			DialogUtils.getSaveOrDiscardDialog(this, new Runnable() {

				@Override
				public void run() {
					if (detailFragment.saveUserData()) {
						finish();
					}
				}
			}, new Runnable() {

				@Override
				public void run() {
					finish();
				}
			}).show();
		} else {
			super.onBackPressed();
		}
	}
}
