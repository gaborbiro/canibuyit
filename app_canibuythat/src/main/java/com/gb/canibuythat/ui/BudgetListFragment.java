package com.gb.canibuythat.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gb.canibuythat.R;
import com.gb.canibuythat.di.Injector;
import com.gb.canibuythat.model.BudgetItem;
import com.gb.canibuythat.presenter.BudgetListPresenter;
import com.gb.canibuythat.screen.BudgetListScreen;
import com.gb.canibuythat.ui.adapter.BudgetItemAdapter;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;

/**
 * A list fragment representing a list of BudgetModifiers. This fragment also supports
 * tablet devices by allowing list
 * items to be given an 'activated' state upon selection. This helps indicate which
 * item is currently being viewed in a
 * {@link BudgetEditorFragment}.
 * <p/>
 * Activities containing this fragment MUST implement the {@link FragmentCallback}
 * interface.
 */
public class BudgetListFragment extends BaseFragment implements BudgetListScreen, BudgetItemAdapter.OnBudgetItemClickedListener {

    /**
     * A dummy implementation of the {@link FragmentCallback} interface that does
     * nothing. Used
     * only when this fragment is not
     * attached to an activity.
     */
    private static FragmentCallback dummyFragmentCallback = id -> {
    };
    /**
     * The fragment's current callback object, which is notified of list item clicks.
     */
    private FragmentCallback callback = dummyFragmentCallback;

    @BindView(android.R.id.list) RecyclerView list;

    private BudgetItemAdapter adapter;

    @Inject BudgetListPresenter presenter;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the fragment
     * (e.g. upon screen orientation changes).
     */
    public BudgetListFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_budget_list, container, false);
        presenter.setScreen(this);
        return root;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        list.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL));
    }

    @Override
    public void onResume() {
        super.onResume();
        presenter.fetch();
    }

    @Override
    protected void inject() {
        Injector.INSTANCE.getGraph().inject(this);
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
    public void setData(List<BudgetItem> budgetItems) {
        adapter = new BudgetItemAdapter(budgetItems, this);
        list.setAdapter(adapter);
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // Reset the active callback interface to the dummy implementation.
        callback = dummyFragmentCallback;
    }

    @Override
    // Notify the active callback interface (the activity, if the
    // fragment is attached to one) that an item has been selected.
    public void onBudgetItemClicked(BudgetItem budgetItem) {
        callback.onBudgetItemSelected(budgetItem.getId());
    }

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
        void onBudgetItemSelected(int id);
    }
}
