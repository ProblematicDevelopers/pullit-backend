# Branch Protection Rules

## develop 브랜치
- Require pull request reviews before merging
- Dismiss stale pull request approvals when new commits are pushed
- Require status checks to pass before merging
  - Backend CI
- Require branches to be up to date before merging
- Include administrators

## main 브랜치
- Require pull request reviews before merging (2 approvals)
- Dismiss stale pull request approvals when new commits are pushed
- Require status checks to pass before merging
  - Backend CI
  - Backend Deploy
- Require branches to be up to date before merging
- Restrict who can push to matching branches (팀 리더만)
- Include administrators