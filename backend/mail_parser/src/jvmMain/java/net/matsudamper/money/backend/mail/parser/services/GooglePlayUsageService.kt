package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices

public object GooglePlayUsageService : MoneyUsageServices {
    override val displayName: String = "Google"

    override fun parse(subject: String, from: String, html: String, plain: String, date: LocalDateTime): List<MoneyUsage> {
        val canHandle = sequence {
            yield(canHandledWithFrom(from))
            yield(canHandledWithSubject(subject))
            yield(canHandledWithPlain(plain))
        }
        if (canHandle.any { it }.not()) return listOf()

        val seller = sequence {
            yield(
                run target@{
                    "Google Play で(.+?)からの定期購入が完了しました。".toRegex().find(plain)
                        ?.groupValues
                        ?.getOrNull(1)
                        ?.trimStart()
                        ?.trimEnd()
                        ?: return@target null
                }
            )
            yield(
                run target@{
                    "Google Play での(.+?)からの購入が完了しました。".toRegex().find(plain)
                        ?.groupValues
                        ?.getOrNull(1)
                        ?.trimStart()
                        ?.trimEnd()
                        ?: return@target null
                }
            )
        }.filterNotNull().firstOrNull()

        val title = run title@{
            "アイテム 価格(.+?)￥".toRegex()
                .find(
                    plain.replace("\r\n", "")
                        .replace("\r", ""),
                )
                ?.groupValues?.getOrNull(1)
                ?.trimStart()
                ?.trimEnd()
        }

        val price = run price@{
            "^合計:(.+?)$".toRegex(RegexOption.MULTILINE)
                .find(plain)
                ?.groupValues?.getOrNull(1)
                ?.mapNotNull { it.toString().toIntOrNull() }
                ?.joinToString("")
                ?.toIntOrNull()
        }

        val parsedDate = run date@{
            val regex = "注文日(.+?)$".toRegex(RegexOption.MULTILINE)
            val line = regex.find(plain)?.groupValues?.getOrNull(1) ?: return@date null

            val result = """(\d+).+?(\d+).+?(\d+).+?(\d+).+?(\d+).+?(\d+)""".toRegex().find(line)
                ?: return@date null

            val year = result.groupValues.getOrNull(1)?.toIntOrNull() ?: return@date null
            val month = result.groupValues.getOrNull(2)?.toIntOrNull() ?: return@date null
            val day = result.groupValues.getOrNull(3)?.toIntOrNull() ?: return@date null
            val hour = result.groupValues.getOrNull(4)?.toIntOrNull() ?: return@date null
            val minute = result.groupValues.getOrNull(5)?.toIntOrNull() ?: return@date null
            val seconds = result.groupValues.getOrNull(6)?.toIntOrNull() ?: return@date null

            LocalDateTime.of(
                LocalDate.of(year, month, day),
                LocalTime.of(hour, minute, seconds)
            )
        }

        return listOf(
            MoneyUsage(
                title = title ?: displayName,
                price = price,
                description = "seller: $seller",
                service = MoneyUsageServiceType.GooglePlay,
                dateTime = parsedDate ?: date,
            )
        )
    }

    private fun canHandledWithPlain(plain: String): Boolean {
        return plain.contains("Google Play の注文履歴を見る。")
    }

    private fun canHandledWithSubject(subject: String): Boolean {
        return subject.startsWith("Google Play のご注文明細")
    }

    private fun canHandledWithFrom(from: String): Boolean {
        return from == "googleplay-noreply@google.com"
    }
}
