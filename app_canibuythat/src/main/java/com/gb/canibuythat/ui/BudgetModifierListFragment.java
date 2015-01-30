package com.gb.canibuythat.ui;


import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.widget.ListView;

import com.gb.canibuythat.R;
import com.gb.canibuythat.provider.BudgetModifierProvider;
import com.gb.canibuythat.provider.Contract;


/**
 * A list fragment representing a list of BudgetModifiers. This fragment also supports tablet devices by allowing list
 * items to be given an 'activated' state upon selection. This helps indicate which item is currently being viewed in a
 * {@link BudgetModifierDetailFragment}.
 * <p/>
 * Activities containing this fragment MUST implement the {@link Callbacks} interface.
 */
public class BudgetModifierListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {

	/**
	 * The serialization (saved instance state) Bundle key representing the activated item position. Only used on
	 * tablets.
	 */
	private static final String	STATE_ACTIVATED_POSITION	= "activated_position";

	/**
	 * The fragment's current callback object, which is notified of list item clicks.
	 */
	private Callbacks			callback					= dummyCallbacks;

	/**
	 * The current activated item position. Only used on tablets.
	 */
	private int					activatedPosition			= ListView.INVALID_POSITION;

	private SimpleCursorAdapter	adapter;

	/**
	 * A callback interface that all activities containing this fragment must implement. This mechanism allows
	 * activities to be notified of item selections.
	 */
	public interface Callbacks {

		/**
		 * Callback for when an item has been selected.
		 */
		public void onItemSelected(int id);
	}

	/**
	 * A dummy implementation of the {@link Callbacks} interface that does nothing. Used only when this fragment is not
	 * attached to an activity.
	 */
	private static Callbacks	dummyCallbacks	= new Callbacks() {

													@Override
													public void onItemSelected(int id) {
													}
												};


	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the fragment (e.g. upon screen orientation
	 * changes).
	 */
	public BudgetModifierListFragment() {
	}


	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		// Restore the previously serialized activated item position.
		if (savedInstanceState != null && savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
			setActivatedPosition(savedInstanceState.getInt(STATE_ACTIVATED_POSITION));
		}
	}


	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		// Activities containing this fragment must implement its callback.
		if (!(activity instanceof Callbacks)) {
			throw new IllegalStateException("Activity must implement fragment's callback.");
		}

		callback = (Callbacks) activity;

		adapter = new SimpleCursorAdapter(getActivity(), R.layout.budgetmodifier_list_item, null, new String[] {
				Contract.BudgetModifier.TITLE, Contract.BudgetModifier.AMOUNT,
				Contract.BudgetModifier.PERIOD_MULTIPLIER, Contract.BudgetModifier.PERIOD
		}, new int[] {
				R.id.title, R.id.amount, R.id.period_multiplier, R.id.period
		}, 0);
		setListAdapter(adapter);
		getLoaderManager().initLoader(hashCode(), null, this);

	}


	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(getActivity(), BudgetModifierProvider.BUDGET_MODIFIERS_URI, null, null, null, null);
	}


	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		adapter.swapCursor(data);
	}


	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		adapter.swapCursor(null);
	}


	@Override
	public void onDetach() {
		super.onDetach();

		// Reset the active callback interface to the dummy implementation.
		callback = dummyCallbacks;
	}


	@Override
	public void onListItemClick(ListView listView, View view, int position, long id) {
		super.onListItemClick(listView, view, position, id);

		// Notify the active callback interface (the activity, if the
		// fragment is attached to one) that an item has been selected.
		callback.onItemSelected((int) getListAdapter().getItemId(position));
	}


	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (activatedPosition != ListView.INVALID_POSITION) {
			// Serialize and persist the activated item position.
			outState.putInt(STATE_ACTIVATED_POSITION, activatedPosition);
		}
	}


	/**
	 * Turns on activate-on-click mode. When this mode is on, list items will be given the 'activated' state when
	 * touched.
	 */
	public void setActivateOnItemClick(boolean activateOnItemClick) {
		getListView().setChoiceMode(activateOnItemClick ? ListView.CHOICE_MODE_SINGLE : ListView.CHOICE_MODE_NONE);
	}


	private void setActivatedPosition(int position) {
		if (position == ListView.INVALID_POSITION) {
			getListView().setItemChecked(activatedPosition, false);
		} else {
			getListView().setItemChecked(position, true);
		}
		activatedPosition = position;
	}
}
