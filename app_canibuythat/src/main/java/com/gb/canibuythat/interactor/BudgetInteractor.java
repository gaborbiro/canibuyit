package com.gb.canibuythat.interactor;

import android.content.Context;
import android.os.Environment;

import com.gb.canibuythat.exception.DomainException;
import com.gb.canibuythat.model.Balance;
import com.gb.canibuythat.model.BudgetItem;
import com.gb.canibuythat.provider.BudgetDbHelper;
import com.gb.canibuythat.provider.BudgetProvider;
import com.gb.canibuythat.repository.BudgetRepository;
import com.gb.canibuythat.rx.SchedulerProvider;
import com.gb.canibuythat.util.FileUtils;
import com.j256.ormlite.dao.Dao;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.MaybeSource;
import io.reactivex.Single;

public class BudgetInteractor {

    private BudgetRepository budgetRepository;
    private Context appContext;
    private SchedulerProvider schedulerProvider;

    @Inject
    public BudgetInteractor(BudgetRepository budgetRepository, Context appContext, SchedulerProvider schedulerProvider) {
        this.budgetRepository = budgetRepository;
        this.appContext = appContext;
        this.schedulerProvider = schedulerProvider;
    }

    public Single<Dao.CreateOrUpdateStatus> createOrUpdate(BudgetItem budgetItem) {
        return budgetRepository.createOrUpdate(budgetItem)
                .onErrorResumeNext(throwable -> Single.error(new DomainException("Error updating budget item in database")))
                .doOnSuccess(createOrUpdateStatus -> appContext.getContentResolver().notifyChange(BudgetProvider.BUDGET_ITEMS_URI, null))
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread());
    }

    public Completable delete(int id) {
        return budgetRepository.delete(id)
                .onErrorResumeNext(throwable -> Completable.error(new DomainException("Error deleting budget item " + id + " in database")))
                .doOnComplete(() -> appContext.getContentResolver().notifyChange(BudgetProvider.BUDGET_ITEMS_URI, null))
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread());
    }

    public Maybe<BudgetItem> read(int id) {
        return budgetRepository.read(id)
                .onErrorResumeNext((MaybeSource<BudgetItem>) throwable -> Maybe.error(new DomainException("Error reading budget item " + id + " in database")))
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread());
    }

    public Single<Balance> calculateBalance() {
        return budgetRepository.calculateBalance()
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread());
    }

    public Completable moveToIndex(int from, int to) {
        return budgetRepository.moveToIndex(from, to)
                .onErrorResumeNext(throwable -> Completable.error(new DomainException("Index swap error (" + from + "->" + to + ")")))
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread());
    }

    public Completable importBudgetDatabaseFromFile(String file) {
        return budgetRepository.importDatabaseFromFile(file)
                .onErrorResumeNext(throwable -> Completable.error(new DomainException(throwable.getMessage(), throwable.getCause())))
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread());
    }

    public Completable exportDatabase() {
        try {
            String pack = appContext.getPackageName();

            File sd = Environment.getExternalStorageDirectory();
            File targetFolder = new File(sd + "/CanIBuyThat");

            if (!targetFolder.exists()) {
                targetFolder.mkdirs();
            }
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmssZ");
            String targetFilename = "budget-" + sdf.format(new Date()) + ".sqlite";
            File to = new File(targetFolder, targetFilename);

            File data = Environment.getDataDirectory();
            String currentDBPath = "/data/" + pack + "/databases/" + BudgetDbHelper.DATABASE_NAME;
            File from = new File(data, currentDBPath);

            FileUtils.copyFiles(from, to);
            return Completable.complete();
        } catch (Throwable t) {
            return Completable.error(new DomainException("Error exporting database", t));
        }
    }
}
