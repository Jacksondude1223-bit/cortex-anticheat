# Cortex AntiCheat

Cortex AntiCheat is a legacy-compatible Spigot/Paper plugin with an optional BungeeCord sync bridge that provides job-based Minecraft anti-cheat checks with packet-style Bukkit event detection, cross-server ban synchronization hooks, and recurring global ban-count announcements.

## Features

- Scheduled anti-cheat job that continuously decays violation levels.
- Compiles against the Spigot 1.8.8 API and avoids modern-only Bukkit calls for broad server-version compatibility.
- Includes a `bungee.yml` proxy entry point for BungeeCord network-wide sync and announcements.
- Packet-event detection through movement, animation, interaction, combat, and client-brand plugin messages.
- Checks for speed, vertical movement, click rate, swing/interact packet rate, reach, ESP-style hidden-player tracking, and known hacked client brands such as Wurst.
- Configurable ban/IP-ban commands and violation threshold.
- Plugin-message sync channel intended for proxy-connected Bed Wars, PVP, SMP, and other backend servers.
- Optional remote IP-ban execution when a synced ban includes an IP address.
- Global announcement that reports how many Cortex bans were recorded over the previous 24 hours.
- Optional Discord webhook embeds for violations, punishments, synced bans, and blocked hacked clients.

## Build

```bash
mvn package
```

The plugin jar is written to `target/cortex-anticheat-1.0.0.jar`.

## Server setup

1. Install the jar on every backend Spigot/Paper server.
2. Give each server a unique `server-id` and set its `server-role` in `config.yml`.
3. Keep `sync.enabled` and the same `sync.channel` on all backend servers that are connected through your proxy.
4. Configure `punishments.command` and `punishments.ip-ban.command` for your ban system.
5. Disable `punishments.ip-ban.execute-on-remote-sync` if your network ban plugin already propagates IP bans globally.
6. To enable Discord logging, set `discord.enabled` to `true` and paste your webhook URL into `discord.webhook-url`.

> Note: Bukkit plugin messaging requires at least one player online to carry outbound sync messages. Install the same jar on BungeeCord to run the included sync bridge, which forwards Cortex ban messages between backend servers and announces the 24-hour network ban count.
