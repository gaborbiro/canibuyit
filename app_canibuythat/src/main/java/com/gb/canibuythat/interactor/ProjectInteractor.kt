package com.gb.canibuythat.interactor

import com.gb.canibuythat.db.model.ApiProject
import com.gb.canibuythat.repository.ProjectRepository
import com.gb.canibuythat.rx.SchedulerProvider
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import javax.inject.Inject
import kotlin.reflect.KMutableProperty0

class ProjectInteractor @Inject
constructor(val projectRepository: ProjectRepository,
            private val schedulerProvider: SchedulerProvider) {

    fun getProject(): Single<Project> = Single.create<Project> { singleEmitter ->
        singleEmitter.onSuccess(Project(projectRepository, schedulerProvider))
    }
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.mainThread())
}

class Project constructor(projectRepository: ProjectRepository,
                          schedulerProvider: SchedulerProvider) {

    private var nameUpdater = Updater(schedulerProvider, projectRepository::projectName)
    private var nameOverrideUpdater = Updater(schedulerProvider, projectRepository::nameOverride)
    private var categoryOverrideUpdater = Updater(schedulerProvider, projectRepository::categoryOverride)
    private var averageOverrideUpdater = Updater(schedulerProvider, projectRepository::averageOverride)
    private var cycleOverrideUpdater = Updater(schedulerProvider, projectRepository::cycleOverride)
    private var whenOverrideUpdater = Updater(schedulerProvider, projectRepository::whenOverride)

    private val apiProject: ApiProject? = projectRepository.project

    var projectName: String? = apiProject?.name
        set(value) {
            field = value
            nameUpdater.update(value)
        }

    var nameOverride: Boolean = apiProject?.nameOverride ?: false
        set(value) {
            field = value
            nameOverrideUpdater.update(value)
        }

    var categoryOverride: Boolean = apiProject?.categoryOverride ?: false
        set(value) {
            field = value
            categoryOverrideUpdater.update(value)
        }

    var averageOverride: Boolean = apiProject?.averageOverride ?: false
        set(value) {
            field = value
            averageOverrideUpdater.update(value)
        }

    var cycleOverride: Boolean = apiProject?.cycleOverride ?: false
        set(value) {
            field = value
            cycleOverrideUpdater.update(value)
        }

    var whenOverride: Boolean = apiProject?.whenOverride ?: false
        set(value) {
            field = value
            whenOverrideUpdater.update(value)
        }

    class Updater<T>(val schedulerProvider: SchedulerProvider,
                     val property: KMutableProperty0<T>) {
        private var disposable: Disposable? = null

        fun update(value: T) {
            disposable?.dispose()
            disposable = Single.create<Unit> { property.set(value) }
                    .subscribeOn(schedulerProvider.io()).subscribe()
        }
    }
}