<a href="https://discord.gg/rhayah27GC"><img src="https://img.shields.io/discord/704163135787106365?style=flat&label=Discord&labelColor=%234260f5&color=%2382aeff" /></a> <a href="https://paypal.me/TimSchroeter"><img src="https://img.shields.io/badge/Donate%20via%20PayPal-%233d91ff?style=flat" /></a> <a href="https://www.patreon.com/keksuccino"><img src="https://img.shields.io/badge/Support%20me%20on%20Patreon-%23ff9b3d?style=flat" /></a>

# About

Just Nicks is a simple **server-side** mod to "nick" players, which means it anonymizes them by giving them a random nickname and skin.

The mod works very similar to how nick features are handled on Bukkit/Spigot servers, so everything is handled purely on the server and clients do not need to have the mod installed.

# Slash Commands

- `/nick` — Gives you a random nickname and signed skin. Requires permission `justnicks.permission.nick` (or op level 4).
- `/nick <name>` — Sets a custom nickname; attempts to fetch a matching signed skin for that name. Same permission as `/nick`.
- `/unnick` — Removes your nickname and restores your real identity. Requires `justnicks.permission.unnick` (or op level 4).
- `/justnicksoptions <option> <value>` — Changes a Just Nicks option at runtime. Requires `justnicks.permission.edit_options` (or op level 4). Option keys match `options.txt`; value type must match each option (boolean, number, or string). Tab-complete suggests available keys and example value formats.

# Permissions

- `justnicks.permission.nick` — Allow using `/nick` with or without a custom name.
- `justnicks.permission.unnick` — Allow using `/unnick`.
- `justnicks.permission.edit_options` — Allow editing options via `/justnicksoptions`.

Operator level 4 bypasses permission checks.

Use a permission mod like [LuckPerms (Fabric/NeoForge)](https://modrinth.com/plugin/luckperms) to assign these permissions to player groups.

# How to Use the Remote Persistent Nick Storage

By default, Just Nicks saves nick states in a local database file (if persistent nicks are enabled in options), but you can also use a remote database.

In `options.txt`, set `persistent_nicks` to `true`, give a real `persistent_nicks_remote_url` (e.g., `https://your.host/justnicks/api`) and `persistent_nicks_remote_token`.<br>
Any placeholder/blank value keeps the local SQLite file in use.

Remote payload fields: `uuid`, `real_name`, `nickname`, optional `skin_uuid`, `skin_name`, `skin_value`, `skin_signature`.

# Copyright

Just Nicks © Copyright 2025 Keksuccino.<br>
Just Nicks is licenced under DSMSLv3.<br>
Please read LICENSE.md for more information about the license.

# Server Needed?

Looking to play Minecraft with friends but setting up a server is just too time-consuming?<br>
No worries! Simply rent a pre-configured server and start playing _in a snap_.

Just click the image below and use code **[keksuccino](https://bisecthosting.com/keksuccino)** to enjoy a **25% discount** on your first month! You can also click the code itself if your ad blocker hides the banner.

<br>

<a href="https://tinyurl.com/bisectkeks"><img src="https://www.bisecthosting.com/partners/custom-banners/f5dd9194-01d8-4ce3-9b6a-a85327d975b1.png" /></a>