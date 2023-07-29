package net.matsudamper.money.frontend.common.base.nav.user
public enum class Screens : Direction {
    Home {
        override val title: String = "ホーム"
        override val url: String = "/"
    },

    Settings {
        override val title: String = "設定"
        override val url: String = "/settings"
    },
    SettingsImap {
        override val title: String = "IMAP設定"
        override val url: String = "/settings/map"
    },
    SettingsCategory {
        override val title: String = "カテゴリ設定"
        override val url: String = "/settings/category"
    },
    SettingsCategoryId {
        override val title: String = "カテゴリ設定"
        override val url: String = "/settings/category/{id}"
    },
    SettingsSubCategory {
        override val title: String = "サブカテゴリ設定"
        override val url: String = "/settings/sub-category"
    },
    SettingsSubCategoryId {
        override val title: String = "サブカテゴリ設定"
        override val url: String = "/settings/sub-category/{id}"

        override fun createUrl(param: Map<String, String>): String {
            return super.createUrl(param)
        }

        override fun parseArgument(path: String): Map<String, String> {
            return super.parseArgument(path)
        }
    },

    Register {
        override val title: String = "リスト"
        override val url: String = "/list"
    },

    NotFound {
        override val title: String = "404"
        override val url: String = "/status/404"
    },
    Login {
        override val title: String = "ログイン"
        override val url: String = "/login"
    },
    Admin {
        override val title: String = "ログイン"
        override val url: String = "/admin"
    },
    MailImport {
        override val title: String = "メールインポート"
        override val url: String = "/mail/import"
    },
    MailLink {
        override val title: String = "メールの登録"
        override val url: String = "/mail/link"
    },
    ;
}
