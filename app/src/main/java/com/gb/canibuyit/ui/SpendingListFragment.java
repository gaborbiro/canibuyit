package com.gb.canibuyit.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gb.canibuyit.R;
import com.gb.canibuyit.di.Injector;
import com.gb.canibuyit.model.Spending;
import com.gb.canibuyit.presenter.BasePresenter;
import com.gb.canibuyit.presenter.SpendingListPresenter;
import com.gb.canibuyit.screen.SpendingListScreen;
import com.gb.canibuyit.ui.adapter.SpendingAdapter;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;

/**
 * A list fragment representing a list of Spendings. This fragment also supports
 * tablet devices by allowing list items to be given an 'activated' state upon selection.
 * This helps indicate which item is currently being viewed in a {@link SpendingEditorFragment}.
 * <p/>
 * Activities containing this fragment MUST implement the {@link FragmentCallback} interface.
 */
public class SpendingListFragment extends BaseFragment implements SpendingListScreen, SpendingAdapter.OnSpendingClickedListener, SwipeRefreshLayout.OnRefreshListener {

    /**
     * A dummy implementation of the {@link FragmentCallback} interface that does
     * nothing. Used
     * only when this fragment is not
     * attached to an activity.
     */
    private static FragmentCallback dummyFragmentCallback = new FragmentCallback() {
        @Override
        public void onSpendingSelected(int id) {
        }

        @Override
        public void refresh() {
        }
    };
    /**
     * The fragment's current callback object, which is notified of list item clicks.
     */
    private FragmentCallback callback = dummyFragmentCallback;

    @BindView(android.R.id.list) RecyclerView list;
    @BindView(R.id.swipe_container) SwipeRefreshLayout swipeRefreshLayout;

    private SpendingAdapter adapter;

    @Inject SpendingListPresenter presenter;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the fragment
     * (e.g. upon screen orientation changes).
     */
    public SpendingListFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_spending_list, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        list.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL));
        adapter = new SpendingAdapter(this);
        list.setAdapter(adapter);
        swipeRefreshLayout.setOnRefreshListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        presenter.fetchSpendings();
    }

    @Override
    protected BasePresenter inject() {
        Injector.INSTANCE.getGraph().inject(this);
        return presenter;
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
    public void setData(List<Spending> spendings) {
        adapter.setData(spendings);
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
    public void onSpendingClicked(Spending spending) {
        callback.onSpendingSelected(spending.getId());
    }

    @Override
    public void onRefresh() {
        swipeRefreshLayout.setRefreshing(false);
        callback.refresh();
    }

    /**
     * A callback interface that all activities containing this fragment must implement.
     * This mechanism allows activities to be notified of events in the fragment.
     */
    interface FragmentCallback {

        /**
         * Callback for when a spending has been selected.
         *
         * @param id database id of the spending
         */
        void onSpendingSelected(int id);

        void refresh();
    }
}