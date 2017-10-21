package com.gb.canibuythat.repository

import com.gb.canibuythat.db.SpendingDBHelper
import com.gb.canibuythat.db.model.ApiProject
import com.j256.ormlite.dao.Dao
import javax.inject.Inject

class ProjectRepository @Inject
constructor(spendingDBHelper: SpendingDBHelper) {
    private val projectDao: Dao<ApiProject, Int> = spendingDBHelper.getDao<Dao<ApiProject, Int>, ApiProject>(ApiProject::class.java)

    var project: ApiProject?
        get() = projectDao.queryForId(1)
        private set(value) {
            projectDao.createOrUpdate(value)
        }

    var projectName: String?
        get() = project?.name
        set(value) {
            project = updateOrCreate(name = value)
        }

    var nameOverride: Boolean
        get() = project?.nameOverride ?: false
        set(value) {
            project = updateOrCreate(nameOverride = value)
        }

    var categoryOverride: Boolean
        get() = project?.categoryOverride ?: false
        set(value) {
            project = updateOrCreate(categoryOverride = value)
        }

    var averageOverride: Boolean
        get() = project?.averageOverride ?: false
        set(value) {
            project = updateOrCreate(averageOverride = value)
        }

    var cycleOverride: Boolean
        get() = project?.cycleOverride ?: false
        set(value) {
            project = updateOrCreate(cycleOverride = value)
        }

    var whenOverride: Boolean
        get() = project?.whenOverride ?: false
        set(value) {
            project = updateOrCreate(whenOverride = value)
        }

    private fun updateOrCreate(id: Int? = 1,
                               name: String? = null,
                               nameOverride: Boolean? = null,
                               categoryOverride: Boolean? = null,
                               averageOverride: Boolean? = null,
                               cycleOverride: Boolean? = null,
                               whenOverride: Boolean? = null): ApiProject {
        val apiProject = project ?: ApiProject(id = id)
        name?.let { apiProject.name = it }
        nameOverride?.let { apiProject.nameOverride = it }
        categoryOverride?.let { apiProject.categoryOverride = it }
        averageOverride?.let { apiProject.averageOverride = it }
        cycleOverride?.let { apiProject.cycleOverride = it }
        whenOverride?.let { apiProject.whenOverride = it }
        return apiProject
    }
}