package com.example.audiotoggle

import java.io.BufferedReader
import java.io.InputStreamReader

class ShizukuShellService : IShizukuShellService.Stub() {

    override fun exec(command: String): Int {
        val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
        drain(process.inputStream)
        drain(process.errorStream)
        return process.waitFor()
    }

    override fun execReadLine(command: String): String {
        val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
        val line = BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
            reader.readLine() ?: ""
        }
        drain(process.errorStream)
        process.waitFor()
        return line
    }

    override fun destroy() {
        // Required by Shizuku user service teardown contract.
        kotlin.system.exitProcess(0)
    }

    private fun drain(stream: java.io.InputStream) {
        BufferedReader(InputStreamReader(stream)).use { reader ->
            while (reader.readLine() != null) {
                // Drain output to avoid blocking.
            }
        }
    }
}
