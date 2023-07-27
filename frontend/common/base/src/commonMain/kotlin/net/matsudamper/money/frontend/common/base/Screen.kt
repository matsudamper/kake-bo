package net.matsudamper.money.frontend.common.base

public sealed interface Screen : Direction {
    public sealed interface Root : Screen {
        public object Home : Root {
            override val title: String = "ホーム"
            override val url: String = "/"
        }

        public object Settings : Root {
            override val title: String = "設定"
            override val url: String = "/settings"
        }

        public object Register : Root {
            override val title: String = "リスト"
            override val url: String = "/list"
        }
    }

    public sealed interface Settings : Screen {
        public object Imap : Settings {
            override val title: String = "IMAP設定"
            override val url: String = "/settings/map"
        }

        public object Category : Settings {
            override val title: String = "カテゴリ設定"
            override val url: String = "/settings/category"
        }

        public object CategoryId : Settings {
            override val title: String = "カテゴリ設定"
            override val url: String = "/settings/category/{id}"
        }

        public object SubCategory : Settings {
            override val title: String = "サブカテゴリ設定"
            override val url: String = "/settings/sub-category"
        }

        public object SubCategoryId : Settings {
            override val title: String = "サブカテゴリ設定"
            override val url: String = "/settings/sub-category/{id}"

            public fun createUrl() {

            }

            override fun parseArgument(path: String): Map<String, String> {
                return super.parseArgument(path)
            }

            public data class Argument(
                val id: Int,
            )
        }
    }

    public object Login : Screen {
        override val title: String = "ログイン"
        override val url: String = "/login"
    }

    public object Admin : Screen {
        override val title: String = "ログイン"
        override val url: String = "/admin"
    }

    public object MailImport : Screen {
        override val title: String = "メールインポート"
        override val url: String = "/mail/import"
    }

    public object MailLink : Screen {
        override val title: String = "メールの登録"
        override val url: String = "/mail/link"
    }

    public companion object {
        public val subClass: List<Screen> = listOf(
            Root.Home,
            Root.Settings,
            Root.Register,
            Login,
            Admin,
            MailImport,
            MailLink,
            Settings.Category,
            Settings.CategoryId,
            Settings.SubCategory,
            Settings.SubCategoryId,
            Settings.Imap,
        )
    }
}
