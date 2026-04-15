# Docs Index

```text
docs/
├── overview.md
├── architecture/
│   ├── overview.md
│   ├── analyzer.md
│   ├── player.md
│   ├── editor.md
│   ├── storage.md
│   └── app-shell.md
├── ui-ux/
│   ├── overview.md
│   ├── library-screen.md
│   ├── import-review-flow.md
│   ├── editor-screen.md
│   └── player-screen.md
└── implementation/
    ├── implementation-roadmap.md
    └── notes.md
```

Use the docs like this:

1. `overview.md`
2. `architecture/overview.md`
3. `architecture/*.md` for component/library contracts
4. `ui-ux/*.md` for screen layout and interaction design
5. `implementation/*.md` for execution planning

Reading rule:

- if the question is "what should the app do?", start with `overview.md`
- if the question is "what component owns this?", use `architecture/`
- if the question is "what should this screen look like?", use `ui-ux/`
- if the question is "what should we build next?", use `implementation/`
