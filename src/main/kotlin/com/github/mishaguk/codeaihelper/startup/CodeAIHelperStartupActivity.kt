package com.github.mishaguk.codeaihelper.startup

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class CodeAIHelperStartupActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        thisLogger().info("codeAIHelper started for project: ${project.name}")
    }
}
