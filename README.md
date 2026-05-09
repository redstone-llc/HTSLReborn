<h1 align="center">
    <img src="src/main/resources/assets/htslreborn/icon.png" height="300" alt="HTSL Reborn Icon">
    <br>
    HTSL Reborn
    <br>
    <a href="https://github.com/redstone-llc/HTSLReborn/commits/main/">
        <img alt="GitHub last commit" src="https://img.shields.io/github/last-commit/redstone-llc/HTSLReborn?style=for-the-badge&logo=github&logoColor=%23cad3f5&labelColor=%23363a4f&color=%2340a02b">
    </a>
    <a href="https://modrinth.com/mod/htslreborn">
        <img alt="Static Badge" src="https://img.shields.io/badge/-Modrinth-green?style=for-the-badge&logo=modrinth&logoColor=%23c6d0f5&labelColor=%23363A4F&color=%2340a02b">
    </a>
    <br>
    <a href="https://discord.gg/pCcpqzU4He">
        <img alt="By Redstone Studios" src="https://img.shields.io/badge/By-Redstone%20Studios-red?style=for-the-badge&labelColor=%23363a4f&color=%23e64553">
    </a>
</h1>

## Overview

HTSL Reborn is a fabric mod that makes programming in Hypixel's Housing gamemode easier. With the help of [SystemsAPI](https://github.com/redstone-llc/SystemsAPI), we ported HTSL's syntax to latest versions. Now that we're based on a more modern loader, there's a large possibility for extra QoL features to come in the future!

## Download build

Itisyan patch v3 (0.2.0.itisyan.patch3):
https://github.com/Itisyan/HTSLReborn/releases/tag/patch3

redstone-llc original (0.2.0): 
https://modrinth.com/mod/htslreborn/version/0.2.0

## Features

- [x] Language parity with HTSL (your HTSL scripts will work flawlessly!)
  - [x] Expressions (Javascript math, etc.)
  - [x] Compiler shortcuts (define statements)
  - [x] Loops
- [x] Importing to Housing (with multiple methods:)
  - [x] Add: adds new actions after existing ones
  - [x] Replace: automatically replaces old actions with new
  - [ ] Update: optimally adjust existing actions so import takes less time (soon!)
- [x] Exporting to code
- [x] File browser hot-reloading
- [x] Item support

## Itisyan fork
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

## Demo

https://github.com/user-attachments/assets/a7a782d7-e7f4-42f0-a898-65cadbcea64b

## Questions

<details>
<summary><b>How do I migrate?</b></summary>

To migrate from HTSL to HTSL Reborn, just copy all your imports into the new HTSL folder, located in the root of your Minecraft instance. You should be good to go once things have been moved over.
    
</details>

<details>
<summary><b>What's better about this compared to the ChatTriggers module?</b></summary>

There are a couple of reasons why we abandoned HTSL to make this new mod.
* We wanted to update to the latest version of Minecraft.
* We wanted to leave the ChatTriggers ecosystem, opting for the power an actual modding framework gives us.
* We wanted to start fresh with a clean codebase (the old one was starting to get out of hand).

What does that mean for you?
* Fewer bugs (in the long term)
* Latest-version (and multi-version) support
* More features!
    
</details>

<details>
<summary><b>I need help!</b></summary>

If you need help figuring out the syntax of HTSL or aren't sure how to use it, check out our [wiki](https://github.com/redstone-llc/HTSLReborn/wiki)!
If you think you may have found a bug, please [create an issue](https://github.com/redstone-llc/HTSLReborn/issues). Otherwise, feel free to join our [discord](https://discord.gg/pCcpqzU4He) or [create a discussion post](https://github.com/redstone-llc/HTSLReborn/discussions).
    
</details>

## Credits

Thank you, primarially, to [@BusterBrown1218](https://github.com/BusterBrown1218), the creator of the original HTSL ChatTriggers module.
