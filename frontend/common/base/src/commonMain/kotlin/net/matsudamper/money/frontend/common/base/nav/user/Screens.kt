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
    SettingsSubCategory {
        override val title: String = "サブカテゴリ設定"
        override val placeholderUrl: String = "/settings/sub-category"
    },
    SettingsSubCategoryId {
        override val title: String = "サブカテゴリ設定"
        override val placeholderUrl: String = "/settings/sub-category/{id}"
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
    MailLink {
        override val title: String = "メールの登録"
        override val placeholderUrl: String = "/mail/link"
    },
    AddMoneyUsage {
        override val title: String = "追加"
        override val placeholderUrl: String = "/add/money-usage"
    }
    ;
}
