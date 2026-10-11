#!/usr/bin/env python3
"""
GitHub Release Download Counter
------------------------------

Dieses Skript liest über die GitHub REST API aus,
wie oft die einzelnen Release-Assets eines Repositories
heruntergeladen wurden.

Beispiel:
    python3 github_downloads.py owner repo

Optional:
    Setze eine GitHub-API-Token-Umgebungsvariable:
        export GITHUB_TOKEN=dein_token
    → erhöht das API-Limit.
"""

import os
import sys
import requests


def get_release_data(owner, repo, token=None):
    """Fragt die GitHub API nach allen Releases eines Repositories."""
    url = f"https://api.github.com/repos/{owner}/{repo}/releases"
    headers = {}

    if token:
        headers["Authorization"] = f"Bearer {token}"

    response = requests.get(url, headers=headers)

    if response.status_code != 200:
        raise RuntimeError(
            f"Fehler beim Abrufen der API-Daten: {response.status_code} – {response.text}"
        )

    return response.json()


def print_download_stats(releases):
    """Gibt die Downloadzahlen pro Release und pro Asset aus."""
    total_downloads = 0

    for release in releases:
        print(f"\n=== Release: {release.get('name')} ({release.get('tag_name')}) ===")

        assets = release.get("assets", [])
        if not assets:
            print("  Keine Assets vorhanden.")
            continue

        for asset in assets:
            name = asset.get("name")
            count = asset.get("download_count", 0)
            print(f"  {name:40}  →  {count} Downloads")
            total_downloads += count

    print("\n--------------------------------------------")
    print(f"Gesamte Downloads aller Releases: {total_downloads}")
    print("--------------------------------------------")


def main():
    if len(sys.argv) != 3:
        print("Usage: python3 github_downloads.py <owner> <repo>")
        sys.exit(1)

    owner = sys.argv[1]
    repo = sys.argv[2]

    token = os.getenv("GITHUB_TOKEN")

    print(f"Frage GitHub API ab für: {owner}/{repo} ...")

    releases = get_release_data(owner, repo, token)
    print_download_stats(releases)


if __name__ == "__main__":
    main()
