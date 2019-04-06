package com.gb.canibuyit.feature.spending.view

import android.content.Context
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.DividerItemDecoration
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.gb.canibuyit.R
import com.gb.canibuyit.base.view.BaseFragment
import com.gb.canibuyit.di.Injector
import com.gb.canibuyit.feature.spending.model.Spending
import com.gb.canibuyit.feature.spending.ui.SpendingAdapter
import com.gb.canibuyit.feature.spending.view.SpendingListFragment.FragmentCallback
import kotlinx.android.synthetic.main.fragment_spending_list.*

/**
 * A list fragment representing a list of Spendings. This fragment also supports
 * tablet devices by allowing list items to be given an 'activated' state upon selection.
 * This helps indicate which item is currently being viewed in a [SpendingEditorFragment].
 *
 * Activities containing this fragment MUST implement the [FragmentCallback] interface.
 */
class SpendingListFragment : BaseFragment<SpendingListScreen, SpendingListPresenter>(),
        SpendingListScreen, SpendingAdapter.OnSpendingClickedListener,
        SwipeRefreshLayout.OnRefreshListener {

    /**
     * The fragment's current callback object, which is notified of list item clicks.
     */
    private var callback: FragmentCallback? = null

    private lateinit var adapter: SpendingAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_spending_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recycler_view.addItemDecoration(
                DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))
        adapter = SpendingAdapter(this)
        recycler_view.adapter = adapter
        swipe_container.setOnRefreshListener(this)
    }

    override fun onResume() {
        super.onResume()
        presenter.fetchSpendings()
    }

    override fun inject() {
        Injector.INSTANCE.graph.inject(this)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        // Activities containing this fragment must implement its callback.
        if (activity !is FragmentCallback) {
            throw IllegalStateException("Activity must implement fragment's callback.")
        }
        callback = activity as FragmentCallback
    }

    override fun setData(spendings: List<Spending>) {
        adapter.setData(spendings)
    }

    override fun onDetach() {
        super.onDetach()
        callback = null
    }

    /**
     * Notify the active callback interface (the activity, if the
     * fragment is attached to one) that an item has been selected.
     */
    override fun onSpendingClicked(spending: Spending) {
        callback?.onSpendingSelected(spending.id!!)
    }

    override fun onRefresh() {
        swipe_container.isRefreshing = false
        callback?.refresh()
    }

    /**
     * A callback interface that all activities containing this fragment must implement.
     * This mechanism allows activities to be notified of events in the fragment.
     */
    internal interface FragmentCallback {

        /**
         * Callback for when a spending has been selected.
         *
         * @param id database id of the spending
         */
        fun onSpendingSelected(id: Int)

        fun refresh()
    }
}
