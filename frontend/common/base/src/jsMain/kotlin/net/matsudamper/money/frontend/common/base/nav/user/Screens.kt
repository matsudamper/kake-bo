package net.matsudamper.money.frontend.common.base.nav.user

public enum class Screens : Direction {
    Home {
        override val title: String = "ホーム"
        override val placeholderUrl: String = "/"
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

    List {
        override val title: String = "リスト"
        override val placeholderUrl: String = "/list"
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
        override val title: String = "追加"
        override val placeholderUrl: String = "/add/money-usage"
    },
    ImportedMail {
        override val title: String = "メール"
        override val placeholderUrl: String = "/mail/{id}"
    },
    ImportedMailContent {
        override val title: String = "メール"
        override val placeholderUrl: String = "/mail/{id}/content"
    },
    ;
}
