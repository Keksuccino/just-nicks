# Just Nicks

Simple server-side mod to "nick" players, which anonymizes their names and skins.

# How to Use the Remote Persistent Nick Storage

In `options.txt`, set `persistent_nicks` to `true`, give a real `persistent_nicks_remote_url` (e.g., `https://your.host/justnicks/api`) and `persistent_nicks_remote_token`.<br>
Any placeholder/blank value keeps the local SQLite file in use.

Remote payload fields: `uuid`, `real_name`, `nickname`, optional `skin_uuid`, `skin_name`, `skin_value`, `skin_signature`.