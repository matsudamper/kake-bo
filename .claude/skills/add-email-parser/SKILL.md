---
name: add-email-parser
description: Guide for adding a new email parser service to parse payment/purchase notification emails. Use when adding support for a new service's email format to extract transaction data (title, price, date, description).
---

# メールパーサー追加ガイド

決済メールをパースして家計簿データ（MoneyUsage）を抽出するパーサーを新規追加する手順。

---

## 手順

### 1. 既存パーサーの確認

実装前に必ず既存の全パーサーを確認し、類似のサービスが既に実装されていないか確認する。

- パーサー一覧: `backend/feature/service_mail_parser/src/jvmMain/kotlin/net/matsudamper/money/backend/mail/parser/services/`
- 登録一覧: `backend/feature/service_mail_parser/src/jvmMain/kotlin/net/matsudamper/money/backend/mail/parser/MailParser.kt`

### 2. MoneyUsageServiceType の追加（必要な場合）

新しいサービスの場合、enumに追加する。

**ファイル**: `backend/base/src/jvmMain/java/net/matsudamper/money/backend/base/element/MoneyUsageServiceType.kt`

```kotlin
NewService(
    id = <次のID>,
    displayName = "サービス名",
),
```

- `id`は既存の最大値+1を使用
- 既存のサービスに該当する場合は新規追加不要

### 3. パーサーの実装

**ディレクトリ**: `backend/feature/service_mail_parser/src/jvmMain/kotlin/net/matsudamper/money/backend/mail/parser/services/`

`MoneyUsageServices` interfaceを実装する `internal object` を作成する。

#### インターフェース

```kotlin
public interface MoneyUsageServices {
    public val displayName: String

    public fun parse(
        subject: String,
        from: String,
        html: String,
        plain: String,
        date: LocalDateTime,
    ): List<MoneyUsage>
}
```

#### データモデル

```kotlin
public data class MoneyUsage(
    val title: String,
    val description: String,
    val dateTime: LocalDateTime,
    val price: Int?,
    val service: MoneyUsageServiceType,
)
```

#### 実装テンプレート

```kotlin
package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDateTime
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import net.matsudamper.money.backend.mail.parser.lib.ParseUtil

internal object XxxUsageServices : MoneyUsageServices {
    override val displayName: String = "サービス名"

    override fun parse(
        subject: String,
        from: String,
        html: String,
        plain: String,
        date: LocalDateTime,
    ): List<MoneyUsage> {
        val canHandle = sequence {
            yield(canHandledWithFrom(from))
            yield(canHandledWithSubject(subject))
        }
        if (canHandle.any { it }.not()) return listOf()

        // plain または html からデータを抽出
        val lines = ParseUtil.splitByNewLine(plain)

        val parsedPrice: Int? = TODO("価格の抽出")
        val parsedDate: LocalDateTime? = TODO("日時の抽出")
        val title: String = TODO("タイトルの抽出")

        return listOf(
            MoneyUsage(
                title = title,
                price = parsedPrice,
                description = "",
                service = MoneyUsageServiceType.XxxService,
                dateTime = parsedDate ?: date,
            ),
        )
    }

    private fun canHandledWithFrom(from: String): Boolean {
        return from == "noreply@example.com"
    }

    private fun canHandledWithSubject(subject: String): Boolean {
        return subject.contains("購入")
    }
}
```

### 4. 転送メール対応（必要な場合）

Gmailからの転送メールを処理する場合は `ParseUtil.parseForwarded()` を使用する。

```kotlin
val forwardedOriginalInfo = ParseUtil.parseForwarded(plain)
// fromやsubjectの判定に転送元の情報を使う
val actualFrom = forwardedOriginalInfo?.from ?: from
val actualSubject = forwardedOriginalInfo?.subject ?: subject
val actualDate = forwardedOriginalInfo?.date ?: date
```

### 5. HTML解析（必要な場合）

HTMLメールの解析には jsoup を使用する。

```kotlin
import org.jsoup.Jsoup

val document = Jsoup.parse(html)
val elements = document.getElementsByTag("td")
```

### 6. MailParser への登録

**ファイル**: `backend/feature/service_mail_parser/src/jvmMain/kotlin/net/matsudamper/money/backend/mail/parser/MailParser.kt`

`sequenceOf(...)` の中に新しいパーサーを追加する。先にマッチしたパーサーが優先されるため、汎用的なパーサー（VpassUsageServices等）の前に配置する。

### 7. ビルドとテスト

CLAUDE.md の「ビルドとテスト」セクションに従う。

---

## ユーティリティ（ParseUtil）

**ファイル**: `backend/feature/service_mail_parser/src/jvmMain/kotlin/net/matsudamper/money/backend/mail/parser/lib/ParseUtil.kt`

| メソッド | 説明 |
|---|---|
| `getInt(value: String): Int?` | 文字列から数字のみを抽出してIntに変換 |
| `removeHtmlTag(value: String): String` | HTMLタグを除去 |
| `splitByNewLine(value: String): List<String>` | `\r\n` と `\n` で分割 |
| `parseForwarded(text: String): MailMetadata?` | 転送メールのメタデータをパース |

---

## 実装のポイント

- パーサーは `internal object`（シングルトン）として実装する
- `parse()` はメールを処理できない場合は空のリストを返す（例外を投げない）
- `canHandledWithFrom` / `canHandledWithSubject` で対象メールかを判定する
- `sequence { yield(...) }.any { it }` による遅延評価パターンで判定を行う
- 日付のパースに失敗した場合は引数の `date` をフォールバックとして使用する
- 価格は `Int?`（nullable）で、抽出できない場合は `null` を返す
- 日本語の日付フォーマットには `DateTimeFormatterBuilder` を使用する
