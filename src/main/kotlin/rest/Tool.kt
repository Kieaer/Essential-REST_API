package rest

import arc.Core
import com.ip2location.IP2Location
import com.neovisionaries.i18n.CountryCode
import mindustry.Vars
import mindustry.gen.Playerc
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.*

class Tool {
    fun longToTime(seconds: Long): String {
        val min = seconds / 60
        val hour = min / 60
        val days = hour / 24
        return String.format("%d:%02d:%02d:%02d",
            days % 365, hour % 24, min % 60, seconds % 60)
    }

    fun longToDateTime(mils: Long): LocalDateTime {
        return Timestamp(mils).toLocalDateTime()
        //return LocalDateTime.ofInstant(Instant.ofEpochMilli(mils), TimeZone.getDefault().toZoneId())
    }

    fun getGeo(data: Any): Locale {
        val ip = if (data is Playerc) Vars.netServer.admins.getInfo(data.uuid()).lastIP else (data as String?)!!

        val ipre = IP2Location()
        ipre.IPDatabasePath = Core.settings.dataDirectory.child("mods/Essentials/data/IP2LOCATION-LITE-DB1.BIN").absolutePath()
        ipre.UseMemoryMappedFile = true

        val res = ipre.IPQuery(ip)
        val code = CountryCode.getByCode(res.countryShort)
        ipre.Close()

        return if (code == null) Locale.getDefault() else code.toLocale()
    }
}