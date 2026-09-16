package com.example.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipInputStream

data class ImportedLogData(
    val date: String, // "yyyy-MM-dd"
    val value: Float = 1.0f,
    val isPaused: Boolean = false
)

data class ImportedHabitData(
    val name: String,
    val description: String = "",
    val category: String = "Importiert",
    val icon: String = "sparkle",
    val color: String = "purple",
    val type: String = "BINARY", // "BINARY" or "NUMBER"
    val unit: String = "",
    val targetValue: Float = 1.0f,
    val frequency: String = "DAILY",
    val logs: List<ImportedLogData> = emptyList()
)

data class CsvImportPreview(
    val sourceName: String, // e.g. "Loop Habit Tracker", "HabitBull / Bull Tracker", "Generisches CSV"
    val habits: List<ImportedHabitData>,
    val totalLogsCount: Int,
    val dateRange: String,
    val errorMessage: String? = null
)

object CsvImporter {

    /**
     * Reads a file from Uri (either CSV or ZIP) and analyzes its structure to generate an Import Preview.
     */
    suspend fun analyzeFile(context: Context, uri: Uri): CsvImportPreview = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: ""
            val fileName = getFileNameFromUri(context, uri).lowercase(Locale.ROOT)

            if (fileName.endsWith(".zip") || mimeType.contains("zip")) {
                return@withContext parseZipFile(context, uri)
            } else {
                return@withContext parseSingleCsvFile(context, uri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext CsvImportPreview(
                sourceName = "Unbekannt",
                habits = emptyList(),
                totalLogsCount = 0,
                dateRange = "-",
                errorMessage = "Fehler beim Lesen der Datei: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Imports the habits and logs from a CsvImportPreview into Room database.
     */
    suspend fun importDataToDatabase(
        context: Context,
        preview: CsvImportPreview,
        replaceExisting: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        if (preview.habits.isEmpty()) return@withContext false
        val db = AppDatabase.getDatabase(context)

        try {
            if (replaceExisting) {
                db.habitDao().clearAllLogs()
                db.habitDao().clearAllHabits()
            }

            val existingHabits = db.habitDao().getAllHabitsRaw()
            val existingNamesMap = existingHabits.associateBy { it.name.trim().lowercase(Locale.ROOT) }

            for ((idx, importedHabit) in preview.habits.withIndex()) {
                val normalizedName = importedHabit.name.trim().lowercase(Locale.ROOT)
                val habitId: Int

                val existing = existingNamesMap[normalizedName]
                if (existing != null && !replaceExisting) {
                    habitId = existing.id
                } else {
                    var earliestDateMillis = System.currentTimeMillis()
                    if (importedHabit.logs.isNotEmpty()) {
                        try {
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                            var oldestDateStr = importedHabit.logs.first().date
                            for (log in importedHabit.logs) {
                                if (log.date < oldestDateStr) {
                                    oldestDateStr = log.date
                                }
                            }
                            val d = sdf.parse(oldestDateStr)
                            if (d != null && d.time < earliestDateMillis) {
                                earliestDateMillis = d.time
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    val habit = Habit(
                        name = importedHabit.name.trim(),
                        description = importedHabit.description,
                        category = importedHabit.category,
                        icon = importedHabit.icon.ifBlank { "sparkle" },
                        color = importedHabit.color.ifBlank { getRandomColorName(idx) },
                        type = if (importedHabit.type.contains("NUMBER") || importedHabit.type.contains("NUMERIC")) "NUMBER" else "BINARY",
                        unit = importedHabit.unit,
                        targetValue = if (importedHabit.targetValue > 0) importedHabit.targetValue else 1.0f,
                        frequency = importedHabit.frequency.ifBlank { "DAILY" },
                        sortOrder = idx,
                        startDate = earliestDateMillis,
                        createdAt = earliestDateMillis
                    )
                    habitId = db.habitDao().insertHabit(habit).toInt()
                }

                // Insert logs for this habit
                val logsToInsert = importedHabit.logs.map { logData ->
                    HabitLog(
                        habitId = habitId,
                        date = logData.date,
                        value = logData.value,
                        isPaused = logData.isPaused,
                        timestamp = System.currentTimeMillis()
                    )
                }

                for (log in logsToInsert) {
                    db.habitDao().insertLog(log)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // ----------------------------------------------------------------------------------
    // ZIP PARSER (LOOP HABIT TRACKER EXPORTS)
    // ----------------------------------------------------------------------------------
    private fun parseZipFile(context: Context, uri: Uri): CsvImportPreview {
        var habitsCsvLines: List<List<String>>? = null
        val checkmarkFiles = mutableMapOf<String, List<List<String>>>() // filename or habitname -> lines

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            ZipInputStream(inputStream).use { zipStream ->
                var entry = zipStream.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && entry.name.lowercase(Locale.ROOT).endsWith(".csv")) {
                        val pathParts = entry.name.split("/", "\\")
                        val simpleName = pathParts.last()
                        
                        val reader = BufferedReader(InputStreamReader(zipStream, Charsets.UTF_8))
                        val lines = readCsvLinesFromReader(reader)

                        if (simpleName.equals("Habits.csv", ignoreCase = true)) {
                            habitsCsvLines = lines
                        } else if (!simpleName.equals("Scores.csv", ignoreCase = true) && !simpleName.equals("History.csv", ignoreCase = true)) {
                            if (simpleName.equals("Checkmarks.csv", ignoreCase = true) && pathParts.size == 1) {
                                checkmarkFiles["Checkmarks.csv"] = lines
                            } else {
                                checkmarkFiles[entry.name] = lines
                            }
                        }
                    }
                    zipStream.closeEntry()
                    entry = zipStream.nextEntry
                }
            }
        }

        if (habitsCsvLines != null) {
            return parseLoopZipData(habitsCsvLines!!, checkmarkFiles)
        } else if (checkmarkFiles.isNotEmpty()) {
            // ZIP with multiple CSVs but no explicit Habits.csv
            val habitsList = mutableListOf<ImportedHabitData>()
            var totalLogs = 0
            val datesFound = mutableSetOf<String>()

            checkmarkFiles.forEach { (filename, lines) ->
                val pathParts = filename.split("/", "\\")
                val habitNameRaw = if (pathParts.size > 1 && pathParts.last().equals("Checkmarks.csv", ignoreCase = true)) {
                    pathParts[pathParts.size - 2]
                } else {
                    pathParts.last().removeSuffix(".csv").removeSuffix(".CSV")
                }
                val habitName = habitNameRaw.replace("_", " ")
                val logs = mutableListOf<ImportedLogData>()

                if (lines.isNotEmpty()) {
                    val firstRow = lines[0]
                    val hasHeader = firstRow.isNotEmpty() && normalizeDate(firstRow[0]) == null
                    val startIndex = if (hasHeader) 1 else 0

                    for (i in startIndex until lines.size) {
                        val row = lines[i]
                        if (row.size >= 2) {
                            val normDate = normalizeDate(row[0])
                            if (normDate != null) {
                                val valStr = row[1].trim()
                                val floatVal = parseLogValue(valStr)
                                if (floatVal != 0f) {
                                    logs.add(ImportedLogData(normDate, floatVal, isPaused = valStr == "3"))
                                    datesFound.add(normDate)
                                }
                            }
                        }
                    }
                }

                habitsList.add(ImportedHabitData(name = habitName, logs = logs))
                totalLogs += logs.size
            }

            val dateRange = formatDateRange(datesFound)
            return CsvImportPreview(
                sourceName = "Loop Habit Tracker (ZIP)",
                habits = habitsList,
                totalLogsCount = totalLogs,
                dateRange = dateRange
            )
        }

        return CsvImportPreview(
            sourceName = "ZIP Archiv",
            habits = emptyList(),
            totalLogsCount = 0,
            dateRange = "-",
            errorMessage = "Keine gültigen CSV-Dateien im ZIP-Archiv gefunden."
        )
    }

    private fun parseLoopZipData(
        habitsLines: List<List<String>>,
        checkmarkFiles: Map<String, List<List<String>>>
    ): CsvImportPreview {
        if (habitsLines.isEmpty()) {
            return CsvImportPreview("Loop Habit Tracker", emptyList(), 0, "-")
        }

        val header = habitsLines.first().map { it.trim().lowercase(Locale.ROOT) }
        val nameIdx = header.indexOfFirst { it.contains("name") }.coerceAtLeast(0)
        val typeIdx = header.indexOfFirst { it.contains("type") }
        val descIdx = header.indexOfFirst { it.contains("description") || it.contains("question") }
        val colorIdx = header.indexOfFirst { it.contains("color") }
        val unitIdx = header.indexOfFirst { it.contains("unit") }
        val targetIdx = header.indexOfFirst { it.contains("target") && it.contains("value") }

        val habitsList = mutableListOf<ImportedHabitData>()
        var totalLogs = 0
        val allDates = mutableSetOf<String>()

        // Look for combined Checkmarks.csv
        val combinedCheckmarks = checkmarkFiles["Checkmarks.csv"] ?: checkmarkFiles["checkmarks.csv"]

        for (rowIdx in 1 until habitsLines.size) {
            val row = habitsLines[rowIdx]
            if (row.size <= nameIdx) continue

            val name = row[nameIdx].trim()
            if (name.isBlank()) continue

            val rawType = if (typeIdx in 0 until row.size) row[typeIdx] else "0"
            val isNumeric = rawType == "1" || rawType.lowercase(Locale.ROOT).contains("num")

            val desc = if (descIdx in 0 until row.size) row[descIdx].trim() else ""
            val colorHex = if (colorIdx in 0 until row.size) row[colorIdx].trim() else ""
            val unit = if (unitIdx in 0 until row.size) row[unitIdx].trim() else ""
            val targetVal = if (targetIdx in 0 until row.size) row[targetIdx].toFloatOrNull() ?: 1f else 1f

            val habitColor = mapHexToColorName(colorHex, rowIdx)
            val logs = mutableListOf<ImportedLogData>()

            // Try to find checkmarks for this habit
            if (combinedCheckmarks != null && combinedCheckmarks.isNotEmpty()) {
                val checkHeader = combinedCheckmarks.first().map { it.trim().lowercase(Locale.ROOT) }
                val habitColIdx = checkHeader.indexOfFirst { it == name.lowercase(Locale.ROOT) || it.contains(name.lowercase(Locale.ROOT)) }

                if (habitColIdx != -1) {
                    for (cRowIdx in 1 until combinedCheckmarks.size) {
                        val cRow = combinedCheckmarks[cRowIdx]
                        if (cRow.size > habitColIdx) {
                            val normDate = normalizeDate(cRow[0])
                            if (normDate != null) {
                                val rawVal = cRow[habitColIdx].trim()
                                val floatVal = if (isNumeric) {
                                    val num = rawVal.toFloatOrNull() ?: 0f
                                    if (num > 100f) num / 1000f else num // Loop stores numbers x 1000
                                } else {
                                    parseLogValue(rawVal)
                                }

                                if (floatVal != 0f) {
                                    logs.add(ImportedLogData(normDate, floatVal, isPaused = rawVal == "3"))
                                    allDates.add(normDate)
                                }
                            }
                        }
                    }
                }
            } else {
                // Check for individual file <Name>.csv or Habit_<Idx>.csv or Folder
                val safeName = name.lowercase(Locale.ROOT).replace(" ", "").replace("_", "")
                val matchFile = checkmarkFiles.entries.find { (k, _) ->
                    val cleanK = k.lowercase(Locale.ROOT).replace(" ", "").replace("_", "")
                    cleanK.contains(safeName) || cleanK.contains("habit${rowIdx}")
                }?.value

                if (matchFile != null && matchFile.isNotEmpty()) {
                    val firstRow = matchFile[0]
                    val hasHeader = firstRow.isNotEmpty() && normalizeDate(firstRow[0]) == null
                    val startIndex = if (hasHeader) 1 else 0

                    for (cRowIdx in startIndex until matchFile.size) {
                        val cRow = matchFile[cRowIdx]
                        if (cRow.size >= 2) {
                            val normDate = normalizeDate(cRow[0])
                            if (normDate != null) {
                                val rawVal = cRow[1].trim()
                                val floatVal = if (isNumeric) {
                                    val num = rawVal.toFloatOrNull() ?: 0f
                                    if (num > 100f) num / 1000f else num
                                } else {
                                    parseLogValue(rawVal)
                                }

                                if (floatVal != 0f) {
                                    logs.add(ImportedLogData(normDate, floatVal, isPaused = rawVal == "3"))
                                    allDates.add(normDate)
                                }
                            }
                        }
                    }
                }
            }

            habitsList.add(
                ImportedHabitData(
                    name = name,
                    description = desc,
                    color = habitColor,
                    type = if (isNumeric) "NUMBER" else "BINARY",
                    unit = unit,
                    targetValue = targetVal,
                    logs = logs
                )
            )
            totalLogs += logs.size
        }

        return CsvImportPreview(
            sourceName = "Loop Habit Tracker (ZIP Export)",
            habits = habitsList,
            totalLogsCount = totalLogs,
            dateRange = formatDateRange(allDates)
        )
    }

    // ----------------------------------------------------------------------------------
    // SINGLE CSV PARSER (HABITBULL / LOOP HABITS.CSV / GENERIC MATRICES)
    // ----------------------------------------------------------------------------------
    private fun parseSingleCsvFile(context: Context, uri: Uri): CsvImportPreview {
        val lines = context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
            readCsvLinesFromReader(reader)
        } ?: emptyList()

        if (lines.isEmpty()) {
            return CsvImportPreview(
                sourceName = "CSV Datei",
                habits = emptyList(),
                totalLogsCount = 0,
                dateRange = "-",
                errorMessage = "Die CSV-Datei ist leer."
            )
        }

        val headerRow = lines.first().map { it.trim() }
        val headerLower = headerRow.map { it.lowercase(Locale.ROOT) }

        // 1. Is this a Loop Habits.csv?
        if (headerLower.any { it.contains("position") } && headerLower.any { it.contains("frequency") }) {
            return parseLoopZipData(lines, emptyMap())
        }

        // 2. Is this a Long-Format CSV (HabitBull / Generic with "Habit Name", "Date", "Value")?
        val habitNameColIdx = headerLower.indexOfFirst { it.contains("habit") || it == "name" || it == "title" || it == "gewohnheit" }
        val dateColIdx = headerLower.indexOfFirst { it.contains("date") || it == "datum" || it == "day" }
        val valueColIdx = headerLower.indexOfFirst { it.contains("value") || it.contains("status") || it.contains("completed") || it == "count" || it == "amount" || it == "ergebnis" || it == "wert" }
        val notesColIdx = headerLower.indexOfFirst { it.contains("note") || it.contains("notiz") || it.contains("comment") }

        if (habitNameColIdx != -1 && dateColIdx != -1) {
            // Standard Row/Long Format
            val habitsMap = mutableMapOf<String, MutableList<ImportedLogData>>()
            val allDates = mutableSetOf<String>()
            var totalLogs = 0

            for (i in 1 until lines.size) {
                val row = lines[i]
                if (row.size <= maxOf(habitNameColIdx, dateColIdx)) continue

                val habitName = row[habitNameColIdx].trim()
                val dateRaw = row[dateColIdx].trim()
                val normDate = normalizeDate(dateRaw) ?: continue

                if (habitName.isBlank()) continue

                val valStr = if (valueColIdx in 0 until row.size) row[valueColIdx].trim() else "1"
                val parsedVal = parseLogValue(valStr)

                if (parsedVal != 0f) {
                    val logsList = habitsMap.getOrPut(habitName) { mutableListOf() }
                    logsList.add(ImportedLogData(normDate, parsedVal, isPaused = valStr.lowercase(Locale.ROOT) == "skip" || valStr == "3"))
                    allDates.add(normDate)
                    totalLogs++
                }
            }

            val habitsList = habitsMap.entries.toList().mapIndexed { idx, entry ->
                ImportedHabitData(
                    name = entry.key,
                    color = getRandomColorName(idx),
                    logs = entry.value
                )
            }

            val detectedSource = if (lines.first().joinToString().lowercase(Locale.ROOT).contains("bull")) "Bull Habit Tracker (HabitBull)" else "Standard Habit CSV"

            return CsvImportPreview(
                sourceName = detectedSource,
                habits = habitsList,
                totalLogsCount = totalLogs,
                dateRange = formatDateRange(allDates)
            )
        }

        // 3. Is this a Wide / Matrix Format CSV?
        // Case A: Col 0 is Date, Col 1..N are Habit Names
        val firstColIsDate = lines.drop(1).take(5).all { row -> row.isNotEmpty() && normalizeDate(row[0]) != null }
        if (firstColIsDate && headerRow.size > 1) {
            val habitsList = mutableListOf<ImportedHabitData>()
            val habitNames = headerRow.drop(1)
            val allDates = mutableSetOf<String>()
            var totalLogs = 0

            habitNames.forEachIndexed { hIdx, hName ->
                if (hName.isNotBlank()) {
                    val colIdx = hIdx + 1
                    val logs = mutableListOf<ImportedLogData>()

                    for (rIdx in 1 until lines.size) {
                        val row = lines[rIdx]
                        if (row.size > colIdx) {
                            val normDate = normalizeDate(row[0])
                            if (normDate != null) {
                                val rawVal = row[colIdx].trim()
                                val parsedVal = parseLogValue(rawVal)
                                if (parsedVal != 0f) {
                                    logs.add(ImportedLogData(normDate, parsedVal, isPaused = rawVal == "3"))
                                    allDates.add(normDate)
                                }
                            }
                        }
                    }

                    habitsList.add(
                        ImportedHabitData(
                            name = hName.trim(),
                            color = getRandomColorName(hIdx),
                            logs = logs
                        )
                    )
                    totalLogs += logs.size
                }
            }

            return CsvImportPreview(
                sourceName = "Matrix CSV (Datum in Spalte 1)",
                habits = habitsList,
                totalLogsCount = totalLogs,
                dateRange = formatDateRange(allDates)
            )
        }

        // Case B: Col 0 is Habit Name, Col 1..N are Dates
        val headerDates = headerRow.drop(1).map { normalizeDate(it) }
        val headerIsDates = headerDates.count { it != null } >= (headerRow.size - 1) / 2
        if (headerIsDates && headerRow.size > 1) {
            val habitsList = mutableListOf<ImportedHabitData>()
            val allDates = mutableSetOf<String>()
            var totalLogs = 0

            for (rIdx in 1 until lines.size) {
                val row = lines[rIdx]
                if (row.isEmpty()) continue
                val hName = row[0].trim()
                if (hName.isBlank()) continue

                val logs = mutableListOf<ImportedLogData>()
                for (cIdx in 1 until row.size) {
                    val normDate = if (cIdx - 1 < headerDates.size) headerDates[cIdx - 1] else null
                    if (normDate != null) {
                        val rawVal = row[cIdx].trim()
                        val parsedVal = parseLogValue(rawVal)
                        if (parsedVal != 0f) {
                            logs.add(ImportedLogData(normDate, parsedVal, isPaused = rawVal == "3"))
                            allDates.add(normDate)
                        }
                    }
                }

                habitsList.add(
                    ImportedHabitData(
                        name = hName,
                        color = getRandomColorName(rIdx - 1),
                        logs = logs
                    )
                )
                totalLogs += logs.size
            }

            return CsvImportPreview(
                sourceName = "Matrix CSV (Gewohnheit in Spalte 1)",
                habits = habitsList,
                totalLogsCount = totalLogs,
                dateRange = formatDateRange(allDates)
            )
        }

        return CsvImportPreview(
            sourceName = "CSV Datei",
            habits = emptyList(),
            totalLogsCount = 0,
            dateRange = "-",
            errorMessage = "Das CSV-Format konnte nicht erkannt werden. Bitte stelle sicher, dass Spalten für Gewohnheiten und Datum vorhanden sind."
        )
    }

    // ----------------------------------------------------------------------------------
    // HELPERS & PARSING UTILITIES DELEGATED TO CsvImportUtilities
    // ----------------------------------------------------------------------------------

    private fun readCsvLinesFromReader(reader: BufferedReader): List<List<String>> = CsvImportUtilities.readCsvLinesFromReader(reader)
    private fun detectDelimiter(sampleLine: String): Char = CsvImportUtilities.detectDelimiter(sampleLine)
    private fun parseCsvRow(line: String, delimiter: Char): List<String> = CsvImportUtilities.parseCsvRow(line, delimiter)
    private fun normalizeDate(rawDateStr: String): String? = CsvImportUtilities.normalizeDate(rawDateStr)
    private fun parseLogValue(rawVal: String): Float = CsvImportUtilities.parseLogValue(rawVal)
    private fun formatDateRange(dates: Set<String>): String = CsvImportUtilities.formatDateRange(dates)
    private fun formatDateDisplay(dateStr: String): String = CsvImportUtilities.formatDateDisplay(dateStr)
    private fun getRandomColorName(index: Int): String = CsvImportUtilities.getRandomColorName(index)
    private fun mapHexToColorName(hex: String, index: Int): String = CsvImportUtilities.mapHexToColorName(hex, index)
    private fun getFileNameFromUri(context: Context, uri: Uri): String = CsvImportUtilities.getFileNameFromUri(context, uri)
}

