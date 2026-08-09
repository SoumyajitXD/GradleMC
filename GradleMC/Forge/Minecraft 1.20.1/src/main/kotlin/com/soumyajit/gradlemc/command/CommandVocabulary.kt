package com.soumyajit.gradlemc.command

object CommandVocabulary {
    const val ROOT_COMMAND = "gradlemc"
    const val HELP = "help"
    const val VERSION_SUBCOMMAND = "version"

    const val GUI = "gui"
    const val STATUS = "status"
    const val MEMORY = "memory"
    const val CHECK = "check"
    const val EXPORT = "export"
    const val REPORTS = "reports"
    const val LATEST = "latest"
    const val SMART = "smart"
    const val SCORE = "score"
    const val ADVICE = "advice"
    const val TEST_FPS = "testfps"
    const val PERF = "perf"
    const val PERFORMANCE = "performance"
    const val START = "start"
    const val STOP = "stop"
    const val OVERHEAD = "overhead"
    const val GUARD = "guard"
    const val EXPLAIN = "explain"
    const val SELF_TEST = "selftest"
    const val MODE = "mode"
    const val LOW_IMPACT = "low_impact"
    const val BALANCED = "balanced"
    const val DETAILED = "detailed"

    /** Public command words kept in one place so tests can assert the lowercase contract. */
    val requiredCommands = setOf(
        ROOT_COMMAND, HELP, GUI, STATUS, VERSION_SUBCOMMAND, MEMORY, CHECK, EXPORT, REPORTS, LATEST,
        SMART, SCORE, ADVICE, TEST_FPS, PERF, PERFORMANCE, START, STOP, OVERHEAD, GUARD,
        EXPLAIN, SELF_TEST, MODE, LOW_IMPACT, BALANCED, DETAILED,
    )
}
