package net.matsudamper.money.frontend.common.base.nav.user

public enum class Screens : Direction {
    HomeRedirect {
        override val title: String = "ホーム"
        override val placeholderUrl: String = "/"
    },
    Home {
        override val title: String = "ホーム"
        override val placeholderUrl: String = "/home/period"
    },
    HomePeriodSubCategory {
        override val title: String = "サブカテゴリ"
        override val placeholderUrl: String = "/home/period/sub-category/{id}"
    },

    Settings {
        override val title: String = "設定"
        override val placeholderUrl: String = "/settings"
    },
    SettingsImap {
        override val title: String = "IMAP設定"
        override val placeholderUrl: String = "/settings/imap"
    },
    SettingsCategory {
        override val title: String = "カテゴリ設定"
        override val placeholderUrl: String = "/settings/category"
    },
    SettingsCategoryId {
        override val title: String = "カテゴリ設定"
        override val placeholderUrl: String = "/settings/category/{id}"
    },
    MailCategoryFilters {
        override val title: String = "メールカテゴリフィルタ一覧"
        override val placeholderUrl: String = "/settings/mail-category-filter"
    },
    MailCategoryFilter {
        override val title: String = "メールカテゴリフィルタ"
        override val placeholderUrl: String = "/settings/mail-category-filter/{id}"
    },

    UsageList {
        override val title: String = "リスト"
        override val placeholderUrl: String = "/usage/list"
    },

    UsageCalendar {
        override val title: String = "カレンダー"
        override val placeholderUrl: String = "/usage/calendar"
    },

    NotFound {
        override val title: String = "404"
        override val placeholderUrl: String = "/status/404"
    },
    Login {
        override val title: String = "ログイン"
        override val placeholderUrl: String = "/login"
    },
    Admin {
        override val title: String = "ログイン"
        override val placeholderUrl: String = "/admin"
    },
    MailImport {
        override val title: String = "メールインポート"
        override val placeholderUrl: String = "/mail/import"
    },
    MailList {
        override val title: String = "メール一覧"
        override val placeholderUrl: String = "/mail"
    },
    AddMoneyUsage {
        override val title: String = "使用用途追加"
        override val placeholderUrl: String = "/add/money-usage"
    },
    ImportedMail {
        override val title: String = "メール"
        override val placeholderUrl: String = "/mail/{id}"
    },
    ImportedMailHTML {
        override val title: String = "メール"
        override val placeholderUrl: String = "/mail/{id}/html"
    },
    ImportedMailPlain {
        override val title: String = "メール"
        override val placeholderUrl: String = "/mail/{id}/plain"
    },
    MoneyUsage {
        override val title: String = "使用用途"
        override val placeholderUrl: String = "/money-usage/{id}"
    },
    ;
}
