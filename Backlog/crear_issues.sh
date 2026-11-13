#!/usr/bin/env bash
# Crea los issues del backlog en el repositorio indicado y los agrega al proyecto de GitHub.
# Requisitos: GitHub CLI (gh) autenticado.  Uso: ./crear_issues.sh whitesans312/BACKEND_TECH [NUMERO_PROYECTO]
set -euo pipefail
REPO="${1:?Indica el repositorio, por ejemplo whitesans312/BACKEND_TECH}"
PROJECT="${2:-}"
python3 - "$REPO" "$PROJECT" <<'PY'
import csv, subprocess, sys
repo, project = sys.argv[1], sys.argv[2]
for row in csv.DictReader(open('issues_github_projects.csv', encoding='utf8')):
    subprocess.run(['gh','label','create',row['label'],'--repo',repo,'--force'], check=False)
    cmd = ['gh','issue','create','--repo',repo,'--title',row['title'],'--body',row['body'],'--label',row['label']]
    if project: cmd += ['--project', project]
    subprocess.run(cmd, check=True)
PY
