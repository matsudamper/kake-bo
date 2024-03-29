/*
 * This file is generated by jOOQ.
 */
package net.matsudamper.money.db.schema.keys


import net.matsudamper.money.db.schema.tables.JAdminSessions
import net.matsudamper.money.db.schema.tables.JApiTokens
import net.matsudamper.money.db.schema.tables.JCategoryMailFilterConditionOperatorType
import net.matsudamper.money.db.schema.tables.JCategoryMailFilterConditionType
import net.matsudamper.money.db.schema.tables.JCategoryMailFilterConditions
import net.matsudamper.money.db.schema.tables.JCategoryMailFilterDatasourceType
import net.matsudamper.money.db.schema.tables.JCategoryMailFilters
import net.matsudamper.money.db.schema.tables.JMoneyUsageCategories
import net.matsudamper.money.db.schema.tables.JMoneyUsageSubCategories
import net.matsudamper.money.db.schema.tables.JMoneyUsages
import net.matsudamper.money.db.schema.tables.JMoneyUsagesMailsRelation
import net.matsudamper.money.db.schema.tables.JUserImapSettings
import net.matsudamper.money.db.schema.tables.JUserMails
import net.matsudamper.money.db.schema.tables.JUserPasswordExtendData
import net.matsudamper.money.db.schema.tables.JUserPasswords
import net.matsudamper.money.db.schema.tables.JUserSessions
import net.matsudamper.money.db.schema.tables.JUsers
import net.matsudamper.money.db.schema.tables.JWebAuthAuthenticator
import net.matsudamper.money.db.schema.tables.records.JAdminSessionsRecord
import net.matsudamper.money.db.schema.tables.records.JApiTokensRecord
import net.matsudamper.money.db.schema.tables.records.JCategoryMailFilterConditionOperatorTypeRecord
import net.matsudamper.money.db.schema.tables.records.JCategoryMailFilterConditionTypeRecord
import net.matsudamper.money.db.schema.tables.records.JCategoryMailFilterConditionsRecord
import net.matsudamper.money.db.schema.tables.records.JCategoryMailFilterDatasourceTypeRecord
import net.matsudamper.money.db.schema.tables.records.JCategoryMailFiltersRecord
import net.matsudamper.money.db.schema.tables.records.JMoneyUsageCategoriesRecord
import net.matsudamper.money.db.schema.tables.records.JMoneyUsageSubCategoriesRecord
import net.matsudamper.money.db.schema.tables.records.JMoneyUsagesMailsRelationRecord
import net.matsudamper.money.db.schema.tables.records.JMoneyUsagesRecord
import net.matsudamper.money.db.schema.tables.records.JUserImapSettingsRecord
import net.matsudamper.money.db.schema.tables.records.JUserMailsRecord
import net.matsudamper.money.db.schema.tables.records.JUserPasswordExtendDataRecord
import net.matsudamper.money.db.schema.tables.records.JUserPasswordsRecord
import net.matsudamper.money.db.schema.tables.records.JUserSessionsRecord
import net.matsudamper.money.db.schema.tables.records.JUsersRecord
import net.matsudamper.money.db.schema.tables.records.JWebAuthAuthenticatorRecord

import org.jooq.UniqueKey
import org.jooq.impl.DSL
import org.jooq.impl.Internal



// -------------------------------------------------------------------------
// UNIQUE and PRIMARY KEY definitions
// -------------------------------------------------------------------------

val KEY_ADMIN_SESSIONS_PRIMARY: UniqueKey<JAdminSessionsRecord> = Internal.createUniqueKey(JAdminSessions.ADMIN_SESSIONS, DSL.name("KEY_admin_sessions_PRIMARY"), arrayOf(JAdminSessions.ADMIN_SESSIONS.SESSION_ID), true)
val KEY_API_TOKENS_PRIMARY: UniqueKey<JApiTokensRecord> = Internal.createUniqueKey(JApiTokens.API_TOKENS, DSL.name("KEY_api_tokens_PRIMARY"), arrayOf(JApiTokens.API_TOKENS.API_TOKEN_ID), true)
val KEY_API_TOKENS_TOKEN: UniqueKey<JApiTokensRecord> = Internal.createUniqueKey(JApiTokens.API_TOKENS, DSL.name("KEY_api_tokens_token"), arrayOf(JApiTokens.API_TOKENS.TOKEN_HASH), true)
val KEY_API_TOKENS_UNIQUE_NAME: UniqueKey<JApiTokensRecord> = Internal.createUniqueKey(JApiTokens.API_TOKENS, DSL.name("KEY_api_tokens_unique_name"), arrayOf(JApiTokens.API_TOKENS.USER_ID, JApiTokens.API_TOKENS.DISPLAY_NAME), true)
val KEY_CATEGORY_MAIL_FILTER_CONDITION_OPERATOR_TYPE_PRIMARY: UniqueKey<JCategoryMailFilterConditionOperatorTypeRecord> = Internal.createUniqueKey(JCategoryMailFilterConditionOperatorType.CATEGORY_MAIL_FILTER_CONDITION_OPERATOR_TYPE, DSL.name("KEY_category_mail_filter_condition_operator_type_PRIMARY"), arrayOf(JCategoryMailFilterConditionOperatorType.CATEGORY_MAIL_FILTER_CONDITION_OPERATOR_TYPE.CATEGORY_MAIL_FILTER_CONDITION_OPERATOR_TYPE_ID), true)
val KEY_CATEGORY_MAIL_FILTER_CONDITION_TYPE_PRIMARY: UniqueKey<JCategoryMailFilterConditionTypeRecord> = Internal.createUniqueKey(JCategoryMailFilterConditionType.CATEGORY_MAIL_FILTER_CONDITION_TYPE, DSL.name("KEY_category_mail_filter_condition_type_PRIMARY"), arrayOf(JCategoryMailFilterConditionType.CATEGORY_MAIL_FILTER_CONDITION_TYPE.CATEGORY_MAIL_FILTER_CONDITION_TYPE_ID), true)
val KEY_CATEGORY_MAIL_FILTER_CONDITIONS_PRIMARY: UniqueKey<JCategoryMailFilterConditionsRecord> = Internal.createUniqueKey(JCategoryMailFilterConditions.CATEGORY_MAIL_FILTER_CONDITIONS, DSL.name("KEY_category_mail_filter_conditions_PRIMARY"), arrayOf(JCategoryMailFilterConditions.CATEGORY_MAIL_FILTER_CONDITIONS.CATEGORY_MAIL_FILTER_CONDITION_ID), true)
val KEY_CATEGORY_MAIL_FILTER_DATASOURCE_TYPE_PRIMARY: UniqueKey<JCategoryMailFilterDatasourceTypeRecord> = Internal.createUniqueKey(JCategoryMailFilterDatasourceType.CATEGORY_MAIL_FILTER_DATASOURCE_TYPE, DSL.name("KEY_category_mail_filter_datasource_type_PRIMARY"), arrayOf(JCategoryMailFilterDatasourceType.CATEGORY_MAIL_FILTER_DATASOURCE_TYPE.CATEGORY_MAIL_FILTER_DATASOURCE_TYPE_ID), true)
val KEY_CATEGORY_MAIL_FILTERS_PRIMARY: UniqueKey<JCategoryMailFiltersRecord> = Internal.createUniqueKey(JCategoryMailFilters.CATEGORY_MAIL_FILTERS, DSL.name("KEY_category_mail_filters_PRIMARY"), arrayOf(JCategoryMailFilters.CATEGORY_MAIL_FILTERS.CATEGORY_MAIL_FILTER_ID), true)
val KEY_MONEY_USAGE_CATEGORIES_PRIMARY: UniqueKey<JMoneyUsageCategoriesRecord> = Internal.createUniqueKey(JMoneyUsageCategories.MONEY_USAGE_CATEGORIES, DSL.name("KEY_money_usage_categories_PRIMARY"), arrayOf(JMoneyUsageCategories.MONEY_USAGE_CATEGORIES.MONEY_USAGE_CATEGORY_ID), true)
val KEY_MONEY_USAGE_SUB_CATEGORIES_PRIMARY: UniqueKey<JMoneyUsageSubCategoriesRecord> = Internal.createUniqueKey(JMoneyUsageSubCategories.MONEY_USAGE_SUB_CATEGORIES, DSL.name("KEY_money_usage_sub_categories_PRIMARY"), arrayOf(JMoneyUsageSubCategories.MONEY_USAGE_SUB_CATEGORIES.MONEY_USAGE_SUB_CATEGORY_ID), true)
val KEY_MONEY_USAGES_PRIMARY: UniqueKey<JMoneyUsagesRecord> = Internal.createUniqueKey(JMoneyUsages.MONEY_USAGES, DSL.name("KEY_money_usages_PRIMARY"), arrayOf(JMoneyUsages.MONEY_USAGES.MONEY_USAGE_ID), true)
val KEY_MONEY_USAGES_MAILS_RELATION_PRIMARY: UniqueKey<JMoneyUsagesMailsRelationRecord> = Internal.createUniqueKey(JMoneyUsagesMailsRelation.MONEY_USAGES_MAILS_RELATION, DSL.name("KEY_money_usages_mails_relation_PRIMARY"), arrayOf(JMoneyUsagesMailsRelation.MONEY_USAGES_MAILS_RELATION.MONEY_USAGE_ID, JMoneyUsagesMailsRelation.MONEY_USAGES_MAILS_RELATION.USER_MAIL_ID), true)
val KEY_USER_IMAP_SETTINGS_PRIMARY: UniqueKey<JUserImapSettingsRecord> = Internal.createUniqueKey(JUserImapSettings.USER_IMAP_SETTINGS, DSL.name("KEY_user_imap_settings_PRIMARY"), arrayOf(JUserImapSettings.USER_IMAP_SETTINGS.USER_ID), true)
val KEY_USER_MAILS_PRIMARY: UniqueKey<JUserMailsRecord> = Internal.createUniqueKey(JUserMails.USER_MAILS, DSL.name("KEY_user_mails_PRIMARY"), arrayOf(JUserMails.USER_MAILS.USER_MAIL_ID), true)
val KEY_USER_PASSWORD_EXTEND_DATA_PRIMARY: UniqueKey<JUserPasswordExtendDataRecord> = Internal.createUniqueKey(JUserPasswordExtendData.USER_PASSWORD_EXTEND_DATA, DSL.name("KEY_user_password_extend_data_PRIMARY"), arrayOf(JUserPasswordExtendData.USER_PASSWORD_EXTEND_DATA.USER_ID), true)
val KEY_USER_PASSWORDS_PRIMARY: UniqueKey<JUserPasswordsRecord> = Internal.createUniqueKey(JUserPasswords.USER_PASSWORDS, DSL.name("KEY_user_passwords_PRIMARY"), arrayOf(JUserPasswords.USER_PASSWORDS.USER_ID), true)
val KEY_USER_SESSIONS_PRIMARY: UniqueKey<JUserSessionsRecord> = Internal.createUniqueKey(JUserSessions.USER_SESSIONS, DSL.name("KEY_user_sessions_PRIMARY"), arrayOf(JUserSessions.USER_SESSIONS.SESSION_ID), true)
val KEY_USER_SESSIONS_USER_ID_AND_NAME: UniqueKey<JUserSessionsRecord> = Internal.createUniqueKey(JUserSessions.USER_SESSIONS, DSL.name("KEY_user_sessions_user_id_and_name"), arrayOf(JUserSessions.USER_SESSIONS.USER_ID, JUserSessions.USER_SESSIONS.NAME), true)
val KEY_USERS_PRIMARY: UniqueKey<JUsersRecord> = Internal.createUniqueKey(JUsers.USERS, DSL.name("KEY_users_PRIMARY"), arrayOf(JUsers.USERS.USER_ID), true)
val KEY_USERS_USER_NAME_UNIQUE: UniqueKey<JUsersRecord> = Internal.createUniqueKey(JUsers.USERS, DSL.name("KEY_users_user_name_unique"), arrayOf(JUsers.USERS.USER_NAME), true)
val KEY_WEB_AUTH_AUTHENTICATOR_PRIMARY: UniqueKey<JWebAuthAuthenticatorRecord> = Internal.createUniqueKey(JWebAuthAuthenticator.WEB_AUTH_AUTHENTICATOR, DSL.name("KEY_web_auth_authenticator_PRIMARY"), arrayOf(JWebAuthAuthenticator.WEB_AUTH_AUTHENTICATOR.ID), true)
val KEY_WEB_AUTH_AUTHENTICATOR_USER_ID_AND_NAME: UniqueKey<JWebAuthAuthenticatorRecord> = Internal.createUniqueKey(JWebAuthAuthenticator.WEB_AUTH_AUTHENTICATOR, DSL.name("KEY_web_auth_authenticator_user_id_and_name"), arrayOf(JWebAuthAuthenticator.WEB_AUTH_AUTHENTICATOR.USER_ID, JWebAuthAuthenticator.WEB_AUTH_AUTHENTICATOR.NAME), true)
