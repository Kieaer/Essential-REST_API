package rest

import arc.Core
import arc.util.Log
import mindustry.Vars
import mindustry.Vars.playerGroup
import mindustry.Vars.world
import mindustry.core.GameState
import mindustry.core.Version
import mindustry.game.Gamemode
import mindustry.type.ItemType
import org.hjson.JsonArray
import org.hjson.JsonObject
import org.hjson.Stringify
import org.jsoup.Jsoup
import rest.Main.Companion.pluginVersion
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.nio.charset.StandardCharsets
import java.sql.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class Network : Thread(){
    lateinit var conn: Connection
    var serverSocket: ServerSocket = ServerSocket(80)

    override fun run() {
        try {
            org.h2.Driver.load()
            conn = DriverManager.getConnection("jdbc:h2:tcp://localhost:9079/player", "", "")

            while (!serverSocket.isClosed) {
                val socket = serverSocket.accept()
                val service = Service(socket)
                service.start()
            }
        } catch (e: SocketTimeoutException) {
            Log.warn("Essential plugin hooked but DBServer isn't enabled. Please check settings -> database -> DBServer in mods/Essentials/config.hjson file.")
            conn.close()
            serverSocket.close()
            currentThread().interrupt()
        } catch (e: IOException) {
            conn.close()
            serverSocket.close()
            currentThread().interrupt()
        }
    }

    inner class Service(private val socket: Socket) : Thread(){
        private val br: BufferedReader = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
        private val ip = socket.inetAddress.hostAddress

        override fun run() {
            val request = br.readLine()

            BufferedWriter(OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)).use { bw ->
                when {
                    request.matches(Regex("GET /rank HTTP/.*")) -> {
                        val data = ranking()
                        sendHeader(bw, data, "text/html")
                        bw.write(data)
                        bw.flush()
                    }
                    request.matches(Regex("POST /rank HTTP/.*")) -> {
                        // TODO 계정 확인
                    }
                    request.matches(Regex("GET / HTTP/.*")) -> {
                        val data = if(Vars.state.`is`(GameState.State.playing)) getJson().toString(Stringify.FORMATTED) else JsonObject().add("status", "There are no maps running on the server!").toString(Stringify.FORMATTED)
                        sendHeader(bw, data, "application/json")
                        bw.write(data)
                        bw.flush()
                    }
                    request.matches(Regex("GET /favicon.ico HTTP/.*")) -> {
                        send404(bw)
                    }
                }
            }

            br.close()
            socket.close()
        }

        private fun sendHeader(bw: BufferedWriter, data: String, type: String) {
            bw.write("HTTP/1.1 200 OK\r\n")
            bw.write("Date: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd a HH:mm:ss SSS", Locale.ENGLISH))}\r\n")
            bw.write("Server: Mindustry/Essentials $pluginVersion\r\n")
            bw.write("Content-Type: $type; charset=utf-8\r\n")
            bw.write("Content-Length: ${data.toByteArray().size + 1}")
            bw.write("\r\n")
            bw.flush()
        }

        private fun send404(bw: BufferedWriter){
            bw.write("HTTP/1.1 418 I'm a teapot\r\n")
            bw.write("Date: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd a HH:mm:ss SSS", Locale.ENGLISH))}\r\n")
            bw.write("Server: Mindustry/Essentials $pluginVersion\r\n")
        }

        private fun getJson(): JsonObject{
            val json = JsonObject()
            json.add("players", playerGroup.size()) // 플레이어 인원
            json.add("version", Version.build) // 버전
            json.add("plugin-version", pluginVersion)
            json.add("name", Core.settings.getString("servername"))
            json.add("mapname", world.map.name())
            json.add("wave", Vars.state.wave)
            json.add("enemy-count", Vars.state.enemies)
            json.add("gamemode", Gamemode.bestFit(Vars.state.rules).name)


            val maps = JsonArray()
            for (map in Vars.maps.all()) {
                maps.add(map.name()+"/"+map.width+"x"+map.height)
            }
            json.add("maps",maps)

            var online = false
            for (p in playerGroup.all()) {
                if (p.isAdmin) {
                    online = true
                    break
                }
            }
            json.add("admin_online", online)

            val players = JsonObject()
            for (p in playerGroup.all()) {
                val pstmt = conn.prepareStatement("SELECT * from players WHERE uuid=?")
                pstmt.setString(1, p.uuid)
                val rs = pstmt.executeQuery()
                if(rs.next()){
                    val buffer = JsonObject()
                    buffer.add("name", rs.getString("name"))
                    buffer.add("placecount", rs.getInt("placecount"))
                    buffer.add("breakcount", rs.getInt("breakcount"))
                    buffer.add("killcount", rs.getInt("killcount"))
                    buffer.add("deathcount", rs.getInt("deathcount"))
                    buffer.add("joincount", rs.getInt("joincount"))
                    buffer.add("kickcount", rs.getInt("kickcount"))
                    buffer.add("level", rs.getInt("level"))
                    buffer.add("exp", rs.getInt("exp"))
                    buffer.add("reqexp", rs.getInt("reqexp"))
                    buffer.add("firstdate", Tool().longToDateTime(rs.getLong("firstdate")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")))
                    buffer.add("lastdate", Tool().longToDateTime(rs.getLong("lastDate")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")))
                    buffer.add("playtime", Tool().longToTime(rs.getLong("playtime")))
                    buffer.add("attackclear", rs.getInt("attackclear"))
                    buffer.add("pvpwincount", rs.getInt("pvpwincount"))
                    buffer.add("pvplosecount", rs.getInt("pvplosecount"))
                    buffer.add("pvpbreakout", rs.getInt("pvpbreakout"))

                    players.add(rs.getString("name"), buffer)
                }
            }
            json.add("playerlist", players)

            val team = JsonObject()
            for(t in Vars.state.teams.active){
                val items = JsonObject()
                for (item in Vars.content.items()) {
                    if (item.type == ItemType.material) {
                        items.add(item.name, Vars.state.teams[t.team].cores.first().items[item]) // resources
                    }
                }
                team.add(t.team.name, items)
            }
            json.add("resource", team)

            val rank = JsonObject()
            val list = arrayOf(
                "placecount",
                "breakcount",
                "killcount",
                "joincount",
                "kickcount",
                "exp",
                "playtime",
                "pvpwincount",
                "reactorcount"
            )
            for (s in list) {
                val sql = "SELECT $s,name FROM players ORDER BY `$s`"

                val pstmt = conn.prepareStatement(sql)
                val result = pstmt.executeQuery()
                while(result.next()){
                    rank.add(result.getString("name"), result.getString(s))
                }
                pstmt.close()
                result.close()
            }

            return json
        }

        private fun ranking(): String {
            val lists = arrayOf("placecount", "breakcount", "killcount", "joincount", "kickcount", "exp", "playtime", "pvpwincount", "reactorcount", "attackclear")
            val results = JsonObject()
            val language = Tool().getGeo(ip)
            val sql = arrayOf(
                "SELECT * FROM players ORDER BY `placecount` DESC LIMIT 15",
                "SELECT * FROM players ORDER BY `breakcount` DESC LIMIT 15",
                "SELECT * FROM players ORDER BY `killcount` DESC LIMIT 15",
                "SELECT * FROM players ORDER BY `joincount` DESC LIMIT 15",
                "SELECT * FROM players ORDER BY `kickcount` DESC LIMIT 15",
                "SELECT * FROM players ORDER BY `exp` DESC LIMIT 15",
                "SELECT * FROM players ORDER BY `playtime` DESC LIMIT 15",
                "SELECT * FROM players ORDER BY `pvpwincount` DESC LIMIT 15",
                "SELECT * FROM players ORDER BY `reactorcount` DESC LIMIT 15",
                "SELECT * FROM players ORDER BY `attackclear` DESC LIMIT 15"
            )
            val bundle = Bundle(language)
            val name = bundle["server.http.rank.name"]
            val country = bundle["server.http.rank.country"]
            val win = bundle["server.http.rank.pvp-win"]
            val lose = bundle["server.http.rank.pvp-lose"]
            val rate = bundle["server.http.rank.pvp-rate"]
            var stmt: Statement? = null
            var rs: ResultSet? = null
            try {
                stmt = conn.createStatement()
                for (a in sql.indices) {
                    rs = stmt.executeQuery(sql[a])
                    val array = JsonArray()
                    if (lists[a] == "pvpwincount") {
                        val header = "<tr><th>$name</th><th>$country</th><th>$win</th><th>$lose</th><th>$rate</th></tr>"
                        array.add(header)
                        while (rs.next()) {
                            val percent: Int = try {
                                rs.getInt("pvpwincount") / rs.getInt("pvplosecount") * 100
                            } catch (e: Exception) {
                                0
                            }
                            val data = """
                                <tr><td>${rs.getString("name")}</td><td>${rs.getString("country")}</td><td>${rs.getInt("pvpwincount")}</td><td>${rs.getInt("pvplosecount")}</td><td>$percent%</td></tr>
                                
                                """.trimIndent()
                            array.add(data)
                        }
                    } else {
                        val header = "<tr><th>" + name + "</th><th>" + country + "</th><th>" + lists[a] + "</th></tr>"
                        array.add(header)
                        while (rs.next()) {
                            val data = """<tr><td>${rs.getString("name")}</td><td>${rs.getString("country")}</td><td>${rs.getString(lists[a])}</td></tr>""".trimIndent()
                            array.add(data)
                        }
                    }
                    results.add(lists[a], array)
                }
            } catch (e: SQLException) {
                e.printStackTrace()
            } finally {
                if (stmt != null) try {
                    stmt.close()
                } catch (ignored: SQLException) {
                }
                if (rs != null) try {
                    rs.close()
                } catch (ignored: SQLException) {
                }
            }
            val reader = javaClass.getResourceAsStream("/HTML/rank.html")
            val br = BufferedReader(InputStreamReader(reader, StandardCharsets.UTF_8))
            var line: String?
            val result = StringBuilder()
            while (br.readLine().also { line = it } != null) {
                result.append(line).append("\n")
            }
            val doc = Jsoup.parse(result.toString())
            for (s in lists) {
                for (b in 0 until results[s].asArray().size()) {
                    doc.getElementById(s).append(results[s].asArray()[b].asString())
                }
            }
            doc.getElementById("info_body").appendText("SERVER INFO (Not now)")
            doc.getElementById("rank-placecount").appendText(bundle["server.http.rank.placecount"])
            doc.getElementById("rank-breakcount").appendText(bundle["server.http.rank.breakcount"])
            doc.getElementById("rank-killcount").appendText(bundle["server.http.rank.killcount"])
            doc.getElementById("rank-joincount").appendText(bundle["server.http.rank.joincount"])
            doc.getElementById("rank-kickcount").appendText(bundle["server.http.rank.kickcount"])
            doc.getElementById("rank-exp").appendText(bundle["server.http.rank.exp"])
            doc.getElementById("rank-playtime").appendText(bundle["server.http.rank.playtime"])
            doc.getElementById("rank-pvpwincount").appendText(bundle["server.http.rank.pvpcount"])
            doc.getElementById("rank-reactorcount").appendText(bundle["server.http.rank.reactorcount"])
            doc.getElementById("rank-attackclear").appendText(bundle["server.http.rank.attackclear"])
            return doc.toString()
        }
    }
}
