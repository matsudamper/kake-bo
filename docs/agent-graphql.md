# GraphQL

## 共通ルール

### 識別子の型
- 識別子に組み込みの `ID` を使わない。何の識別子かが分かる名前の scalar を定義して使う（例: `UserId`、`NoteImageId`、`FollowingGroupId`）
- `ID` のままだと、別の種類の識別子を取り違えて渡してもスキーマでもコードでも気付けない
