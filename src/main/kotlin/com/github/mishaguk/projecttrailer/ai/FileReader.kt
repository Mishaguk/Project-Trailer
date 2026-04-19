package com.github.mishaguk.projecttrailer.ai

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.nio.file.Path

internal object FileReader {

    private const val MAX_CHARS_PER_CALL = 4_000
    private const val MAX_LINES_PER_CALL = 200

    fun read(project: Project, relPath: String, startLine: Int?, endLine: Int?): String = ReadAction.compute<String, RuntimeException> {
        val base = project.basePath ?: return@compute "ERROR: project has no base path"
        val normalized = relPath.trimStart('/', '\\').replace('\\', '/')
        if (normalized.isEmpty()) return@compute "ERROR: empty path"
        if (normalized.split('/').any { it in ProjectStructureScanner.DENY_LIST }) {
            return@compute "ERROR: path contains denied directory"
        }
        val absolute = Path.of(base, normalized).toString().replace('\\', '/')
        var vf = LocalFileSystem.getInstance().findFileByPath(absolute)
        if (vf == null) {
            // Fallback: agent may have reconstructed the path incorrectly from the indented
            // listing. Try to locate by basename anywhere in the project's content roots.
            val basename = normalized.substringAfterLast('/')
            vf = findByName(project, basename)
                ?: return@compute "ERROR: file not found: $normalized"
        }
        if (vf.isDirectory) return@compute "ERROR: path is a directory: $normalized"

        val text = try {
            String(vf.contentsToByteArray(), Charsets.UTF_8)
        } catch (e: Exception) {
            return@compute "ERROR: read failed: ${e.message}"
        }

        val lines = text.lines()
        val from = (startLine ?: 1).coerceAtLeast(1)
        val to = (endLine ?: lines.size).coerceAtMost(lines.size).coerceAtMost(from + MAX_LINES_PER_CALL - 1)
        if (from > lines.size) return@compute "ERROR: startLine $from beyond file length ${lines.size}"

        val slice = lines.subList(from - 1, to).joinToString("\n")
        val truncated = if (slice.length > MAX_CHARS_PER_CALL) slice.substring(0, MAX_CHARS_PER_CALL) + "\n… (truncated)" else slice
        "$normalized:$from-$to\n$truncated"
    }

    private fun findByName(project: Project, basename: String): VirtualFile? {
        if (basename.isBlank()) return null
        for (root in ProjectRootManager.getInstance(project).contentRoots) {
            val hit = search(root, basename)
            if (hit != null) return hit
        }
        return null
    }

    private fun search(dir: VirtualFile, basename: String): VirtualFile? {
        if (!dir.isDirectory) return null
        if (dir.name in ProjectStructureScanner.DENY_LIST) return null
        val children = dir.children ?: return null
        for (child in children) {
            if (!child.isDirectory && child.name == basename) return child
        }
        for (child in children) {
            if (child.isDirectory) {
                val hit = search(child, basename)
                if (hit != null) return hit
            }
        }
        return null
    }
}
