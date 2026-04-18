package com.github.mishaguk.projecttrailer.startup

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class ProjectTrailerStartupActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        thisLogger().info("ProjectTrailer started for project: ${project.name}")
    }
}
