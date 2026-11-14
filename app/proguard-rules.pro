# R8 is enabled for release in app/build.gradle.kts (isMinifyEnabled = true).
# R8 already receives AGP's default rules, every dependency's consumer rules,
# and aapt_rules.txt generated from the merged manifest. Only names that are
# used reflectively or stored outside the app process need app-level rules.

# Readable release stack traces: keep source file + line numbers, but replace
# the original file name so local paths do not leak. mapping.txt is written to
# app/build/outputs/mapping/release/mapping.txt for retrace.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# TimeUp/AppInfo are parceled into PendingIntent extras. The marshalled Intent
# stores the class name and can outlive the process or an app update, so keep
# these two names stable to avoid ClassNotFoundException after an upgrade.
# -keepnames still allows R8 to shrink and optimize the classes.
-keepnames class io.wiger.timeup.data.model.TimeUp
-keepnames class io.wiger.timeup.data.model.AppInfo
