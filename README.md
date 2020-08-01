# Essential-REST_API
REST API plugin that works using data from essentials plugins.

## Json form
### Request
`GET / HTTP 1.1`

### Result
```
{
  "players": 1,
  "version": 104,
  "plugin-version": "11-dev-1",
  "name": "Experimental plugin server",
  "mapname": "Glacier",
  "wave": 1,
  "enemy-count": 0,
  "gamemode": "pvp",
  "maps": [
    "Bullet/200x200",
    "map/500x200",
    "Ancient Caldera/256x256",
    "Fork/250x300",
    "Fortress/256x256",
    "Glacier/150x250",
    "Islands/256x256",
    "Labyrinth/200x200",
    "Maze/256x256",
    "Shattered/350x350",
    "Tendrils/300x300",
    "Triad/200x200",
    "Veins/350x200",
    "Wasteland/300x300"
  ],
  "admin_online": false,
  "playerlist": {
    "cloud9350": {
      "name": "cloud9350",
      "placecount": 0,
      "breakcount": 0,
      "killcount": 0,
      "deathcount": 0,
      "joincount": 5,
      "kickcount": 0,
      "level": 4,
      "exp": 7185,
      "reqexp": 8160,
      "firstdate": "1970-01-26 05:31:23.647",
      "lastdate": "1970-01-26 05:31:23.647",
      "playtime": "0:00:04:35",
      "attackclear": 0,
      "pvpwincount": 0,
      "pvplosecount": 0,
      "pvpbreakout": 0
    }
  },
  "resource": {
    "crux": {
      "copper": 200,
      "lead": 0,
      "metaglass": 0,
      "graphite": 0,
      "titanium": 0,
      "thorium": 0,
      "silicon": 0,
      "plastanium": 0,
      "phase-fabric": 0,
      "surge-alloy": 0
    },
    "sharded": {
      "copper": 200,
      "lead": 0,
      "metaglass": 0,
      "graphite": 0,
      "titanium": 0,
      "thorium": 0,
      "silicon": 0,
      "plastanium": 0,
      "phase-fabric": 0,
      "surge-alloy": 0
    }
  }
}
```

## Ranking site
### Request
`GET /rank HTTP 1.1`

### Result
Still working