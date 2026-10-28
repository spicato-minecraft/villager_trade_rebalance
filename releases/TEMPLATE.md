# Release changelog template

Copy this file to `releases/{minecraft_version}-{mod_version}.md` before creating a GitHub Release.

Write for players, not developers. Focus on what changed in the game.

---

## Example

```markdown
- Librarian enchanted books are now a single enchant lineage that levels up with the villager
- Fortune, Looting, and Mending books no longer appear on new librarians
```

## Guidelines

- Use plain language ("librarians" not "VillagerProfession.LIBRARIAN")
- One bullet per player-visible change
- Omit internal refactors, test additions, and CI changes unless players notice them
- Do not copy PR titles or commit messages
