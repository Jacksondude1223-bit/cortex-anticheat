# Cortex AntiCheat

Cortex AntiCheat is a Paper plugin that provides job-based Minecraft anti-cheat checks with packet-style Bukkit event detection, cross-server ban synchronization hooks, and recurring global ban-count announcements.

## Features

- Scheduled anti-cheat job that continuously decays violation levels.
- Packet-event detection through movement, animation, interaction, and combat events.
- Checks for speed, vertical movement, click rate, swing/interact packet rate, and reach.
- Configurable ban/IP-ban commands and violation threshold.
- Plugin-message sync channel intended for proxy-connected Bed Wars, PVP, SMP, and other backend servers.
- Optional remote IP-ban execution when a synced ban includes an IP address.
- Global announcement that reports how many Cortex bans were recorded over the previous 24 hours.

## Build

```bash
mvn package
```

The plugin jar is written to `target/cortex-anticheat-1.0.0.jar`.

## Server setup

1. Install the jar on every backend Paper server.
2. Give each server a unique `server-id` and set its `server-role` in `config.yml`.
3. Keep `sync.enabled` and the same `sync.channel` on all backend servers that are connected through your proxy.
4. Configure `punishments.command` and `punishments.ip-ban.command` for your ban system.
5. Disable `punishments.ip-ban.execute-on-remote-sync` if your network ban plugin already propagates IP bans globally.

> Note: Bukkit plugin messaging requires at least one player online to carry outbound sync messages. For production networks, pair the included channel protocol with a proxy plugin or Redis bridge for guaranteed delivery.
