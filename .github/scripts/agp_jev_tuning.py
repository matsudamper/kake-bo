#!/usr/bin/env python3
import html
import json
import re
import time
import urllib.request
from pathlib import Path

import numpy as np
import onnxruntime as ort
from huggingface_hub import hf_hub_download
from transformers import AutoTokenizer

MODEL_REPO = "heman10x/rlcd-modernbert-151m"
MODEL_FILE = "model_fp16.onnx"
CALIBRATOR_FILE = "calibrator.json"
JETBRAINS_URL = "https://plugins.jetbrains.com/api/plugins/22989/updates?size=100&page={page}"
JETBRAINS_META_URL = "https://plugins.jetbrains.com/files/22989/{update_id}/meta.json"
REPORT_PATH = Path("agp-jev-tuning-report.json")
LABEL = "<<LABEL>>"
SEP = "<<SEP>>"
ABSTAIN = "__insufficient_evidence__"
THRESHOLDS = [0.35, 0.5, 0.65, 0.8, 0.9, 0.95]
MARGINS = [0.0, 0.1, 0.2, 0.3, 0.4]
SUPPORT_PATTERNS = [
    re.compile(r"(?i)\bsupport(?:s|ed|ing)?\s+(?:for\s+)?(?:Android\s+Gradle\s+Plugin|AGP)(?:\s+version)?\s+(\d+\.\d+(?:\.\d+)?)"),
    re.compile(r"(?i)\b(?:compatible|compatibility)\s+with\s+(?:Android\s+Gradle\s+Plugin|AGP)(?:\s+version)?\s+(\d+\.\d+(?:\.\d+)?)"),
    re.compile(r"(?i)\\b(?:Android\\s+Gradle\\s+Plugin|AGP)(?:\\s+version)?\\s+(\\d+\\.\\d+(?:\\.\\d+)?)\\b[^.!?;]{0,80}\\bsupport(?:s|ed)?\\b"),
]

def fetch_json(url):
    req = urllib.request.Request(url, headers={"Accept": "application/json", "User-Agent": "kake-bo-agp-jev-tuning"})
    with urllib.request.urlopen(req, timeout=30) as response:
        return json.load(response)

def strip_html(value):
    return " ".join(re.sub(r"<[^>]+>", " ", html.unescape(value or "")).split())

def major_minor(version):
    match = re.match(r"^(\d+)\.(\d+)", version)
    if not match:
        raise ValueError(version)
    return f"{int(match.group(1))}.{int(match.group(2))}"

def nearby(version, delta=2):
    major, minor = map(int, major_minor(version).split("."))
    return f"{major}.{minor + delta}"

def supported_versions(note):
    versions = []
    for pattern in SUPPORT_PATTERNS:
        versions.extend(major_minor(match.group(1)) for match in pattern.finditer(note))
    return list(dict.fromkeys(versions))

def build_cases():
    updates = []
    for page in range(20):
        page_updates = fetch_json(JETBRAINS_URL.format(page=page))
        if not page_updates:
            break
        updates.extend(page_updates)
        if len(page_updates) < 100:
            break

    cases = []
    seen = set()
    for update in updates:
        if (update.get("channel") or "stable").lower() != "stable" or update.get("hidden", False):
            continue
        try:
            metadata = fetch_json(JETBRAINS_META_URL.format(update_id=update.get("id")))
        except Exception:
            continue
        note = strip_html(metadata.get("notes"))
        versions = supported_versions(note)
        if not note or note in seen or not versions:
            continue
        seen.add(note)
        target = versions[0]
        control = nearby(target)
        while control in versions:
            control = nearby(control)
        cases.append({"id": f"real-positive-{update.get('id')}", "kind": "real-positive", "target": target, "control": control, "note": note, "expected": True})
        cases.append({"id": f"real-negative-{update.get('id')}", "kind": "real-near-negative", "target": control, "control": target, "note": note, "expected": False})
        if len(seen) >= 12:
            break

    for update in updates:
        if (update.get("channel") or "stable").lower() != "stable" or update.get("hidden", False):
            continue
        try:
            metadata = fetch_json(JETBRAINS_META_URL.format(update_id=update.get("id")))
        except Exception:
            continue
        note = strip_html(metadata.get("notes"))
        if not note or note in seen or supported_versions(note):
            continue
        if not re.search(r"(?i)\b(?:AGP|Android\s+Gradle\s+Plugin)\b", note):
            continue
        cases.append({"id": f"real-unclear-{update.get('id')}", "kind": "real-unclear", "target": "9.4", "control": "9.3", "note": note, "expected": False})
        if sum(case["kind"] == "real-unclear" for case in cases) >= 6:
            break

    cases.extend([
        {"id": "synthetic-positive-support", "kind": "synthetic", "target": "9.4", "control": "9.3", "note": "Added support for Android Gradle Plugin 9.4.", "expected": True},
        {"id": "synthetic-positive-compatible", "kind": "synthetic", "target": "9.4", "control": "9.3", "note": "This Android Plugin release is compatible with AGP 9.4.1 and includes sync fixes.", "expected": True},
        {"id": "synthetic-positive-long", "kind": "synthetic", "target": "9.4", "control": "9.5", "note": "Fixed editor rendering. Android Gradle Plugin 9.4 support is now available in this release. Improved device discovery.", "expected": True},
        {"id": "synthetic-negative-explicit", "kind": "synthetic", "target": "9.4", "control": "9.3", "note": "AGP 9.4 is not supported in this release. Use AGP 9.3 instead.", "expected": False},
        {"id": "synthetic-negative-planned", "kind": "synthetic", "target": "9.4", "control": "9.3", "note": "Support for AGP 9.4 is planned for a future release and is not available yet.", "expected": False},
        {"id": "synthetic-negative-different", "kind": "synthetic", "target": "9.4", "control": "9.3", "note": "Added support for AGP 9.3 and improved Gradle sync performance.", "expected": False},
        {"id": "synthetic-negative-unrelated", "kind": "synthetic", "target": "9.4", "control": "9.3", "note": "Fixed the Android project wizard and improved device discovery.", "expected": False},
        {"id": "synthetic-negative-mention", "kind": "synthetic", "target": "9.4", "control": "9.3", "note": "AGP 9.4 projects may fail to sync due to a known issue being investigated.", "expected": False},
    ])
    if sum(case["kind"] == "real-positive" for case in cases) < 2:
        raise RuntimeError("実リリースノートのpositive評価ケースが不足しています")
    return cases

def relevant_context(note, target):
    units = [unit.strip() for unit in re.split(r"(?<=[.!?;])\s+|\n+", note) if unit.strip()]
    target_units = [unit for unit in units if target in unit and re.search(r"(?i)\b(?:AGP|Android\s+Gradle\s+Plugin)\b", unit)]
    if target_units:
        return " ".join(target_units[:3])
    agp_units = [unit for unit in units if re.search(r"(?i)\b(?:AGP|Android\s+Gradle\s+Plugin)\b", unit)]
    return " ".join(agp_units[:3]) if agp_units else note

def softmax(logits, temperature):
    scaled = np.asarray(logits, dtype=np.float64) / temperature
    scaled -= np.max(scaled)
    values = np.exp(scaled)
    return values / np.sum(values)

class Engine:
    def __init__(self):
        started = time.monotonic()
        model = hf_hub_download(repo_id=MODEL_REPO, filename=MODEL_FILE)
        calibrator = hf_hub_download(repo_id=MODEL_REPO, filename=CALIBRATOR_FILE)
        self.tokenizer = AutoTokenizer.from_pretrained(MODEL_REPO)
        options = ort.SessionOptions()
        options.intra_op_num_threads = 4
        options.inter_op_num_threads = 1
        options.graph_optimization_level = ort.GraphOptimizationLevel.ORT_ENABLE_ALL
        self.session = ort.InferenceSession(model, options, providers=["CPUExecutionProvider"])
        self.calibrator = json.loads(Path(calibrator).read_text(encoding="utf-8"))
        self.load_seconds = time.monotonic() - started

    def infer(self, prompt, ids):
        tokens = self.tokenizer(prompt, truncation=True, max_length=512, return_tensors="np")
        feeds = {"input_ids": tokens["input_ids"].astype(np.int64), "attention_mask": tokens["attention_mask"].astype(np.int64)}
        started = time.monotonic()
        output = self.session.run(None, feeds)[0][0][:len(ids)]
        latency = (time.monotonic() - started) * 1000
        temperature = float(self.calibrator.get("per_k", {}).get(str(len(ids)), self.calibrator["temperature"]))
        probs = softmax(output, temperature)
        by_id = {candidate_id: float(probs[index]) for index, candidate_id in enumerate(ids)}
        return {"selected": ids[int(np.argmax(probs))], "probabilities": by_id, "latency_ms": latency}

def choice_prompt(note, target, arm):
    context = relevant_context(note, target) if "relevant" in arm else note
    if "short" in arm:
        descriptions = [
            ("supported", "support for the target AGP version is explicitly confirmed"),
            ("not_confirmed", "support for the target AGP version is not explicitly confirmed"),
        ]
    elif "four" in arm:
        descriptions = [
            ("supported", "the release explicitly confirms support for the target Android Gradle Plugin version"),
            ("unsupported", "the release explicitly says the target Android Gradle Plugin version is unsupported"),
            ("different", "the release discusses support for a different Android Gradle Plugin version"),
            ("unclear", "the release does not explicitly establish support for the target Android Gradle Plugin version"),
        ]
    else:
        descriptions = [
            ("supported", "the release explicitly confirms support for the target Android Gradle Plugin version"),
            ("not_confirmed", "the release does not explicitly confirm support for the target Android Gradle Plugin version"),
        ]
    if "reversed" in arm:
        descriptions = list(reversed(descriptions))
    ids = [candidate_id for candidate_id, _ in descriptions] + [ABSTAIN]
    labels = [f"It is {description}" for _, description in descriptions] + ["insufficient evidence"]
    question = f"Which statement best describes these release notes with respect to Android Gradle Plugin (AGP) {target}?"
    text = f"Question: {question}\\n\\nContext:\\n{context}"
    return "".join(f"{LABEL}{value}" for value in labels) + SEP + text, ids

def noul_prompt(note, target, relevant):
    context = relevant_context(note, target) if relevant else note
    proposition = f"These release notes explicitly confirm support for Android Gradle Plugin (AGP) {target}."
    labels = [f"true: {proposition}", f"false: not {proposition}", "insufficient evidence"]
    ids = ["supported", "not_confirmed", ABSTAIN]
    text = f"Context:\\n{context}\\n\\nEvaluate proposition: {proposition}"
    return "".join(f"{LABEL}{value}" for value in labels) + SEP + text, ids

def prompt_for(arm, note, target):
    if arm == "noul_raw":
        return noul_prompt(note, target, False)
    if arm == "noul_relevant":
        return noul_prompt(note, target, True)
    return choice_prompt(note, target, arm)

def metrics(rows, threshold, margin=None):
    tp = fp = tn = fn = 0
    for row in rows:
        predicted = row["selected"] == "supported" and row["p_supported"] >= threshold
        if margin is not None:
            predicted = predicted and row["p_supported"] - row["control_p_supported"] >= margin
        expected = row["expected"]
        if predicted and expected:
            tp += 1
        elif predicted and not expected:
            fp += 1
        elif not predicted and expected:
            fn += 1
        else:
            tn += 1
    total = tp + fp + tn + fn
    return {"threshold": threshold, "margin": margin, "accuracy": (tp + tn) / total, "precision": tp / (tp + fp) if tp + fp else 1.0, "recall": tp / (tp + fn) if tp + fn else 0.0, "tp": tp, "fp": fp, "tn": tn, "fn": fn}

def evaluate_arm(engine, cases, arm):
    rows = []
    for case in cases:
        prompt, ids = prompt_for(arm, case["note"], case["target"])
        result = engine.infer(prompt, ids)
        control_prompt, control_ids = prompt_for(arm, case["note"], case["control"])
        control = engine.infer(control_prompt, control_ids)
        rows.append({**case, "selected": result["selected"], "p_supported": result["probabilities"].get("supported", 0.0), "probabilities": result["probabilities"], "control_selected": control["selected"], "control_p_supported": control["probabilities"].get("supported", 0.0), "latency_ms": result["latency_ms"], "control_latency_ms": control["latency_ms"]})
    direct = [metrics(rows, threshold) for threshold in THRESHOLDS]
    contrastive = [metrics(rows, threshold, margin) for threshold in THRESHOLDS for margin in MARGINS]
    safe = [item for item in contrastive if item["fp"] == 0]
    best_safe = max(safe, key=lambda item: (item["recall"], item["accuracy"], -item["threshold"], -item["margin"])) if safe else None
    return {"arm": arm, "mean_inference_ms": sum(row["latency_ms"] + row["control_latency_ms"] for row in rows) / (2 * len(rows)), "direct": direct, "contrastive": contrastive, "best_safe": best_safe, "rows": rows}

def main():
    cases = build_cases()
    engine = Engine()
    arms = ["choice3_raw", "choice3_relevant", "choice3_relevant_short", "choice3_relevant_reversed", "choice_four_raw", "choice_four_relevant", "noul_raw", "noul_relevant"]
    arm_reports = [evaluate_arm(engine, cases, arm) for arm in arms]
    ranked = sorted([report for report in arm_reports if report["best_safe"]], key=lambda report: (report["best_safe"]["recall"], report["best_safe"]["accuracy"]), reverse=True)
    report = {
        "model": MODEL_REPO,
        "model_file": MODEL_FILE,
        "model_load_seconds": engine.load_seconds,
        "calibrator": engine.calibrator,
        "case_count": len(cases),
        "positive_cases": sum(case["expected"] for case in cases),
        "negative_cases": sum(not case["expected"] for case in cases),
        "arms": arm_reports,
        "best_arm": None if not ranked else {"arm": ranked[0]["arm"], "metrics": ranked[0]["best_safe"]},
    }
    REPORT_PATH.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({key: value for key, value in report.items() if key != "arms"}, ensure_ascii=False, indent=2))
    print("\\nARM SUMMARY")
    for item in arm_reports:
        print(f"{item['arm']}: mean_ms={item['mean_inference_ms']:.1f} best_safe={item['best_safe']}")
    print("\\nTOP ARM MISSES")
    for item in ranked[:3]:
        config = item["best_safe"]
        misses = []
        for row in item["rows"]:
            predicted = row["selected"] == "supported" and row["p_supported"] >= config["threshold"] and row["p_supported"] - row["control_p_supported"] >= config["margin"]
            if predicted != row["expected"]:
                misses.append({"id": row["id"], "expected": row["expected"], "selected": row["selected"], "p": round(row["p_supported"], 4), "control_p": round(row["control_p_supported"], 4), "note": row["note"][:180]})
        print(item["arm"], json.dumps(misses, ensure_ascii=False))

if __name__ == "__main__":
    main()
