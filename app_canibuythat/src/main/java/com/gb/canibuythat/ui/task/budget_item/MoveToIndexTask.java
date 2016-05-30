package com.gb.canibuythat.ui.task.budget_item;

import com.gb.canibuythat.model.BudgetItem;
import com.gb.canibuythat.provider.Contract;
import com.gb.canibuythat.ui.task.Callback;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.UpdateBuilder;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MoveToIndexTask extends SQLTaskBase<Integer> {

    private int mFrom;
    private int mTo;

    public MoveToIndexTask(int sourceIndex, int targetIndex, Callback<Integer> callback) {
        super(callback);
        if (sourceIndex == targetIndex) {
            throw new IllegalArgumentException(
                    "Moving to the same index does not make sense!");
        }
        mFrom = sourceIndex;
        mTo = targetIndex;
    }

    @Override protected Integer doWork(Dao<BudgetItem, Integer> dao) throws SQLException {
        List<UpdateBuilder<BudgetItem, Integer>> updates = new ArrayList<>();
        Map<String, Object> fieldMap = new HashMap<>();
        BudgetItem item;

        for (int i = mFrom; i != mTo + (mFrom < mTo ? 1 : -1); ) {
            fieldMap.put(Contract.BudgetItem.ORDERING, i);
            item = dao.queryForFieldValues(fieldMap).get(0);
            int newIndex = modulo(i + (mFrom < mTo ? -1 : 1), Math.abs(mTo - mFrom) + 1);
            UpdateBuilder<BudgetItem, Integer> builder = dao.updateBuilder();
            builder.where().eq(Contract.BudgetItem._ID, item.mId);
            builder.updateColumnValue(Contract.BudgetItem.ORDERING, newIndex);
            updates.add(builder);
            i += mFrom < mTo ? 1 : -1;
        }

        dao.setAutoCommit(dao.getConnectionSource()
                .getReadWriteConnection(), false);
        int result = 0;
        for (UpdateBuilder<BudgetItem, Integer> update : updates) {
            result += update.update();
        }
        dao.commit(dao.getConnectionSource()
                .getReadWriteConnection());

        return result;
    }

    private int modulo(int n, int base) {
        return (n % base + base) % base;
    }
}
