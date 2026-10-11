package com.example.whisperrecorder.npu

import java.util.Locale

/** Pure package-format helpers kept free of Android APIs so path/checksum rules are unit-testable. */
internal object NpuPackageFormat {
    fun safeRelativePath(path: String): String {
        require(path.isNotBlank()) { "Package path is blank" }
        require(!path.startsWith('/') && !path.startsWith('\\')) { "Absolute package path is forbidden: $path" }
        require(!path.contains('\\')) { "Backslashes are forbidden in package paths: $path" }
        val parts = path.split('/')
        require(parts.none { it.isBlank() || it == "." || it == ".." }) { "Unsafe package path: $path" }
        return parts.joinToString("/")
    }

    fun parseChecksums(text: String): Map<String, String> {
        val result = linkedMapOf<String, String>()
        text.lineSequence().map { it.trimEnd() }.filter { it.isNotBlank() }.forEach { line ->
            val match = Regex("^([0-9a-fA-F]{64})\\s{2}(.+)$").matchEntire(line)
                ?: error("Invalid checksums.sha256 line: $line")
            val path = safeRelativePath(match.groupValues[2])
            require(result.put(path, match.groupValues[1].lowercase(Locale.US)) == null) {
                "Duplicate checksum path: $path"
            }
        }
        return result
    }
}
