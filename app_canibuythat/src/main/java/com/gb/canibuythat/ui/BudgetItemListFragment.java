package com.gb.canibuythat.ui;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.gb.canibuythat.R;
import com.gb.canibuythat.provider.BudgetProvider;
import com.gb.canibuythat.ui.dragndroplist.DragNDropCursorAdapter;
import com.gb.canibuythat.ui.dragndroplist.DragNDropListView;

/**
 * A list fragment representing a list of BudgetModifiers. This fragment also supports
 * tablet devices by allowing list
 * items to be given an 'activated' state upon selection. This helps indicate which
 * item is currently being viewed in a
 * {@link BudgetItemEditorFragment}.
 * <p/>
 * Activities containing this fragment MUST implement the {@link FragmentCallback}
 * interface.
 */
public class BudgetItemListFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemClickListener,
        DragNDropListView.OnItemDragNDropListener {

    /**
     * A dummy implementation of the {@link FragmentCallback} interface that does
     * nothing. Used
     * only when this fragment is not
     * attached to an activity.
     */
    private static FragmentCallback dummyFragmentCallback = new FragmentCallback() {

        @Override public void onListItemClick(int id) {
        }
    };
    /**
     * The fragment's current callback object, which is notified of list item clicks.
     */
    private FragmentCallback mCallback = dummyFragmentCallback;

    private DragNDropListView mList;
    private DragNDropCursorAdapter mAdapter;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the fragment
     * (e.g. upon screen orientation changes).
     */
    public BudgetItemListFragment() {
    }

    @Nullable @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View root =
                inflater.inflate(R.layout.fragment_budget_item_list, container, false);
        mList = (DragNDropListView) root.findViewById(android.R.id.list);
        mList.setOnItemDragNDropListener(this);
        mList.setOnItemClickListener(this);
        return root;
    }

    @Override public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getLoaderManager().initLoader(hashCode(), null, this);
    }

    private DragNDropCursorAdapter getListAdapter() {
        if (mAdapter == null) {
            mAdapter = new BudgetItemListAdapter(getActivity(), null);

            if (mList != null) {
                mList.setDragNDropAdapter(mAdapter);
            }
        }
        return mAdapter;
    }

    @Override public void onAttach(Context context) {
        super.onAttach(context);

        // Activities containing this fragment must implement its callback.
        if (!(getActivity() instanceof FragmentCallback)) {
            throw new IllegalStateException(
                    "Activity must implement fragment's callback.");
        }

        mCallback = (FragmentCallback) getActivity();
    }


    @Override public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(), BudgetProvider.BUDGET_ITEMS_URI, null,
                null, null, null);
    }

    @Override public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        getListAdapter().swapCursor(data);
    }

    @Override public void onLoaderReset(Loader<Cursor> loader) {
        getListAdapter().swapCursor(null);
    }

    @Override public void onDetach() {
        super.onDetach();

        // Reset the active callback interface to the dummy implementation.
        mCallback = dummyFragmentCallback;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // Notify the active callback interface (the activity, if the
        // fragment is attached to one) that an item has been selected.
        mCallback.onListItemClick((int) getListAdapter().getItemId(position));
    }

    @Override
    public void onItemDragStart(DragNDropListView parent, View view, int position,
            long id) {

    }

    @Override
    public void onItemDrop(DragNDropListView parent, View view, int startPosition,
            int endPosition, long id) {

    }

    /**
     * A callback interface that all activities containing this fragment must implement.
     * This mechanism allows activities to be notified of events in the fragment.
     */
    public interface FragmentCallback {

        /**
         * Callback for when a budget item has been selected.
         *
         * @param id database id of the budget item
         */
        void onListItemClick(int id);
    }
}
