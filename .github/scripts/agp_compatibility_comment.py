#!/usr/bin/env python3
import base64
import html
import json
import os
import re
import time
import urllib.error
import urllib.parse
import urllib.request
from html.parser import HTMLParser

AGP_FILE = "gradle/libs.versions.toml"
ANDROID_STUDIO_COMPATIBILITY_URL = "https://developer.android.com/build/releases/about-agp"
JETBRAINS_ANDROID_PLUGIN_ID = 22989
JETBRAINS_ANDROID_PLUGIN_URL = "https://plugins.jetbrains.com/plugin/22989-android/versions/stable"
COMMENT_MARKER = "<!-- agp-compatibility-report -->"


class TableParser(HTMLParser):
    def __init__(self):
        super().__init__()
        self.rows = []
        self.current_row = None
        self.current_cell = None
        self.cell_tag = None

    def handle_starttag(self, tag, attrs):
        if tag == "tr":
            self.current_row = []
        elif tag in ("td", "th") and self.current_row is not None:
            self.current_cell = []
            self.cell_tag = tag

    def handle_data(self, data):
        if self.current_cell is not None:
            self.current_cell.append(data)

    def handle_endtag(self, tag):
        if tag == self.cell_tag and self.current_cell is not None:
            value = " ".join("".join(self.current_cell).split())
            self.current_row.append(value)
            self.current_cell = None
            self.cell_tag = None
        elif tag == "tr" and self.current_row is not None:
            if self.current_row:
                self.rows.append(self.current_row)
            self.current_row = None


def request(url, token=None, method="GET", body=None, accept="application/json", include_headers=False):
    headers = {
        "Accept": accept,
        "User-Agent": "kake-bo-agp-compatibility-check",
    }
    if token:
        headers["Authorization"] = f"Bearer {token}"
        headers["X-GitHub-Api-Version"] = "2022-11-28"
    data = None if body is None else json.dumps(body).encode()
    last_error = None
    for attempt in range(3):
        try:
            req = urllib.request.Request(url, headers=headers, data=data, method=method)
            with urllib.request.urlopen(req, timeout=30) as response:
                payload = response.read().decode()
                if include_headers:
                    return payload, dict(response.headers.items())
                return payload
        except (urllib.error.URLError, TimeoutError) as error:
            last_error = error
            if attempt < 2:
                time.sleep(2 ** attempt)
    raise RuntimeError(f"取得に失敗しました: {url}: {last_error}")


def request_json(url, token=None, method="GET", body=None, include_headers=False):
    result = request(url, token=token, method=method, body=body, include_headers=include_headers)
    if include_headers:
        payload, headers = result
        return json.loads(payload), headers
    return json.loads(result)


def github_file(repo, ref, path, token):
    encoded_path = urllib.parse.quote(path)
    encoded_ref = urllib.parse.quote(ref, safe="")
    url = f"https://api.github.com/repos/{repo}/contents/{encoded_path}?ref={encoded_ref}"
    payload = request_json(url, token=token)
    return base64.b64decode(payload["content"]).decode()


def agp_version(toml_text):
    match = re.search(r'^agp\s*=\s*"([^"]+)"\s*$', toml_text, re.MULTILINE)
    if not match:
        raise RuntimeError(f"{AGP_FILE} から agp バージョンを取得できません")
    return match.group(1)


def major_minor(version):
    match = re.match(r"^(\d+)\.(\d+)", version)
    if not match:
        raise RuntimeError(f"AGP バージョンを解釈できません: {version}")
    return int(match.group(1)), int(match.group(2))


def find_android_studio_support(target_version):
    page = request(ANDROID_STUDIO_COMPATIBILITY_URL, accept="text/html")
    parser = TableParser()
    parser.feed(page)
    target = major_minor(target_version)
    for row in parser.rows:
        if len(row) < 3:
            continue
        range_match = re.search(r"(\d+\.\d+)\s*[-–—]\s*(\d+\.\d+)", row[2])
        if not range_match:
            continue
        minimum = major_minor(range_match.group(1))
        maximum = major_minor(range_match.group(2))
        if minimum <= target <= maximum:
            return {
                "name": row[0],
                "version": row[1],
                "range": row[2],
            }
    return None


def strip_html(value):
    if not value:
        return ""
    return " ".join(re.sub(r"<[^>]+>", " ", html.unescape(value)).split())


def is_stable(update):
    channel = (update.get("channel") or "stable").lower()
    return channel == "stable" and not update.get("hidden", False)


def update_timestamp(update):
    value = update.get("cdate")
    try:
        return int(value)
    except (TypeError, ValueError):
        return 0


def notes_support_agp(notes, target_version):
    major, minor = major_minor(target_version)
    text = strip_html(notes)
    version = rf"{major}\.{minor}(?:\.\d+)?"
    patterns = [
        rf"\bsupport(?:s|ed|ing)?\s+(?:for\s+)?(?:Android\s+Gradle\s+Plugin|AGP)\s+{version}\b",
        rf"\b(?:Android\s+Gradle\s+Plugin|AGP)\s+{version}\b[^.!?;]{{0,80}}\bsupport(?:s|ed)?\b",
        rf"\b(?:compatible|compatibility)\s+with\s+(?:Android\s+Gradle\s+Plugin|AGP)\s+{version}\b",
    ]
    return any(re.search(pattern, text, re.IGNORECASE) for pattern in patterns)


def intellij_compatibility(update):
    compatible = update.get("compatibleVersions") or {}
    idea_versions = []
    if isinstance(compatible, dict):
        for product, version in compatible.items():
            if "IDEA" in str(product).upper() and version:
                idea_versions.append(str(version))
    if idea_versions:
        return ", ".join(dict.fromkeys(idea_versions))
    return update.get("sinceUntil") or format_build_range(update)


def format_build_range(update):
    since = update.get("since")
    until = update.get("until")
    if since and until:
        return f"build {since} — {until}"
    if since:
        return f"build {since}+"
    if until:
        return f"build <= {until}"
    return "不明"


def find_jetbrains_android_plugin_support(target_version):
    latest = None
    page = 0
    while True:
        url = f"https://plugins.jetbrains.com/api/plugins/{JETBRAINS_ANDROID_PLUGIN_ID}/updates?size=100&page={page}"
        updates = request_json(url)
        if not updates:
            break
        stable = [item for item in updates if is_stable(item)]
        if stable:
            page_latest = max(stable, key=update_timestamp)
            if latest is None or update_timestamp(page_latest) > update_timestamp(latest):
                latest = page_latest
            matched = max(
                (item for item in stable if notes_support_agp(item.get("notes"), target_version)),
                key=update_timestamp,
                default=None,
            )
            if matched:
                return matched, latest
        if len(updates) < 100:
            break
        page += 1
    return None, latest


def jetbrains_row(target_version, matched, latest):
    if matched:
        version = matched.get("version") or "不明"
        compatibility = intellij_compatibility(matched)
        update_id = matched.get("id")
        detail_url = (
            f"https://plugins.jetbrains.com/plugin/{JETBRAINS_ANDROID_PLUGIN_ID}-android/versions/stable/{update_id}"
            if update_id
            else JETBRAINS_ANDROID_PLUGIN_URL
        )
        return (
            f"✅ Android Plugin {version} で AGP {'.'.join(map(str, major_minor(target_version)))} の対応を明記。"
            f" IntelliJ IDEA互換: {compatibility}。 [リリース]({detail_url})"
        )
    if latest:
        version = latest.get("version") or "不明"
        compatibility = intellij_compatibility(latest)
        return (
            f"⚠️ 最新Stableの Android Plugin {version} まで確認しましたが、"
            f"AGP {'.'.join(map(str, major_minor(target_version)))} 対応の明記を確認できません。"
            f" IntelliJ IDEA互換: {compatibility}。 [Stable一覧]({JETBRAINS_ANDROID_PLUGIN_URL})"
        )
    return f"⚠️ JetBrains Marketplace から Android Plugin のStable版を取得できませんでした。"


def build_comment(old_version, new_version, studio, jetbrains_status, errors):
    if studio:
        studio_status = (
            f"✅ {studio['name']} ({studio['version']}) が AGP {studio['range']} をサポート。 "
            f"[公式互換表]({ANDROID_STUDIO_COMPATIBILITY_URL})"
        )
    else:
        studio_status = (
            f"⚠️ AGP {'.'.join(map(str, major_minor(new_version)))} を含む Android Studio の互換範囲を"
            f"公式表から確認できませんでした。 [公式互換表]({ANDROID_STUDIO_COMPATIBILITY_URL})"
        )
    lines = [
        COMMENT_MARKER,
        "## AGP互換性確認",
        "",
        f"AGP `{old_version}` → `{new_version}` の更新について公式情報を確認しました。",
        "",
        "| 項目 | 状況 |",
        "| --- | --- |",
        f"| Android Studio | {studio_status} |",
        f"| IntelliJ IDEA / Android Plugin | {jetbrains_status} |",
    ]
    if errors:
        lines.extend(["", "### 取得時の警告"])
        lines.extend(f"- {error}" for error in errors)
    lines.extend(["", "_このコメントはAGP更新時に自動更新されます。_"])
    return "\n".join(lines)


def next_link(link_header):
    for part in (link_header or "").split(","):
        match = re.match(r'\s*<([^>]+)>;\s*rel="([^"]+)"', part)
        if match and match.group(2) == "next":
            return match.group(1)
    return None


def upsert_comment(repo, pr_number, token, body):
    comments_url = f"https://api.github.com/repos/{repo}/issues/{pr_number}/comments?per_page=100"
    existing = None
    page_url = comments_url
    while page_url:
        comments, headers = request_json(page_url, token=token, include_headers=True)
        existing = next((comment for comment in comments if COMMENT_MARKER in (comment.get("body") or "")), None)
        if existing:
            break
        page_url = next_link(headers.get("Link") or headers.get("link"))
    if existing:
        request_json(
            f"https://api.github.com/repos/{repo}/issues/comments/{existing['id']}",
            token=token,
            method="PATCH",
            body={"body": body},
        )
    else:
        request_json(comments_url.split("?", 1)[0], token=token, method="POST", body={"body": body})


def main():
    token = os.environ["GITHUB_TOKEN"]
    repo = os.environ["GITHUB_REPOSITORY"]
    with open(os.environ["GITHUB_EVENT_PATH"], encoding="utf-8") as event_file:
        event = json.load(event_file)
    pull_request = event["pull_request"]
    pr_number = pull_request["number"]
    base_sha = pull_request["base"]["sha"]
    head_sha = pull_request["head"]["sha"]

    old_version = agp_version(github_file(repo, base_sha, AGP_FILE, token))
    new_version = agp_version(github_file(repo, head_sha, AGP_FILE, token))
    if old_version == new_version:
        print("AGP バージョンに変更がないため終了します")
        return

    errors = []
    studio = None
    matched = None
    latest = None
    try:
        studio = find_android_studio_support(new_version)
    except Exception as error:
        errors.append(f"Android Studio互換表: {error}")
    try:
        matched, latest = find_jetbrains_android_plugin_support(new_version)
    except Exception as error:
        errors.append(f"JetBrains Marketplace: {error}")

    jetbrains_status = jetbrains_row(new_version, matched, latest)
    body = build_comment(old_version, new_version, studio, jetbrains_status, errors)
    upsert_comment(repo, pr_number, token, body)
    print(f"PR #{pr_number} の互換性コメントを更新しました")


if __name__ == "__main__":
    main()
