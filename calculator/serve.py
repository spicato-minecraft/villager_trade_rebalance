#!/usr/bin/env python3
"""Serve the village ledger lab and persist calibration.json / lab.json."""

from __future__ import annotations

import json
import sys
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

DIR = Path(__file__).resolve().parent
CALIBRATION = DIR / "calibration.json"
LAB = DIR / "lab.json"
PORT = int(sys.argv[1]) if len(sys.argv) > 1 else 8080


class Handler(SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=str(DIR), **kwargs)

    def do_PUT(self) -> None:
        path = self.path.split("?", 1)[0]
        if path == "/lab.json":
            self._put_json(LAB, self._validate_lab)
            return
        if path != "/calibration.json":
            self.send_error(404, "Only PUT /calibration.json or /lab.json is supported")
            return
        self._put_json(CALIBRATION, self._validate_calibration)

    def _validate_calibration(self, data: dict) -> None:
        if data.get("version") == "2.0":
            if "materials" not in data or not isinstance(data["materials"], dict):
                raise ValueError("v2 body must include a materials object")
        elif "calibration" not in data:
            raise ValueError("Body must be v2 (materials) or v1 (calibration)")

    def _validate_lab(self, data: dict) -> None:
        if data.get("version") != 1:
            raise ValueError("lab.json must include version: 1")
        if "professions" not in data or not isinstance(data["professions"], dict):
            raise ValueError("lab.json must include a professions object")

    def _put_json(self, dest: Path, validate) -> None:
        length = int(self.headers.get("Content-Length", 0))
        body = self.rfile.read(length)
        try:
            data = json.loads(body)
            if not isinstance(data, dict):
                raise ValueError("Body must be a JSON object")
            validate(data)
            dest.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
        except (json.JSONDecodeError, ValueError, OSError) as exc:
            self.send_error(400, str(exc))
            return

        self.send_response(204)
        self.end_headers()

    def end_headers(self) -> None:
        path = self.path.split("?", 1)[0]
        if path.endswith((".html", ".js", ".css")) or path in ("/", ""):
            self.send_header("Cache-Control", "no-store")
        super().end_headers()

    def log_message(self, fmt: str, *args) -> None:
        if len(args) >= 2 and args[1] == "204":
            return
        super().log_message(fmt, *args)


def main() -> None:
    if not CALIBRATION.exists():
        CALIBRATION.write_text(
            json.dumps(
                {
                    "version": "2.0",
                    "updated_at": None,
                    "model_meta": {
                        "anchor_material": "stick",
                        "anchor_qty": 32,
                        "anchor_emeralds": 1,
                        "animal_active_min": 2,
                        "mob_hunt_active_min": 6,
                        "mob_hunt_default_risk": 1.25,
                    },
                    "materials": {},
                },
                indent=2,
            )
            + "\n",
            encoding="utf-8",
        )

    server = ThreadingHTTPServer(("127.0.0.1", PORT), Handler)
    print(f"Serving {DIR}")
    print(f"Open http://127.0.0.1:{PORT}/")
    print(f"Calibration file: {CALIBRATION}")
    print(f"Lab file: {LAB}")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nStopped.")


if __name__ == "__main__":
    main()
