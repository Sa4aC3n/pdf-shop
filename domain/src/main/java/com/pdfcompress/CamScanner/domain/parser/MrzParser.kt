package com.pdfcompress.CamScanner.domain.parser

import com.pdfcompress.CamScanner.domain.model.PassportMrzData
import java.util.Calendar
import java.util.Date
import java.util.Locale

object MrzParser {

    private val COUNTRY_NAMES = mapOf(
        "IRQ" to Pair("العراق", "Iraq"),
        "EGY" to Pair("مصر", "Egypt"),
        "SAU" to Pair("المملكة العربية السعودية", "Saudi Arabia"),
        "SYR" to Pair("سوريا", "Syria"),
        "JOR" to Pair("الأردن", "Jordan"),
        "LBN" to Pair("لبنان", "Lebanon"),
        "ARE" to Pair("الإمارات العربية المتحدة", "United Arab Emirates"),
        "KWT" to Pair("الكويت", "Kuwait"),
        "QAT" to Pair("قطر", "Qatar"),
        "BHR" to Pair("البحرين", "Bahrain"),
        "OMN" to Pair("سلطنة عُمان", "Oman"),
        "YEM" to Pair("اليمن", "Yemen"),
        "DZA" to Pair("الجزائر", "Algeria"),
        "MAR" to Pair("المغرب", "Morocco"),
        "TUN" to Pair("تونس", "Tunisia"),
        "LBY" to Pair("ليبيا", "Libya"),
        "SDN" to Pair("السودان", "Sudan"),
        "PSE" to Pair("فلسطين", "Palestine"),
        "TUR" to Pair("تركيا", "Turkey"),
        "IRN" to Pair("إيران", "Iran"),
        "USA" to Pair("الولايات المتحدة الأمريكية", "United States"),
        "GBR" to Pair("المملكة المتحدة", "United Kingdom"),
        "DEU" to Pair("ألمانيا", "Germany"),
        "FRA" to Pair("فرنسا", "France"),
        "ITA" to Pair("إيطاليا", "Italy"),
        "ESP" to Pair("إسبانيا", "Spain"),
        "CAN" to Pair("كندا", "Canada"),
        "AUS" to Pair("أستراليا", "Australia"),
        "IND" to Pair("الهند", "India"),
        "PAK" to Pair("باكستان", "Pakistan"),
        "CHN" to Pair("الصين", "China"),
        "RUS" to Pair("روسيا", "Russia"),
        "SWE" to Pair("السويد", "Sweden"),
        "NLD" to Pair("هولندا", "Netherlands"),
        "CHE" to Pair("سويسرا", "Switzerland"),
        "BEL" to Pair("بلجيكا", "Belgium"),
        "AUT" to Pair("النمسا", "Austria"),
        "MYS" to Pair("ماليزيا", "Malaysia"),
        "IDN" to Pair("إندونيسيا", "Indonesia")
    )

    fun parse(rawText: String): PassportMrzData? {
        if (rawText.isBlank()) return null

        // 1. First, search for direct contiguous TD3 block (P<... or P[A-Z]...)
        val contiguousMatch = findContiguousTd3(rawText)
        if (contiguousMatch != null) {
            val parsed = parseTd3(contiguousMatch.first, contiguousMatch.second)
            if (parsed != null) return parsed
        }

        // 2. Extract and clean individual lines
        val lines = extractCandidateLines(rawText)
        if (lines.isEmpty()) return null

        // Pass A: Strict TD3 (44 chars)
        for (i in 0 until lines.size - 1) {
            val line1 = lines[i]
            val line2 = lines[i + 1]
            if (line1.length == 44 && line2.length == 44 && (line1.startsWith("P") || line1.startsWith("V"))) {
                val parsed = parseTd3(line1, line2)
                if (parsed != null) return parsed
            }
        }

        // Pass B: Fuzzy TD3 with padding/trimming (tolerates length 40..48)
        for (i in 0 until lines.size - 1) {
            val l1Raw = lines[i]
            val l2Raw = lines[i + 1]
            if ((l1Raw.startsWith("P") || l1Raw.startsWith("V") || l1Raw.contains("<<")) && l1Raw.length in 38..52 && l2Raw.length in 38..52) {
                val l1 = padOrTrim(l1Raw, 44)
                val l2 = padOrTrim(l2Raw, 44)
                val parsed = parseTd3(l1, l2)
                if (parsed != null) return parsed
            }
        }

        // Pass C: Try any 2 consecutive lines with length ~44
        for (i in 0 until lines.size - 1) {
            val l1 = padOrTrim(lines[i], 44)
            val l2 = padOrTrim(lines[i + 1], 44)
            val parsed = parseTd3(l1, l2)
            if (parsed != null) return parsed
        }

        // Pass D: Try TD1 (Identity Cards: 3 lines of 30 chars)
        for (i in 0 until lines.size - 2) {
            val line1 = padOrTrim(lines[i], 30)
            val line2 = padOrTrim(lines[i + 1], 30)
            val line3 = padOrTrim(lines[i + 2], 30)
            if (line1.length == 30 && line2.length == 30 && line3.length == 30) {
                val parsed = parseTd1(line1, line2, line3)
                if (parsed != null) return parsed
            }
        }

        return null
    }

    /**
     * Attempts to find a contiguous 88-char stream starting with 'P<' or 'P[A-Z]'
     */
    private fun findContiguousTd3(raw: String): Pair<String, String>? {
        val cleaned = normalizeChars(raw).replace("\n", "").replace("\r", "")
        val regex = Regex("""(P[A-Z<][A-Z<]{42})([A-Z0-9<]{44})""")
        val match = regex.find(cleaned)
        return if (match != null) {
            Pair(match.groupValues[1], match.groupValues[2])
        } else {
            // Also try finding P< and slicing 44 and 44
            val pIndex = cleaned.indexOf("P<")
            if (pIndex >= 0 && cleaned.length >= pIndex + 88) {
                val l1 = cleaned.substring(pIndex, pIndex + 44)
                val l2 = cleaned.substring(pIndex + 44, pIndex + 88)
                Pair(l1, l2)
            } else {
                null
            }
        }
    }

    private fun normalizeChars(text: String): String {
        return text.uppercase(Locale.US)
            .replace("«", "<")
            .replace("‹", "<")
            .replace(">", "<")
            .replace("(", "<")
            .replace(")", "<")
            .replace("[", "<")
            .replace("]", "<")
            .replace("{", "<")
            .replace("}", "<")
            .replace("|", "")
            .replace("~", "<")
            .replace("-", "<")
            .replace("_", "<")
            .replace("/", "<")
            .replace("\\", "<")
            .replace("*", "<")
            .replace("+", "<")
    }

    private fun extractCandidateLines(raw: String): List<String> {
        val candidates = mutableListOf<String>()
        val rawLines = raw.lines()

        for (line in rawLines) {
            var cleaned = normalizeChars(line)

            // Multiple spaces in MRZ are almost always filler '<'
            cleaned = cleaned.replace("  ", "<<").replace(" ", "")

            // Keep only A-Z, 0-9, and <
            cleaned = cleaned.filter { it in 'A'..'Z' || it in '0'..'9' || it == '<' }

            if (cleaned.length in 25..55 && (cleaned.contains("<") || cleaned.startsWith("P"))) {
                candidates.add(cleaned)
            }
        }
        return candidates
    }

    private fun padOrTrim(line: String, targetLength: Int): String {
        return when {
            line.length == targetLength -> line
            line.length > targetLength -> line.substring(0, targetLength)
            else -> line.padEnd(targetLength, '<')
        }
    }

    /**
     * ICAO 9303 TD3 (Passports, 2 lines of 44 characters)
     */
    private fun parseTd3(line1Raw: String, line2Raw: String): PassportMrzData? {
        try {
            val line1 = padOrTrim(line1Raw, 44)
            val line2 = padOrTrim(line2Raw, 44)

            // Document code (e.g., P< or PM)
            val docCodeRaw = line1.substring(0, 2)
            val docCode = if (docCodeRaw.startsWith("P") || docCodeRaw.startsWith("V")) {
                docCodeRaw.replace("<", "")
            } else {
                "P"
            }

            // Country code: 3 letters (auto-correct any digits to letters)
            val countryCodeRaw = line1.substring(2, 5)
            val countryCode = toLetters(countryCodeRaw)

            val namesBlock = line1.substring(5, 44)
            val (surname, givenNames) = parseNames(namesBlock)

            // Document Number (chars 0..8 of line 2) + Check Digit (char 9)
            val (docNum, docNumCheckOk) = parseAndVerifyDocNum(line2.substring(0, 9), line2[9])

            // Nationality code: 3 letters
            val nationalityCode = toLetters(line2.substring(10, 13))

            // Date of birth (chars 13..18) + Check Digit (char 19)
            val dobDigits = toDigits(line2.substring(13, 19))
            val dobCheckChar = toDigitChar(line2[19])
            val dobCheckOk = verifyCheckDigit(dobDigits, dobCheckChar)
            val dobFormatted = formatMrzDate(dobDigits, isBirthDate = true)

            // Sex (char 20)
            val sexRaw = line2[20]
            val sex = when (sexRaw) {
                'M' -> "M"
                'F' -> "F"
                '<' -> "X"
                else -> if (sexRaw in listOf('0', 'O', 'K')) "X" else "X"
            }
            val sexLabel = when (sex) {
                "M" -> "ذكر / Male"
                "F" -> "أنثى / Female"
                else -> "غير محدد / Unspecified"
            }

            // Expiry date (chars 21..26) + Check Digit (char 27)
            val expiryDigits = toDigits(line2.substring(21, 27))
            val expiryCheckChar = toDigitChar(line2[27])
            val expiryCheckOk = verifyCheckDigit(expiryDigits, expiryCheckChar)
            val expiryFormatted = formatMrzDate(expiryDigits, isBirthDate = false)
            val isExpired = isDateExpired(expiryDigits)

            // Personal number (chars 28..41)
            val personalNumberRaw = line2.substring(28, 42).replace("<", "").trim()
            val personalNumber = if (personalNumberRaw.isNotEmpty()) personalNumberRaw else null

            // Composite check digit (char 43)
            val compositeCheck = line2[43]
            val compositeSource = line2.substring(0, 10) + line2.substring(13, 20) + line2.substring(21, 43)
            val compositeCheckOk = verifyCheckDigit(compositeSource, compositeCheck)

            val countryNamePair = COUNTRY_NAMES[countryCode]
            val countryName = countryNamePair?.let { "${it.first} (${it.second})" } ?: countryCode

            val nationalityPair = COUNTRY_NAMES[nationalityCode]
            val nationalityName = nationalityPair?.let { "${it.first} (${it.second})" } ?: nationalityCode

            val fullName = if (givenNames.isNotBlank()) "$givenNames $surname" else surname
            val docType = if (docCode.startsWith("P")) "جواز سفر (Passport)" else "وثيقة سفر رسمية (Travel Document)"

            val isCompleteValid = docNumCheckOk && dobCheckOk && expiryCheckOk

            // Must have extracted at least a valid document number or name
            if (docNum.isBlank() && surname.isBlank()) return null

            return PassportMrzData(
                rawMrz = "$line1\n$line2",
                documentType = docType,
                countryCode = countryCode,
                countryName = countryName,
                documentNumber = docNum,
                documentNumberCheckOk = docNumCheckOk,
                nationality = nationalityCode,
                nationalityName = nationalityName,
                dateOfBirth = dobFormatted,
                dateOfBirthRaw = dobDigits,
                dateOfBirthCheckOk = dobCheckOk,
                sex = sex,
                sexLabel = sexLabel,
                expiryDate = expiryFormatted,
                expiryDateRaw = expiryDigits,
                expiryDateCheckOk = expiryCheckOk,
                isExpired = isExpired,
                personalNumber = personalNumber,
                surname = surname,
                givenNames = givenNames,
                fullName = fullName,
                isCompositeCheckOk = compositeCheckOk,
                isCompleteValid = isCompleteValid
            )
        } catch (_: Exception) {
            return null
        }
    }

    /**
     * ICAO 9303 TD1 (Identity Cards, 3 lines of 30 characters)
     */
    private fun parseTd1(line1: String, line2: String, line3: String): PassportMrzData? {
        try {
            val docCode = line1.substring(0, 2).replace("<", "")
            val countryCode = toLetters(line1.substring(2, 5))
            val (docNum, docNumCheckOk) = parseAndVerifyDocNum(line1.substring(5, 14), line1[14])

            val dobRaw = toDigits(line2.substring(0, 6))
            val dobCheck = toDigitChar(line2[6])
            val dobCheckOk = verifyCheckDigit(dobRaw, dobCheck)
            val dobFormatted = formatMrzDate(dobRaw, isBirthDate = true)

            val sexRaw = line2[7]
            val sex = when (sexRaw) {
                'M' -> "M"
                'F' -> "F"
                else -> "X"
            }
            val sexLabel = when (sex) {
                "M" -> "ذكر / Male"
                "F" -> "أنثى / Female"
                else -> "غير محدد / Unspecified"
            }

            val expiryRaw = toDigits(line2.substring(8, 14))
            val expiryCheck = toDigitChar(line2[14])
            val expiryCheckOk = verifyCheckDigit(expiryRaw, expiryCheck)
            val expiryFormatted = formatMrzDate(expiryRaw, isBirthDate = false)
            val isExpired = isDateExpired(expiryRaw)

            val nationalityCode = toLetters(line2.substring(15, 18))
            val (surname, givenNames) = parseNames(line3)

            val countryNamePair = COUNTRY_NAMES[countryCode]
            val countryName = countryNamePair?.let { "${it.first} (${it.second})" } ?: countryCode

            val nationalityPair = COUNTRY_NAMES[nationalityCode]
            val nationalityName = nationalityPair?.let { "${it.first} (${it.second})" } ?: nationalityCode

            val fullName = if (givenNames.isNotBlank()) "$givenNames $surname" else surname
            val docType = "بطاقة هوية وطنية (National ID)"

            return PassportMrzData(
                rawMrz = "$line1\n$line2\n$line3",
                documentType = docType,
                countryCode = countryCode,
                countryName = countryName,
                documentNumber = docNum,
                documentNumberCheckOk = docNumCheckOk,
                nationality = nationalityCode,
                nationalityName = nationalityName,
                dateOfBirth = dobFormatted,
                dateOfBirthRaw = dobRaw,
                dateOfBirthCheckOk = dobCheckOk,
                sex = sex,
                sexLabel = sexLabel,
                expiryDate = expiryFormatted,
                expiryDateRaw = expiryRaw,
                expiryDateCheckOk = expiryCheckOk,
                isExpired = isExpired,
                personalNumber = null,
                surname = surname,
                givenNames = givenNames,
                fullName = fullName,
                isCompositeCheckOk = true,
                isCompleteValid = docNumCheckOk && dobCheckOk && expiryCheckOk
            )
        } catch (_: Exception) {
            return null
        }
    }

    private fun parseAndVerifyDocNum(raw: String, checkChar: Char): Pair<String, Boolean> {
        val cleaned = raw.replace("<", "").trim()
        val expectedDigit = toDigitChar(checkChar)

        if (verifyCheckDigit(raw, expectedDigit)) {
            return Pair(cleaned, true)
        }

        // Try auto-correcting single OCR misidentifications
        val candidates = mutableListOf<String>()
        candidates.add(raw)
        // Common substitutions: O <-> 0, I <-> 1, Z <-> 2, S <-> 5, B <-> 8
        val charArray = raw.toCharArray()
        for (i in charArray.indices) {
            val original = charArray[i]
            val alt = when (original) {
                'O', 'Q', 'D' -> '0'
                '0' -> 'O'
                'I', 'L' -> '1'
                '1' -> 'I'
                'Z' -> '2'
                '2' -> 'Z'
                'S' -> '5'
                '5' -> 'S'
                'B' -> '8'
                '8' -> 'B'
                else -> null
            }
            if (alt != null) {
                charArray[i] = alt
                val attempt = String(charArray)
                if (verifyCheckDigit(attempt, expectedDigit)) {
                    return Pair(attempt.replace("<", "").trim(), true)
                }
                charArray[i] = original
            }
        }

        return Pair(cleaned, false)
    }

    private fun toLetters(str: String): String {
        return str.map { c ->
            when (c) {
                '0' -> 'O'
                '1' -> 'I'
                '2' -> 'Z'
                '5' -> 'S'
                '8' -> 'B'
                else -> c
            }
        }.joinToString("")
    }

    private fun toDigits(str: String): String {
        return str.map { c ->
            when (c) {
                'O', 'Q', 'D', 'U' -> '0'
                'I', 'L' -> '1'
                'Z' -> '2'
                'S' -> '5'
                'B' -> '8'
                in '0'..'9' -> c
                else -> '0'
            }
        }.joinToString("")
    }

    private fun toDigitChar(c: Char): Char {
        return when (c) {
            'O', 'Q', 'D' -> '0'
            'I', 'L' -> '1'
            'Z' -> '2'
            'S' -> '5'
            'B' -> '8'
            in '0'..'9' -> c
            else -> '0'
        }
    }

    private fun parseNames(raw: String): Pair<String, String> {
        val parts = raw.split("<<")
        val surname = parts.getOrNull(0)?.replace("<", " ")?.trim() ?: ""
        val givenNames = parts.getOrNull(1)?.replace("<", " ")?.trim() ?: ""
        return Pair(cleanName(surname), cleanName(givenNames))
    }

    private fun cleanName(name: String): String {
        return name.split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { it.lowercase(Locale.US).replaceFirstChar { c -> c.uppercase(Locale.US) } }
    }

    /**
     * ICAO 9303 Weighting: 7, 3, 1, 7, 3, 1...
     */
    fun calculateCheckDigit(value: String): Char {
        val weights = intArrayOf(7, 3, 1)
        var sum = 0
        for (i in value.indices) {
            val char = value[i]
            val charVal = when (char) {
                in '0'..'9' -> char - '0'
                in 'A'..'Z' -> char - 'A' + 10
                '<' -> 0
                else -> 0
            }
            sum += charVal * weights[i % 3]
        }
        return ((sum % 10) + '0'.code).toChar()
    }

    fun verifyCheckDigit(value: String, expectedCheckDigit: Char): Boolean {
        if (expectedCheckDigit !in '0'..'9') return false
        return calculateCheckDigit(value) == expectedCheckDigit
    }

    private fun formatMrzDate(yymmdd: String, isBirthDate: Boolean): String {
        if (yymmdd.length != 6) return yymmdd
        try {
            val yy = yymmdd.substring(0, 2).toInt()
            val mm = yymmdd.substring(2, 4).toInt()
            val dd = yymmdd.substring(4, 6).toInt()

            val currentYear = Calendar.getInstance().get(Calendar.YEAR) % 100
            val fullYear = if (isBirthDate) {
                if (yy <= currentYear) 2000 + yy else 1900 + yy
            } else {
                if (yy >= (currentYear - 20)) 2000 + yy else 1900 + yy
            }

            val monthNames = arrayOf(
                "يناير (Jan)", "فبراير (Feb)", "مارس (Mar)", "أبريل (Apr)",
                "مايو (May)", "يونيو (Jun)", "يوليو (Jul)", "أغسطس (Aug)",
                "سبتمبر (Sep)", "أكتوبر (Oct)", "نوفمبر (Nov)", "ديسمبر (Dec)"
            )
            val monthLabel = if (mm in 1..12) monthNames[mm - 1] else "$mm"
            return "$dd $monthLabel $fullYear"
        } catch (_: Exception) {
            return yymmdd
        }
    }

    private fun isDateExpired(yymmdd: String): Boolean {
        if (yymmdd.length != 6) return false
        try {
            val yy = yymmdd.substring(0, 2).toInt()
            val mm = yymmdd.substring(2, 4).toInt()
            val dd = yymmdd.substring(4, 6).toInt()

            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val currentYearTwoDigits = currentYear % 100
            val fullYear = if (yy >= (currentYearTwoDigits - 20)) 2000 + yy else 1900 + yy

            val cal = Calendar.getInstance()
            cal.set(fullYear, mm - 1, dd, 23, 59, 59)
            return Date().after(cal.time)
        } catch (_: Exception) {
            return false
        }
    }
}
