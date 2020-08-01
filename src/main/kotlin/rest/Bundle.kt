package rest

import java.text.MessageFormat
import java.util.*

class Bundle(locale: Locale) {
    private var resource: ResourceBundle = try {
        ResourceBundle.getBundle("bundle.bundle", locale, UTF8Control())
    } catch (e: Exception) {
        ResourceBundle.getBundle("bundle.bundle", Locale.US, UTF8Control())
    }

    operator fun get(key: String, vararg params: Any?): String {
        return try {
            MessageFormat.format(resource.getString(key), *params)
        } catch (e: MissingResourceException) {
            key
        }
    }
}