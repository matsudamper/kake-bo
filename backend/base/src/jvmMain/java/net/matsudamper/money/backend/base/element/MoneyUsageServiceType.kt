package net.matsudamper.money.backend.base.element

public enum class MoneyUsageServiceType(
    private val id: Int,
    public val displayName: String,
) {
    Amazon(
        id = 1,
        displayName = "Amazon",
    ),
    RakutenPay(
        id = 2,
        displayName = "Rakuten Pay",
    ),
    Macdonalds(
        id = 3,
        displayName = "Macdonalds",
    ),
    Steam(
        id = 4,
        displayName = "Steam",
    ),
    MovieTicket(
        id = 5,
        displayName = "ムビチケ",
    ),
    GooglePlay(
        id = 7,
        displayName = "Google Play",
    ),
    YouTube(
        id = 8,
        displayName = "YouTube",
    ),
    ESekiReserve(
        id = 9,
        displayName = "e席リザーブ",
    ),
    UberEats(
        id = 10,
        displayName = "UberEats",
    ),
    PayPal(
        id = 11,
        displayName = "PayPal",
    ),
    Yodobashi(
        id = 12,
        displayName = "ヨドバシカメラ",
    ),
    FanzaDojin(
        id = 13,
        displayName = "Fanza同人",
    ),
    Shunsugu(
        id = 14,
        displayName = "旬すぐ",
    ),
    PostCoffee(
        id = 15,
        displayName = "PostCoffee",
    ),
    BicCamera(
        id = 16,
        displayName = "ビックカメラ",
    ),
    BookWalker(
        id = 17,
        displayName = "Book Walker",
    ),
    Rakuten(
        id = 18,
        displayName = "楽天",
    ),
    EkiNet(
        id = 19,
        displayName = "えきねっと",
    ),
    JapanTsushin(
        id = 20,
        displayName = "日本通信",
    ),
    AuEasyPayment(
        id = 21,
        displayName = "auかんたん決済",
    ),
    Nintendo(
        id = 22,
        displayName = "任天堂",
    ),
    NttEastAtBilling(
        id = 23,
        displayName = "@ビリング",
    ),
    Mountbell(
        id = 24,
        displayName = "モンベル",
    ),
    CreditCard(
        id = 25,
        displayName = "クレジットカード",
    ),
    YouTubeMembership(
        id = 26,
        displayName = "YouTube Membership",
    ),
    Dmm(
        id = 27,
        displayName = "DMM",
    ),
    YahooShopping(
        id = 28,
        displayName = "Yahoo!ショッピング",
    ),
    AuPay(
        id = 29,
        displayName = "au PAY",
    ),
    Booth(
        id = 30,
        displayName = "Booth",
    ),
    Povo(
        id = 31,
        displayName = "povo",
    ),
    NijisanjiOfficialStore(
        id = 32,
        displayName = "にじさんじオフィシャルストア",
    ),
}
