package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices

internal object ESekiReserveUsegeService : MoneyUsageServices {
    override val displayName: String = "e席リザーブ"

    override fun parse(
        subject: String,
        from: String,
        html: String,
        plain: String,
        date: LocalDateTime,
    ): List<MoneyUsage> {
        val canHandle = sequence {
            yield(canHandledWithFrom(from))
            yield(canHandledWithSubject(subject))
            yield(canHandledWithPlain(plain))
        }
        if (canHandle.any { it }.not()) return listOf()

        val lines = plain
            .split("\r\n")
            .flatMap { it.split("\n") }

        val title = run title@{
            val index = lines.indexOfFirst { it.contains("作品名") }.takeIf { it >= 0 }
                ?: return@title null

            lines.getOrNull(index + 1)
                ?.trimStart()
                ?.trimEnd()
                ?: return@title null
        }

        val price = run price@{
            "合計金額(.+?)$".toRegex(RegexOption.MULTILINE)
                .find(plain)
                ?.groupValues?.getOrNull(1)
                ?.mapNotNull { it.toString().toIntOrNull() }
                ?.joinToString("")
                ?.toIntOrNull()
        }

        val parsedDate = run date@{
            val index = lines.indexOfFirst { it.contains("上映開始時間") }.takeIf { it >= 0 }
                ?: return@date null

            val result = """(\d+)年(\d+)月(\d+)日.*?(\d+):(\d+)""".toRegex()
                .find(
                    lines.getOrNull(index + 1)
                        ?.trimStart()
                        ?.trimEnd()
                        ?: return@date null,
                )
                ?: return@date null

            val year = result.groupValues.getOrNull(1)?.toIntOrNull() ?: return@date null
            val month = result.groupValues.getOrNull(2)?.toIntOrNull() ?: return@date null
            val day = result.groupValues.getOrNull(3)?.toIntOrNull() ?: return@date null
            val hour = result.groupValues.getOrNull(4)?.toIntOrNull() ?: return@date null
            val minute = result.groupValues.getOrNull(5)?.toIntOrNull() ?: return@date null

            LocalDateTime.of(
                LocalDate.of(year, month, day),
                LocalTime.of(hour, minute),
            )
        }

        return listOf(
            MoneyUsage(
                title = title ?: displayName,
                price = price,
                description = "",
                service = MoneyUsageServiceType.ESekiReserve,
                dateTime = parsedDate ?: date,
            ),
        )
    }

    private fun canHandledWithFrom(from: String): Boolean {
        return from == "e-reserve@aeonent.jp"
    }

    private fun canHandledWithSubject(subject: String): Boolean {
        return subject.startsWith("e席リザーブ")
    }

    private fun canHandledWithPlain(plain: String): Boolean {
        return plain.contains("e席リザーブ")
    }
}
