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

Just Nicks © Copyright 2025 Keksuccino.
Just Nicks is licenced under DSMSLv3.
Please read LICENSE.md for more information about the license.