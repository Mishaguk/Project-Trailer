    package com.github.mishaguk.projecttrailer.ai

    internal object TourSchema {

        const val SYSTEM_PROMPT: String =
            "You are a codebase tour guide for newcomers. " +
                    "Given a listing of the project's folders and files, produce an ordered high-level tour of 5 to 20 steps " +
                    "that explains the architecture, main modules, and responsibilities of the project. " +
                    "Focus primarily on directories, packages, and logical components rather than individual files. " +
                    "Use files only when they represent important entry points (e.g., main class, configuration, build file). " +
                    "For each step: 'path' must EXACTLY match a path from the listing. " +
                    "Paths are project-relative and MUST NOT include the project root folder name. " +
                    "Do NOT prepend the top-level directory. " +
                    "Use the path exactly as shown in the listing (including spaces and casing). " +
                    "Examples:\n" +
                    "Correct: src/main/java/\n" +
                    "Incorrect: gamestudio-8886/src/main/java/\n" +
                    "'title' is a short human label; 'explanation' is at most two sentences describing what exists there and why it matters. " +
                    "Order steps so earlier ones give high-level context and later ones go into more detail. " +
                    "Do not invent paths."

        const val SYSTEM_PROMPT_AGENT: String = SYSTEM_PROMPT +
                " You may call the `read_file(path, startLine?, endLine?)` tool ONLY if the project structure is insufficient " +
                "to understand a module. Avoid unnecessary tool calls. Only call read_file on valid file paths from the listing. " +
                "Never call read_file on directories. Prefer reasoning from the structure first. " +
                "When you have enough information, stop calling tools."

        fun userPrompt(structure: String): String =
            "Project structure (indented listing, directories end with '/'):\n\n$structure"

        // OpenAI Structured Outputs JSON schema for the /chat/completions response_format.
        const val JSON_SCHEMA: String = """{
            "type":"object",
            "additionalProperties":false,
            "required":["steps"],
            "properties":{
              "steps":{
                "type":"array",
             "minItems": 5,
             "maxItems": 20,
                "items":{
                  "type":"object",
                  "additionalProperties":false,
                  "required":["path","title","explanation"],
                  "properties":{
                    "path":{"type":"string"},
                    "title":{"type":"string"},
                    "explanation":{"type":"string"}
                  }
                }
              }
            }
        }"""
    }
