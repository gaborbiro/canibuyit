package com.gb.canibuythat.ui;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
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
import android.widget.Toast;

import com.gb.canibuythat.App;
import com.gb.canibuythat.R;
import com.gb.canibuythat.provider.BudgetProvider;
import com.gb.canibuythat.provider.Contract;
import com.gb.canibuythat.ui.dragndroplist.DragNDropCursorAdapter;
import com.gb.canibuythat.ui.dragndroplist.DragNDropListView;
import com.gb.canibuythat.ui.task.Callback;
import com.gb.canibuythat.ui.task.budget_item.MoveToIndexTask;

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
public class BudgetItemListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemClickListener, DragNDropListView.OnItemDragNDropListener {

    /**
     * A dummy implementation of the {@link FragmentCallback} interface that does
     * nothing. Used
     * only when this fragment is not
     * attached to an activity.
     */
    private static FragmentCallback dummyFragmentCallback = id -> {};
    /**
     * The fragment's current callback object, which is notified of list item clicks.
     */
    private FragmentCallback callback = dummyFragmentCallback;

    private DragNDropListView list;
    private DragNDropCursorAdapter adapter;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the fragment
     * (e.g. upon screen orientation changes).
     */
    public BudgetItemListFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_budget_item_list, container, false);
        list = (DragNDropListView) root.findViewById(android.R.id.list);
        list.setOnItemDragNDropListener(this);
        list.setOnItemClickListener(this);
        return root;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getLoaderManager().initLoader(hashCode(), null, this);
    }

    private DragNDropCursorAdapter getListAdapter() {
        if (adapter == null) {
            adapter = new BudgetItemListAdapter(getActivity(), null);

            if (list != null) {
                list.setDragNDropAdapter(adapter);
            }
        }
        return adapter;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // Activities containing this fragment must implement its callback.
        if (!(getActivity() instanceof FragmentCallback)) {
            throw new IllegalStateException("Activity must implement fragment's callback.");
        }

        callback = (FragmentCallback) getActivity();
    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(), BudgetProvider.BUDGET_ITEMS_URI, null, null, null, Contract.BudgetItem.ORDERING + " ASC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        getListAdapter().swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        getListAdapter().swapCursor(null);
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // Reset the active callback interface to the dummy implementation.
        callback = dummyFragmentCallback;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // Notify the active callback interface (the activity, if the
        // fragment is attached to one) that an item has been selected.
        callback.onListItemClick((int) getListAdapter().getItemId(position));
    }

    @Override
    public void onItemDragStart(DragNDropListView parent, View view, int position, long id) {

    }

    @Override
    public void onItemDrop(DragNDropListView parent, View view, final int startPosition, final int endPosition, long id) {
        new MoveToIndexTask(startPosition, endPosition, mReorderCallback).execute();
    }

    private Callback<Integer> mReorderCallback = new Callback<Integer>() {
        @Override
        public void onError(Throwable t) {
            if (t.getCause() != null
                    && t.getCause().getCause() != null
                    && t.getCause().getCause() instanceof SQLiteConstraintException) {
                t = t.getCause().getCause();
            }
            t.printStackTrace();
            Toast.makeText(App.getAppContext(), "Error saving data. Check logs for more information.", Toast.LENGTH_SHORT).show();
        }
    };

    /**
     * A callback interface that all activities containing this fragment must implement.
     * This mechanism allows activities to be notified of events in the fragment.
     */
    interface FragmentCallback {

        /**
         * Callback for when a budget item has been selected.
         *
         * @param id database id of the budget item
         */
        void onListItemClick(int id);
    }
}
