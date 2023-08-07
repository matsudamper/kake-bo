package net.matsudamper.money.backend.base.element

import net.matsudamper.money.element.MoneyUsageServiceId

public enum class MoneyUsageServiceType(
    private val dbValue: Int,
    public val displayName: String,
) {
    Amazon(
        dbValue = 1,
        displayName = "Amazon",
    ),
    RakutenPay(
        dbValue = 2,
        displayName = "Rakuten Pay",
    ),
    Macdonalds(
        dbValue = 3,
        displayName = "Macdonalds",
    ),
    Steam(
        dbValue = 4,
        displayName = "Steam",
    ),
    MovieTicket(
        dbValue = 5,
        displayName = "ムビチケ",
    ),
    GooglePlay(
        dbValue = 7,
        displayName = "Google Play",
    ),
    YouTube(
        dbValue = 8,
        displayName = "YouTube",
    ),
    ESekiReserve(
        dbValue = 9,
        displayName = "e席リザーブ",
    ),
    UberEats(
        dbValue = 10,
        displayName = "UberEats",
    ),
    PayPal(
        dbValue = 11,
        displayName = "PayPal",
    ),
    Yodobashi(
        dbValue = 12,
        displayName = "ヨドバシカメラ",
    ),
    ;

    public fun toId(): MoneyUsageServiceId = MoneyUsageServiceId(dbValue)
}
