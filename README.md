<h1 align="center">
    <img src="src/main/resources/assets/htslreborn/icon.png" height="300" alt="HTSL Reborn Icon">
    <br>
    HTSL Reborn
    <br>
    <a href="https://discord.gg/pCcpqzU4He">
        <img alt="By Redstone Studios" src="https://img.shields.io/badge/By-Redstone%20Studios-red?style=for-the-badge&labelColor=%23363a4f&color=%23e64553">
    </a>
</h1>
<h4 align="center">
    <b>Patch Fork</b> - <b>Itisyan</b>
</h4>

## Overview

HTSL Reborn is a fabric mod that makes programming in Hypixel's Housing gamemode easier. With the help of [SystemsAPI](https://github.com/redstone-llc/SystemsAPI), we ported HTSL's syntax to latest versions. Now that we're based on a more modern loader, there's a large possibility for extra QoL features to come in the future!

This fork is a derivative of HTSL Reborn 0.2.0 that fixes bugs and improves speed of imports.

## Original version

<b>HTSLReborn - Redstone Studios : </b><br>
https://github.com/redstone-llc/HTSLReborn<br>
https://modrinth.com/mod/htslreborn

## Download patched build
<b>Warning:</b> <i>HTSLReborn identifies Hypixel menu names, so you must configure the Hypixel language to <b>English</b>.</i>

<b>HTSLReborn - Itisyan Fork (0.2.0.itisyan.patch4) download: </b><br>
https://github.com/Itisyan/HTSLReborn/releases/tag/patch4

Minecraft version: 1.21.11<br>
Loader: [Fabric](https://fabricmc.net/use/installer)

<b>Dependencies: (Required)</b>
- Fabric API: https://modrinth.com/mod/fabric-api/version/0.141.3+1.21.11
- oωo (owo-lib) : https://modrinth.com/mod/owo-lib/version/0.13.0+1.21.11
- SystemsAPI : https://modrinth.com/mod/systemsapi/version/jqOWChWz
- Fabric Language Kotlin : https://modrinth.com/mod/fabric-language-kotlin/version/1.13.11+kotlin.2.3.21

<b><i>If you want to use the patched version, delete any other version of HTSL from your mods folder.</i></b>

## Itisyan fork (patched version) changes

Patch4:
- Export/import: Treat the return-to-actions timeout after chat-backed text values as recoverable.
- Export: Variable actions and conditions now export with var/globalvar/teamvar while still accepting stat/globalstat/teamstat on import.
- Export: Group/team/region conditions now prefer hasGroup/hasTeam/inRegion while still accepting inGroup/inTeam/hasRegion aliases on import.
- Import: Temporarily raises menu timeouts during imports and restores them afterward.
- Import: Added recovery for transient Action Settings/Settings timeouts after paginated choices such as function, menu, layout, team, group, and region selectors.

Patch3:
- Export: Added a targeted retry/recovery for the transient SystemsAPI menu race where reading a chat-backed value can briefly leave currentScreen null and trigger "Expected GenericContainerScreen but found null".

Patch2:
- Fix Incorrect export of OR conditions: They were being exported as AND. Fixed in HTSLExporter.kt line 185.
- Fix Color + placeholder stuck together in actionBar imports: Only the color was being imported. Fixed.
- Fix StatValue with commas: 1,234, 2,345L, and 3,456.5 were sometimes parsed as text instead of numbers. Fixed in ActionParser.kt line 118 and ConditionParser.kt line 85.
- Fix Conditions with color + placeholder stuck together: Same issue as actionBar, but inside conditions. Fixed in ConditionParser.kt line 65.
- Fix Custom coordinates: launchTarget "custom_coordinates" 1 2 3 could consume 3 as the force value instead of keeping the default value. Fixed in LocationParser.kt line 27.
- File explorer: More robust folder creation with createDirectories, stricter extension filtering .htsl, .nbt, and watcher fixed to target the correct folder.
- /htsl commands: Path resolution now uses the import folder, automatic .htsl / .nbt extension completion added, absolute paths are supported, and item deletion no longer crashes when the file is missing.
- Import: Temporarily reduced the fixed SystemsAPI delay from 50 ms to 25 ms during imports, then automatically restores it after the import.
- Import: Added menu-close detection after the vanilla close to avoid an unnecessary timeout during hidden chat inputs.

## Questions

If you have any issues with this patched version, you can DM me on Discord: <b>itisyan</b> (Skorp#5135)

## Credits

Thank you, primarially, to [@BusterBrown1218](https://github.com/BusterBrown1218), the creator of the original HTSL ChatTriggers module.

HTSLReborn creators and contributors:
- [@sinender](https://github.com/sinender)
- [@Wekendd](https://github.com/Wekendd)
- [@PixelBedrock](https://github.com/PixelBedrock)
