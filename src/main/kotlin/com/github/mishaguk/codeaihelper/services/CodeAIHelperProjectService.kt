package com.github.mishaguk.codeaihelper.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.github.mishaguk.codeaihelper.CodeAIHelperBundle

@Service(Service.Level.PROJECT)
class CodeAIHelperProjectService(project: Project) {

    init {
        thisLogger().info(CodeAIHelperBundle.message("projectService", project.name))
    }

    fun getRandomNumber() = (1..100).random()
}
