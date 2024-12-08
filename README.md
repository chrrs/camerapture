# Camerapture

> Fabric mod with cameras that can take pictures!

![Version](https://img.shields.io/github/v/release/chrrs/camerapture?include_prereleases&style=flat-square)
![Build status](https://img.shields.io/github/actions/workflow/status/chrrs/camerapture/build.yml?style=flat-square)
[![Modrinth](https://img.shields.io/modrinth/dt/9dzLWnmZ?style=flat-square&logo=modrinth)](https://modrinth.com/mod/camerapture)
[![CurseForge](https://img.shields.io/curseforge/dt/1051342?style=flat-square&logo=curseforge)](https://curseforge.com/minecraft/mc-mods/camerapture)

Read more about it on [Modrinth](https://modrinth.com/mod/camerapture)
or [CurseForge](https://curseforge.com/minecraft/mc-mods/camerapture).

## Project Structure

Camerapture supports multiple Minecraft versions using branches. See the list of branches for versions Camerapture
supports. Any version prefixed with `old/` is not supported anymore.

Currently, the main branch is `1.21.4`, which all other versions are rebased on.

### Update checklist

- Create a new branch based on the main branch.
- Update minecraft version.
    - Change in `gradle.properties` and update all dependencies.
    - **Optional:** Deprecate older version by adding `old/` to the branch name.
- Fix all conflicts and test on all loaders.
- Release new version as described below.

### Release checklist

- Update the version number.
    - Change in `gradle.properties`.
    - Add an entry in `CHANGELOG.md`.
- Commit and push a new tag. (example: `v1.2.3`)
    - Tag name is the version number prefixed by `v`.
- Rebase other versions onto the main branch and fix all conflicts.
- Manually trigger the Publish workflow on GitHub for every version.

## Credits

- henkelmax for making the [Camera Mod for Forge](https://modrinth.com/mod/camera-mod) that's used on the QSMP, for
  being the main inspiration of this mod.
- All the great people on our origins server for being amazing and patient with me <3
