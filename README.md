# BedWars1058-SoloControls

Debug helper for BedWars1058 (and compatible forks). Build: `gradle build` (JDK 17/21, only the Spigot 1.8.8 API).

## /bw start
`/bw start [seconds]` in an arena lobby (permission `solocontrols.use`, default op) starts the game with any number of
players, including just you. Also `/bw-solocontrols start [seconds]`.

## Config in game
`/bw-solocontrols config <list [section] | get <path> | set <path> <value> | add <path> <value> | remove <path> <value> | reload>`
Changes apply immediately and are written to config.yml (comments are kept). `/bw-solocontrols reload` re-reads the file.

## /bw-solocontrols end
Ends the game immediately (BedWars sends everyone back to the lobby and resets the map).
* Playing in the game: needs `solocontrols.end`.
* Spectating the game: needs `solocontrols.end.spectating`.
Both default to op. Aliases: `/bwsc`, `/bwd`, `/bwdebug`.

## Developer note
This plugin is coded with AI assistance and architected completely by me. Regardless, use at your own risk.

## License
This plugin is under the MIT License, check the [License](LICENSE) file for more details.