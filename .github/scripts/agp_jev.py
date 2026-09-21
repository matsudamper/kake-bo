#!/usr/bin/env python3
# プロンプトと閾値は agp_jev_tuning.py の choice2_semantic_simple_no_abstain アームに揃える。
# 揃えていないと agp_jev_tuning.py の評価結果が本番判定の根拠にならない。
import json
import re
import time
from pathlib import Path

import numpy as np
import onnxruntime as ort
from huggingface_hub import hf_hub_download
from transformers import AutoTokenizer

MODEL_REPO = "heman10x/rlcd-modernbert-151m"
MODEL_FILE = "model_fp16.onnx"
CALIBRATOR_FILE = "calibrator.json"
LABEL = "<<LABEL>>"
SEP = "<<SEP>>"
SUPPORTED = "supported"
NOT_CONFIRMED = "not_confirmed"
# choice2_semantic_simple_no_abstain の positive ケースは p=0.64〜0.87 / 対照差 0.04〜0.13 に収まる。
# 対照差はどの設定でも僅差で、誤検知ゼロにできる設定では recall が 0 になるため、この判定は参考値に留める。
SUPPORT_PROBABILITY_THRESHOLD = 0.65
CONTROL_MARGIN = 0.05
AGP_MENTION_PATTERN = re.compile(r"(?i)\b(?:AGP|Android\s+Gradle\s+Plugin)\b")
AGP_VERSION_PATTERN = re.compile(
    r"(?i)\b(?:Android\s+Gradle\s+Plugin|AGP)\s*(?:version\s+|v\s*)?(?:>=?\s*)?(\d+\.\d+(?:\.\d+)?)"
)


def mentions_agp(note):
    return bool(AGP_MENTION_PATTERN.search(note))


def major_minor(version):
    match = re.match(r"^(\d+)\.(\d+)", version)
    if not match:
        raise ValueError(f"AGP バージョンを解釈できません: {version}")
    return f"{int(match.group(1))}.{int(match.group(2))}"


def control_version(note, target):
    major, minor = map(int, major_minor(target).split("."))
    candidate = f"{major}.{minor + 2}"
    while candidate in note:
        minor += 2
        candidate = f"{major}.{minor + 2}"
    return candidate


def relevant_context(note, target):
    units = [unit.strip() for unit in re.split(r"(?<=[.!?;])\s+|\n+", note) if unit.strip()]
    target_units = [unit for unit in units if target in unit and mentions_agp(unit)]
    if target_units:
        return " ".join(target_units[:3])
    agp_units = [unit for unit in units if mentions_agp(unit)]
    return " ".join(agp_units[:3]) if agp_units else note


def semantic_context(note, target):
    context = relevant_context(note, target)
    target_major_minor = major_minor(target)

    def replace_version(match):
        marker = "TARGET_VERSION" if major_minor(match.group(1)) == target_major_minor else "OTHER_VERSION"
        return f"target dependency {marker}"

    context = AGP_VERSION_PATTERN.sub(replace_version, context)
    return AGP_MENTION_PATTERN.sub("target dependency", context)


def build_prompt(note, target):
    descriptions = [
        (SUPPORTED, "support for target dependency TARGET_VERSION is available in this release"),
        (NOT_CONFIRMED, "support for target dependency TARGET_VERSION is not established by this release"),
    ]
    labels = [f"It is {description}" for _, description in descriptions]
    question = f"Which statement best describes these release notes with respect to Android Gradle Plugin (AGP) {target}?"
    text = f"Question: {question}\n\nContext:\n{semantic_context(note, target)}"
    ids = [candidate_id for candidate_id, _ in descriptions]
    return "".join(f"{LABEL}{value}" for value in labels) + SEP + text, ids


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

    def support_probability(self, note, target):
        prompt, ids = build_prompt(note, target)
        tokens = self.tokenizer(prompt, truncation=True, max_length=512, return_tensors="np")
        feeds = {
            "input_ids": tokens["input_ids"].astype(np.int64),
            "attention_mask": tokens["attention_mask"].astype(np.int64),
        }
        output = self.session.run(None, feeds)[0][0][:len(ids)]
        temperature = float(self.calibrator.get("per_k", {}).get(str(len(ids)), self.calibrator["temperature"]))
        probabilities = softmax(output, temperature)
        selected = ids[int(np.argmax(probabilities))]
        return selected, float(probabilities[ids.index(SUPPORTED)])

    def judge(self, note, target):
        control = control_version(note, target)
        selected, probability = self.support_probability(note, target)
        _, control_probability = self.support_probability(note, control)
        supported = (
            selected == SUPPORTED
            and probability >= SUPPORT_PROBABILITY_THRESHOLD
            and probability - control_probability >= CONTROL_MARGIN
        )
        return {
            "model": MODEL_REPO,
            "supported": supported,
            "probability": probability,
            "control_version": control,
            "control_probability": control_probability,
        }
