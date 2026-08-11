#!/bin/sh
cd "$(dirname "$0")" || exit 1

REPO="KingStackApp"
MSG="${1:-update}"

TOKEN=$(git config --global github.token)
if [ -z "$TOKEN" ]; then
    printf 'github.token が未設定です\n'
    exit 1
fi

USER=$(curl -s -H "Authorization: token $TOKEN" https://api.github.com/user | grep '"login"' | head -n 1 | cut -d '"' -f 4)
if [ -z "$USER" ]; then
    printf 'GitHubユーザーの取得に失敗しました\n'
    exit 1
fi

curl -s -o /dev/null -X POST \
    -H "Authorization: token $TOKEN" \
    -H "Accept: application/vnd.github+json" \
    -d "{\"name\":\"$REPO\",\"private\":true}" \
    https://api.github.com/user/repos

if [ ! -d .git ]; then
    git init
    git branch -M main
fi

git config user.name "$USER"
git config user.email "$USER@users.noreply.github.com"

git remote remove origin 2>/dev/null
git remote add origin "https://$TOKEN@github.com/$USER/$REPO.git"

git add -A
git commit -m "$MSG" || true
git push -u origin main || git push -u origin main --force

printf 'done: %s\n' "$REPO"
