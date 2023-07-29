package net.matsudamper.money.frontend.common.base.nav.user

public sealed interface ScreenStructure {
    public val direction: Screens

    public sealed interface Root : ScreenStructure {
        public class Home : Root {
            override val direction: Screens = Screens.Home
        }

        public sealed interface Settings : Root {
            public object Root : Settings {
                override val direction: Screens = Screens.Settings
            }

            public object Imap : Settings {
                override val direction: Screens = Screens.SettingsImap
            }

            public object Category : Settings {
                override val direction: Screens = Screens.SettingsCategory
            }

            public object CategoryId : Settings {
                override val direction: Screens = Screens.SettingsCategoryId
            }

            public object SubCategory : Settings {
                override val direction: Screens = Screens.SettingsSubCategory
            }

            public class SubCategoryId(
                public val id: Int,
            ) : Settings {
                override val direction: Screens = Screens.SettingsSubCategoryId
            }
        }

        public class Register : Root {
            override val direction: Screens = Screens.Register
        }
    }

    public object NotFound : ScreenStructure {
        override val direction: Screens = Screens.NotFound
    }

    public object Login : ScreenStructure {
        override val direction: Screens = Screens.Login
    }

    public object Admin : ScreenStructure {
        override val direction: Screens = Screens.Admin
    }

    public object MailImport : ScreenStructure {
        override val direction: Screens = Screens.MailImport
    }

    public object MailLink : ScreenStructure {
        override val direction: Screens = Screens.MailLink
    }
}
