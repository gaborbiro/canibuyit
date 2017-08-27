package com.gb.canibuythat.repository;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import com.gb.canibuythat.UserPreferences;
import com.gb.canibuythat.model.Balance;
import com.gb.canibuythat.model.BudgetItem;
import com.gb.canibuythat.provider.BalanceCalculator;
import com.gb.canibuythat.provider.BudgetDbHelper;
import com.gb.canibuythat.provider.Contract;
import com.gb.canibuythat.ui.model.BalanceReading;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.UpdateBuilder;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;

public class BudgetRepository {

    private BudgetDbHelper budgetDbHelper;
    private Dao<BudgetItem, Integer> budgetItemDao;
    private UserPreferences userPreferences;

    @Inject
    public BudgetRepository(BudgetDbHelper budgetDbHelper, UserPreferences userPreferences) {
        this.budgetDbHelper = budgetDbHelper;
        try {
            this.budgetItemDao = budgetDbHelper.getDao(BudgetItem.class);
        } catch (SQLException e) {
            // ignore
        }
        this.userPreferences = userPreferences;
    }

    public Maybe<List<BudgetItem>> getAll() {
        try {
            return Maybe.just(budgetItemDao.queryForAll());
        } catch (SQLException e) {
            return Maybe.error(e);
        }
    }

    public Single<Dao.CreateOrUpdateStatus> createOrUpdate(BudgetItem budgetItem) {
        try {
            return Single.just(budgetItemDao.createOrUpdate(budgetItem));
        } catch (SQLException e) {
            return Single.error(e);
        }
    }

    public Completable delete(int id) {
        try {
            if (budgetItemDao.deleteById(id) > 0) {
                return Completable.complete();
            } else {
                return Completable.error(new Exception("Delete error: budget item " + id + " was not found in the database"));
            }
        } catch (SQLException e) {
            return Completable.error(e);
        }
    }

    public Maybe<BudgetItem> read(int id) {
        try {
            BudgetItem budgetItem = budgetItemDao.queryForId(id);

            if (budgetItem != null) {
                return Maybe.just(budgetItem);
            } else {
                return Maybe.empty();
            }
        } catch (SQLException e) {
            return Maybe.error(e);
        }
    }

    public Single<Balance> calculateBalance() {
        float bestCase = 0;
        float worstCase = 0;

        // blocking thread
        BalanceReading balanceReading = userPreferences.getBalanceReading();

        for (BudgetItem item : budgetItemDao) {
            if (item.getEnabled()) {
                Date startDate = balanceReading != null ? balanceReading.when : null;
                BalanceCalculator.BalanceResult result = BalanceCalculator.get()
                        .getEstimatedBalance(item, startDate, userPreferences.getEstimateDate());
                bestCase += result.bestCase;
                worstCase += result.worstCase;
            }
        }

        if (balanceReading != null) {
            bestCase += balanceReading.balance;
            worstCase += balanceReading.balance;
        }
        return Single.just(Balance.builder().balanceReading(balanceReading).bestCaseBalance(bestCase).worstCaseBalance(worstCase).build());
    }

    public Completable moveToIndex(int from, int to) {
        List<UpdateBuilder<BudgetItem, Integer>> updates = new ArrayList<>();
        Map<String, Object> fieldMap = new HashMap<>();
        BudgetItem item;

        try {
            for (int i = from; i != to + (from < to ? 1 : -1); ) {
                fieldMap.put(Contract.BudgetItem.ORDERING, i);
                item = budgetItemDao.queryForFieldValues(fieldMap).get(0);
                int newIndex = modulo(i + (from < to ? -1 : 1), Math.abs(to - from) + 1);
                UpdateBuilder<BudgetItem, Integer> builder = budgetItemDao.updateBuilder();
                builder.where().eq(Contract.BudgetItem._ID, item.getId());
                builder.updateColumnValue(Contract.BudgetItem.ORDERING, newIndex);
                updates.add(builder);
                i += from < to ? 1 : -1;
            }

            budgetItemDao.setAutoCommit(budgetItemDao.getConnectionSource().getReadWriteConnection(), false);

            for (UpdateBuilder<BudgetItem, Integer> update : updates) {
                update.update();
            }
            budgetItemDao.commit(budgetItemDao.getConnectionSource().getReadWriteConnection());
            return Completable.complete();
        } catch (SQLException e) {
            return Completable.error(e);
        }
    }

    private int modulo(int n, int base) {
        return (n % base + base) % base;
    }

    public Completable importDatabaseFromFile(String file) {
        SQLiteDatabase db;
        Cursor cursor;
        try {
            db = budgetDbHelper.getDatabaseFromFile(file);
        } catch (SQLiteException e) {
            return Completable.error(new Exception("Cannot open database from " + file, e));
        }
        try {
            cursor = budgetDbHelper.getAllBudgetItems(db);
        } catch (SQLException e) {
            return Completable.error(new Exception("Error reading " + file, e));
        }
        try {
            budgetDbHelper.replaceBudgetDatabase(cursor);
            return Completable.complete();
        } catch (SQLException e) {
            return Completable.error(new Exception("Error writing to table " + Contract.BudgetItem.TABLE, e));
        } finally {
            try {
                cursor.close();
            } catch (Throwable t) {
                // ignore
            }
            try {
                db.close();
            } catch (Throwable t) {
                // ignore
            }
        }
    }
}
