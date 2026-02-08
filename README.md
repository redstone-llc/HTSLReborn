<h1 align="center">
    <img src="src/main/resources/assets/htslreborn/icon.png" height="256" alt="HTSL Reborn Icon">
    <br>
    HTSL Reborn
    <br>
    <a href="https://github.com/redstone-llc/HTSLReborn/commits/main/">
        <img alt="GitHub last commit" src="https://img.shields.io/github/last-commit/redstone-llc/HTSLReborn?style=for-the-badge&logo=github&logoColor=%23cad3f5&labelColor=%23363a4f&color=%2340a02b">
    </a>
    <a href="https://discord.gg/pCcpqzU4He">
        <img alt="By Redstone Studios" src="https://img.shields.io/badge/By-Redstone%20Studios-red?style=for-the-badge&labelColor=%23363a4f&color=%23e64553">
    </a>
    <br>
    <a href="https://modrinth.com/mod/htslreborn">
        <img alt="Static Badge" src="https://img.shields.io/badge/-Modrinth-green?style=for-the-badge&logo=modrinth&logoColor=%23c6d0f5&labelColor=%23363A4F&color=%2340a02b">
    </a>
</h1>

## Overview

HTSL Reborn is a fabric mod that makes programming in Hypixel's Housing gamemode easier. With the help of [SystemsAPI](https://github.com/redstone-llc/SystemsAPI), we ported HTSL's syntax to latest versions. Now that we're based on a more modern loader, there's a large possibility for extra QoL features to come in the future!

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

## Demo

https://github.com/user-attachments/assets/a7a782d7-e7f4-42f0-a898-65cadbcea64b

## Questions

<details>
<summary><b>How do I migrate?</b></summary>

To migrate from HTSL to HTSL Reborn, just copy all your imports into the new HTSL folder, located in the root of your minecraft instance. You should be good to go once things have been moved over.
    
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

If you think you may have found a bug, please [create an issue](https://github.com/redstone-llc/HTSLReborn/issues). Otherwise, feel free to join our [discord](https://discord.gg/pCcpqzU4He) or [create a discussion post](https://github.com/redstone-llc/HTSLReborn/discussions).
    
</details>

## Credits

Thank you, primarially, to [@BusterBrown1218](https://github.com/BusterBrown1218), the creator of the original HTSL ChatTriggers module.
