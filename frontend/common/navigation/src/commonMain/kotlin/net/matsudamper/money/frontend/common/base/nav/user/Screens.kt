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
    HomePeriodCategory {
        override val title: String = "カテゴリ"
        override val placeholderUrl: String = "/home/period/category/{id}"
    },
    HomePeriodSubCategory {
        override val title: String = "サブカテゴリ"
        override val placeholderUrl: String = "/home/period/category/{categoryId}/sub-category/{subCategoryId}"
    },
    HomeMonthlyCategory {
        override val title: String = "月間カテゴリ"
        override val placeholderUrl: String = "/home/monthly/{year-month}/category/{id}"
    },
    HomeMonthlySubCategory {
        override val title: String = "月間サブカテゴリ"
        override val placeholderUrl: String = "/home/monthly/{year-month}/sub-category/{id}"
    },
    HomeMonthly {
        override val title: String = "月別"
        override val placeholderUrl: String = "/home/monthly"
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
    Api {
        override val title: String = "API設定"
        override val placeholderUrl: String = "/settings/api"
    },
    SettingsLogin {
        override val title: String = "ログイン設定"
        override val placeholderUrl: String = "/settings/login"
    },
    SettingsTextFieldTest {
        override val title: String = "TextFieldテスト"
        override val placeholderUrl: String = "/settings/textfield-test"
    },

    UsageList {
        override val title: String = "リスト"
        override val placeholderUrl: String = "/usage/list"
    },

    UsageCalendar {
        override val title: String = "カレンダー"
        override val placeholderUrl: String = "/usage/calendar"
    },

    CalendarDateList {
        override val title: String = "日付別一覧"
        override val placeholderUrl: String = "/usage/calendar/date"
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
    ImportedMailList {
        override val title: String = "インポート済みメール"
        override val placeholderUrl: String = "/mail/imported"
    },
    AddRecurringUsage {
        override val title: String = "定期使用用途追加"
        override val placeholderUrl: String = "/add/recurring-usage"
    },
    Add {
        override val title: String = "登録"
        override val placeholderUrl: String = "/add"
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
    Splash {
        override val title: String = ""
        override val placeholderUrl: String = "/splash"
    },
}
