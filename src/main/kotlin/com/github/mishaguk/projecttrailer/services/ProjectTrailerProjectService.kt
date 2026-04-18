package com.github.mishaguk.projecttrailer.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.github.mishaguk.projecttrailer.ProjectTrailerBundle

@Service(Service.Level.PROJECT)
class ProjectTrailerProjectService(project: Project) {

    init {
        thisLogger().info(ProjectTrailerBundle.message("projectService", project.name))
    }

    fun getRandomNumber() = (1..100).random()
}
