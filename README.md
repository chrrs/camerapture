# Camerapture

> Fabric mod with cameras that can take pictures!

Read more about it on [Modrinth](https://modrinth.com/mod/camerapture).

## Project Structure

Camerapture supports multiple Minecraft versions using [Stonecutter](https://stonecutter.kikugie.dev/).
The easiest way to interact with this is by using an IDE such as IntelliJ. To switch between versions,
use the Gradle tasks under the `stonecutter` category. Make sure to switch back to 1.20.4 to commit changes.

1.21 support is in a separate `1.21` branch. It could've been done using Stonecutter, but the underlying
Minecraft code has changed drastically after 1.20.4. Using versioned comments would be more work than
necessary to maintain support. This does mean that changes have to be merged manually and released manually.
This is less than ideal, and hopefully can be solved once 1.20 support is dropped.

### Release checklist

- Update the version number.
  - Change in `gradle.properties`.
  - Add an entry in `CHANGELOG.md`.
- Commit and push a new tag. (example: `v1.2.3`)
  - Tag name is the version number prefixed by `v`.
- CI should do the rest of the work.

---

> **FIXME:** Ideally, this would be less manual. The biggest problem here is the merge, and I don't want
> to force a specific 'commit first, then merge, then tag the commit' order.

- Merge `main` into the `1.21` branch.
- Manually release 1.21.
  - Run `./gradlew chiseledPublish` on the `1.21` branch.
  - Add the JAR file to the GitHub release.
 
## Credits
- henkelmax for making the [Camera Mod for Forge](https://modrinth.com/mod/camera-mod) that's used on the QSMP, for being the main inspiration of this mod.
- All the great people on our origins server for being amazing and patient with me <3
