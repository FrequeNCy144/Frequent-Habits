package com.example.data

import android.content.Context
import android.net.Uri
import java.io.BufferedReader
import java.text.SimpleDateFormat
import java.util.*

object CsvImportUtilities {

    fun readCsvLinesFromReader(reader: BufferedReader): List<List<String>> {
        val rawLines = mutableListOf<String>()
        var line: String?
        while (true) {
            line = reader.readLine()
            if (line == null) break
            if (line.isNotBlank()) {
                rawLines.add(line)
            }
        }
        if (rawLines.isEmpty()) return emptyList()

        val delimiter = detectDelimiter(rawLines.first())
        val parsedLines = mutableListOf<List<String>>()

        for (row in rawLines) {
            parsedLines.add(parseCsvRow(row, delimiter))
        }

        return parsedLines
    }

    fun detectDelimiter(sampleLine: String): Char {
        val commaCount = sampleLine.count { it == ',' }
        val semicolonCount = sampleLine.count { it == ';' }
        val tabCount = sampleLine.count { it == '\t' }

        return when {
            semicolonCount > commaCount && semicolonCount > tabCount -> ';'
            tabCount > commaCount && tabCount > semicolonCount -> '\t'
            else -> ','
        }
    }

    fun parseCsvRow(line: String, delimiter: Char): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var insideQuotes = false

        for (ch in line) {
            when {
                ch == '"' -> {
                    insideQuotes = !insideQuotes
                }
                ch == delimiter && !insideQuotes -> {
                    tokens.add(sb.toString())
                    sb.clear()
                }
                else -> {
                    sb.append(ch)
                }
            }
        }
        tokens.add(sb.toString())
        return tokens
    }

    fun normalizeDate(rawDateStr: String): String? {
        val clean = rawDateStr.trim().trim('"', '\'')
        if (clean.isBlank()) return null

        // Try YYYY-MM-DD
        if (clean.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))) {
            return clean
        }

        val dateFormats = listOf(
            "yyyy/MM/dd",
            "yyyy.MM.dd",
            "dd.MM.yyyy",
            "dd/MM/yyyy",
            "MM/dd/yyyy",
            "dd-MM-yyyy",
            "MM-dd-yyyy",
            "yyyyMMdd"
        )

        for (formatStr in dateFormats) {
            try {
                val sdf = SimpleDateFormat(formatStr, Locale.US)
                sdf.isLenient = false
                val date = sdf.parse(clean)
                if (date != null) {
                    val outSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    return outSdf.format(date)
                }
            } catch (_: Exception) {}
        }

        return null
    }

    fun parseLogValue(rawVal: String): Float {
        val clean = rawVal.trim().lowercase(Locale.ROOT).trim('"', '\'')
        if (clean.isBlank() || clean == "-1") {
            return 0f
        }

        if (clean == "0" || clean == "false" || clean == "no" || clean == "n" || clean == "missed" || clean.startsWith("no")) {
            return -1f
        }

        if (clean == "1" || clean == "2" || clean == "true" || clean == "yes" || clean == "y" || clean == "x" || clean == "check" || clean == "completed" || clean == "done" || clean.startsWith("yes")) {
            return 1f
        }

        return clean.toFloatOrNull() ?: 0f
    }

    fun formatDateRange(dates: Set<String>): String {
        if (dates.isEmpty()) return "Keine Einträge"
        val sorted = dates.sorted()
        val first = formatDateDisplay(sorted.first())
        val last = formatDateDisplay(sorted.last())
        return if (first == last) first else "$first bis $last"
    }

    fun formatDateDisplay(dateStr: String): String {
        return try {
            val sdfIn = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val sdfOut = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY)
            val d = sdfIn.parse(dateStr)
            if (d != null) sdfOut.format(d) else dateStr
        } catch (_: Exception) {
            dateStr
        }
    }

    fun getRandomColorName(index: Int): String {
        val colors = listOf("purple", "blue", "green", "orange", "red", "pink", "cyan", "yellow", "indigo", "teal")
        return colors[index % colors.size]
    }

    fun mapHexToColorName(hex: String, index: Int): String {
        val cleanHex = hex.trim().lowercase(Locale.ROOT).removePrefix("#")
        return when {
            cleanHex.contains("ff8f") || cleanHex.contains("ff98") || cleanHex.contains("orange") -> "orange"
            cleanHex.contains("4ca") || cleanHex.contains("green") || cleanHex.contains("4ef") -> "green"
            cleanHex.contains("219") || cleanHex.contains("blue") || cleanHex.contains("1e8") -> "blue"
            cleanHex.contains("9c2") || cleanHex.contains("purple") || cleanHex.contains("783") -> "purple"
            cleanHex.contains("f44") || cleanHex.contains("red") || cleanHex.contains("e91") -> "red"
            cleanHex.contains("e91") || cleanHex.contains("pink") -> "pink"
            cleanHex.contains("00b") || cleanHex.contains("cyan") -> "cyan"
            else -> getRandomColorName(index)
        }
    }

    fun getFileNameFromUri(context: Context, uri: Uri): String {
        var name = ""
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex != -1) {
                    name = cursor.getString(nameIndex)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return if (name.isBlank()) uri.lastPathSegment ?: "export.csv" else name
    }
}
