import importlib
import json
import re
import unittest
from pathlib import Path


ATOM_SRC_PATTERN = re.compile(
    r"^(?P<module>[A-Za-z_][\w.]+)\.(?P<class>[A-Za-z_]\w*)\(\)\.(?P<method>[A-Za-z_]\w*)$"
)


class TestAtomMetadata(unittest.TestCase):
    def test_component_config_yaml_is_valid_and_matches_meta_keys(self):
        import yaml

        repo_root = Path(__file__).resolve().parents[1]
        problems = []
        checked = 0
        for component_dir in sorted((repo_root / "components").glob("astronverse-*")):
            config_path = component_dir / "config.yaml"
            meta_path = component_dir / "meta.json"
            if not config_path.exists() or not meta_path.exists():
                continue

            config = yaml.safe_load(config_path.read_text(encoding="utf-8-sig")) or {}
            meta = json.loads(meta_path.read_text(encoding="utf-8-sig"))
            if not isinstance(config, dict):
                problems.append(f"{config_path}: top-level YAML value must be a mapping")
                continue
            if not isinstance(meta, dict):
                problems.append(f"{meta_path}: top-level JSON value must be a mapping")
                continue

            config_atoms = set((config.get("atomic") or {}).keys())
            meta_atoms = set(meta.keys())
            missing_from_meta = sorted(config_atoms - meta_atoms)
            missing_from_config = sorted(meta_atoms - config_atoms)
            if missing_from_meta:
                problems.append(f"{component_dir.name}: config atoms missing from meta.json: {missing_from_meta}")
            if missing_from_config:
                problems.append(f"{component_dir.name}: meta atoms missing from config.yaml: {missing_from_config}")
            checked += 1

        self.assertGreaterEqual(checked, 20)
        self.assertEqual(problems, [])

    def test_component_meta_sources_resolve_to_runtime_callables(self):
        repo_root = Path(__file__).resolve().parents[1]
        meta_paths = sorted((repo_root / "components").glob("*/meta.json"))

        checked = 0
        problems = []
        for meta_path in meta_paths:
            data = json.loads(meta_path.read_text(encoding="utf-8-sig"))
            for key, item in data.items():
                src = item.get("src")
                if not src:
                    problems.append(f"{meta_path}: {key} is missing src")
                    continue

                match = ATOM_SRC_PATTERN.match(src)
                if not match:
                    problems.append(f"{meta_path}: {key} has unsupported src format: {src}")
                    continue

                checked += 1
                try:
                    module = importlib.import_module(match.group("module"))
                    owner = getattr(module, match.group("class"))
                    target = getattr(owner, match.group("method"))
                except Exception as exc:  # pragma: no cover - assertion message path
                    problems.append(f"{meta_path}: {key} cannot resolve {src}: {type(exc).__name__}: {exc}")
                    continue

                if not callable(target):
                    problems.append(f"{meta_path}: {key} resolves to a non-callable target: {src}")

        self.assertGreater(checked, 250)
        self.assertEqual(problems, [])


if __name__ == "__main__":
    unittest.main()
