package com.pdfcompress.CamScanner.domain.parser

import com.pdfcompress.CamScanner.domain.model.PassportMrzData
import java.text.SimpleDateFormat
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
        val lines = extractCandidateLines(rawText)
        if (lines.isEmpty()) return null

        // Try TD3 (Passports: 2 lines of 44 chars)
        for (i in 0 until lines.size - 1) {
            val line1 = lines[i]
            val line2 = lines[i + 1]
            if (line1.length == 44 && line2.length == 44) {
                val parsed = parseTd3(line1, line2)
                if (parsed != null) return parsed
            }
        }

        // Try TD1 (ID Cards: 3 lines of 30 chars)
        for (i in 0 until lines.size - 2) {
            val line1 = lines[i]
            val line2 = lines[i + 1]
            val line3 = lines[i + 2]
            if (line1.length == 30 && line2.length == 30 && line3.length == 30) {
                val parsed = parseTd1(line1, line2, line3)
                if (parsed != null) return parsed
            }
        }

        // Try fuzzy match for TD3 if characters are slightly trimmed/padded
        for (i in 0 until lines.size - 1) {
            val l1 = padOrTrim(lines[i], 44)
            val l2 = padOrTrim(lines[i + 1], 44)
            if (l1.startsWith("P") || l1.startsWith("V")) {
                val parsed = parseTd3(l1, l2)
                if (parsed != null) return parsed
            }
        }

        return null
    }

    private fun extractCandidateLines(raw: String): List<String> {
        val candidates = mutableListOf<String>()
        val rawLines = raw.lines()

        for (line in rawLines) {
            // Clean up common OCR artifacts
            var cleaned = line.uppercase(Locale.US)
                .replace(" ", "")
                .replace("«", "<")
                .replace("‹", "<")
                .replace(">", "<")
                .replace("|", "")
                .replace("~", "<")
                .replace("-", "<")

            // Keep only A-Z, 0-9, and <
            cleaned = cleaned.filter { it in 'A'..'Z' || it in '0'..'9' || it == '<' }

            if (cleaned.length in 28..48 && cleaned.count { it == '<' } >= 2) {
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
    private fun parseTd3(line1: String, line2: String): PassportMrzData? {
        try {
            val docCode = line1.substring(0, 2).replace("<", "")
            val countryCode = line1.substring(2, 5)
            val namesBlock = line1.substring(5, 44)

            val (surname, givenNames) = parseNames(namesBlock)

            val docNumRaw = line2.substring(0, 9).replace("<", "")
            val docNumCheck = line2.substring(9, 10).firstOrNull() ?: '0'
            val docNumCheckOk = verifyCheckDigit(line2.substring(0, 9), docNumCheck)

            val nationalityCode = line2.substring(10, 13)

            val dobRaw = line2.substring(13, 19)
            val dobCheck = line2.substring(19, 20).firstOrNull() ?: '0'
            val dobCheckOk = verifyCheckDigit(dobRaw, dobCheck)
            val dobFormatted = formatMrzDate(dobRaw, isBirthDate = true)

            val sexRaw = line2.substring(20, 21)
            val sex = when (sexRaw) {
                "M" -> "M"
                "F" -> "F"
                else -> "X"
            }
            val sexLabel = when (sex) {
                "M" -> "ذكر / Male"
                "F" -> "أنثى / Female"
                else -> "غير محدد / Unspecified"
            }

            val expiryRaw = line2.substring(21, 27)
            val expiryCheck = line2.substring(27, 28).firstOrNull() ?: '0'
            val expiryCheckOk = verifyCheckDigit(expiryRaw, expiryCheck)
            val expiryFormatted = formatMrzDate(expiryRaw, isBirthDate = false)
            val isExpired = isDateExpired(expiryRaw)

            val personalNumberRaw = line2.substring(28, 42).replace("<", "").trim()
            val personalNumber = if (personalNumberRaw.isNotEmpty()) personalNumberRaw else null

            val compositeCheck = line2.substring(43, 44).firstOrNull() ?: '0'
            // Composite includes docNum + check + dob + check + expiry + check + personal + check
            val compositeSource = line2.substring(0, 10) + line2.substring(13, 20) + line2.substring(21, 43)
            val compositeCheckOk = verifyCheckDigit(compositeSource, compositeCheck)

            val countryNamePair = COUNTRY_NAMES[countryCode]
            val countryName = countryNamePair?.let { "${it.first} (${it.second})" } ?: countryCode

            val nationalityPair = COUNTRY_NAMES[nationalityCode]
            val nationalityName = nationalityPair?.let { "${it.first} (${it.second})" } ?: nationalityCode

            val fullName = if (givenNames.isNotBlank()) "$givenNames $surname" else surname

            val docType = if (docCode.startsWith("P")) "جواز سفر (Passport)" else "وثيقة سفر رسمية (Travel Document)"

            val isCompleteValid = docNumCheckOk && dobCheckOk && expiryCheckOk

            return PassportMrzData(
                rawMrz = "$line1\n$line2",
                documentType = docType,
                countryCode = countryCode,
                countryName = countryName,
                documentNumber = docNumRaw,
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
            val countryCode = line1.substring(2, 5)
            val docNumRaw = line1.substring(5, 14).replace("<", "")
            val docNumCheck = line1.substring(14, 15).firstOrNull() ?: '0'
            val docNumCheckOk = verifyCheckDigit(line1.substring(5, 14), docNumCheck)

            val dobRaw = line2.substring(0, 6)
            val dobCheck = line2.substring(6, 7).firstOrNull() ?: '0'
            val dobCheckOk = verifyCheckDigit(dobRaw, dobCheck)
            val dobFormatted = formatMrzDate(dobRaw, isBirthDate = true)

            val sexRaw = line2.substring(7, 8)
            val sex = when (sexRaw) {
                "M" -> "M"
                "F" -> "F"
                else -> "X"
            }
            val sexLabel = when (sex) {
                "M" -> "ذكر / Male"
                "F" -> "أنثى / Female"
                else -> "غير محدد / Unspecified"
            }

            val expiryRaw = line2.substring(8, 14)
            val expiryCheck = line2.substring(14, 15).firstOrNull() ?: '0'
            val expiryCheckOk = verifyCheckDigit(expiryRaw, expiryCheck)
            val expiryFormatted = formatMrzDate(expiryRaw, isBirthDate = false)
            val isExpired = isDateExpired(expiryRaw)

            val nationalityCode = line2.substring(15, 18)

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
                documentNumber = docNumRaw,
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

    private fun parseNames(raw: String): Pair<String, String> {
        val parts = raw.split("<<")
        val surname = parts.getOrNull(0)?.replace("<", " ")?.trim() ?: ""
        val givenNames = parts.getOrNull(1)?.replace("<", " ")?.trim() ?: ""
        return Pair(cleanName(surname), cleanName(givenNames))
    }

    private fun cleanName(name: String): String {
        return name.split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { it.lowercase().replaceFirstChar { c -> c.uppercase() } }
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
