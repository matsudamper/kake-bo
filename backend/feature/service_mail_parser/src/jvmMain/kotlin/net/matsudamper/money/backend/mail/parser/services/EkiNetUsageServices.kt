package net.matsudamper.money.backend.mail.parser.services

import java.text.Normalizer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import net.matsudamper.money.backend.mail.parser.lib.ParseUtil

internal object EkiNetUsageServices : MoneyUsageServices {
    override val displayName: String = "えきネット"

    override fun parse(
        subject: String,
        from: String,
        html: String,
        plain: String,
        date: LocalDateTime,
    ): List<MoneyUsage> {
        val forwardedInfo = ParseUtil.parseForwarded(plain)
        val canHandle = sequence {
            yield(canHandled(from = from, subject = subject))
            yield(
                run {
                    if (forwardedInfo != null) {
                        val forwardedFrom = forwardedInfo.from ?: return@run false
                        val forwardedSubject = forwardedInfo.subject ?: return@run false
                        canHandled(from = forwardedFrom, subject = forwardedSubject)
                    } else {
                        false
                    }
                },
            )
        }
        if (canHandle.any { it }.not()) return listOf()
        val lines = ParseUtil.splitByNewLine(plain)

        val trainInfo = getTrainInfo(lines)
        val section = getSection(lines)
        val price = getPrice(lines)
        val dateTime = run dateTime@{
            val rideDateTime = getRideDateTime(plain)
            if (rideDateTime != null) return@dateTime rideDateTime

            val forwardedDate = forwardedInfo?.date
            if (forwardedDate != null) return@dateTime forwardedDate

            date
        }

        return listOf(
            MoneyUsage(
                title = section,
                price = price,
                description = trainInfo,
                service = MoneyUsageServiceType.EkiNet,
                dateTime = dateTime,
            ),
        )
    }

    private fun getPrice(lines: List<String>): Int {
        val index = lines.indexOf("■お支払い総額")
            .takeIf { it >= 0 }!!
            .plus(1)

        return ParseUtil.getInt(lines[index])!!
    }

    private fun getSection(lines: List<String>): String {
        val ticketInfo = run description@{
            val startIndex = lines.indexOf("==乗車券情報==")
                .takeIf { it >= 0 }!!
                .plus(1)

            val endIndex = lines.subList(startIndex, lines.size)
                .indexOf("")
                .takeIf { it >= 0 }!!
                .plus(startIndex)
            lines.subList(startIndex, endIndex)
        }
        return ticketInfo.associate {
            val (key, value) = it.split("：")
            key to value
        }["区　間"]!!
    }

    private fun getRideDateTime(plain: String): LocalDateTime? {
        // 全角の括弧・コロン・数字の表記ゆれを吸収する
        val normalized = Normalizer.normalize(plain, Normalizer.Form.NFKC)
        val rideDateMatch = rideDateRegex.find(normalized) ?: return null
        // 乗車券情報の区間には時刻が無いので、乗車日の直後にある最初の時刻を出発時刻とする
        val departureTimeMatch = departureTimeRegex.find(normalized, rideDateMatch.range.last + 1) ?: return null

        return runCatching {
            LocalDateTime.of(
                LocalDate.of(
                    rideDateMatch.groupValues[1].toInt(),
                    rideDateMatch.groupValues[2].toInt(),
                    rideDateMatch.groupValues[3].toInt(),
                ),
                LocalTime.of(
                    departureTimeMatch.groupValues[1].toInt(),
                    departureTimeMatch.groupValues[2].toInt(),
                ),
            )
        }.getOrNull()
    }

    private fun getTrainInfo(lines: List<String>): String {
        val startIndex = lines.indexOf("==列車情報==")
            .takeIf { it >= 0 }!!
            .plus(1)

        val endIndex = lines.subList(startIndex, lines.size)
            .indexOf("")
            .takeIf { it >= 0 }!!
            .plus(startIndex)
        return lines.subList(startIndex, endIndex)
            .joinToString("\n")
    }

    private fun canHandled(
        from: String,
        subject: String,
    ): Boolean {
        return from == "reservation@eki-net.com" && subject.contains("【申込完了】申込内容（JRきっぷ）のご案内")
    }

    private val rideDateRegex = """乗車日\s*:\s*(\d{4})\s*年\s*(\d{1,2})\s*月\s*(\d{1,2})\s*日""".toRegex()

    private val departureTimeRegex = """\(\s*(\d{1,2})\s*時\s*(\d{1,2})\s*分\s*\)""".toRegex()
}
