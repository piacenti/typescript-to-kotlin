package piacenti.typescript2kotlin

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File


internal class MainKtTest {
    @Test
    fun reactSpreadsheetTest() {
        val translator = Translator()
        val result =
            translator.translate(this::class.java.getResource("/input/index.d.ts")!!.readText(), "react-spreadsheet")
        val actuals=result.map {
            File(this::class.java.getResource("/").path, it.fileName).apply {
                writeText(it.code)
            }
        }
        val expectedFiles = File(this::class.java.getResource("/expected").path).listFiles()
            .filter { it.name.contains("reactSpreadsheet") }
        actuals.forEach { actual ->
            val expected = expectedFiles.first { it.name == actual.name }
            assertEquals(expected.readText(), actual.readText())
        }
    }
}