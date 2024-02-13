import dev.inmo.tgbotapi.extensions.api.answers.answer
import dev.inmo.tgbotapi.extensions.api.bot.setMyCommands
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onAnyInlineQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommandWithArgs
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.text
import dev.inmo.tgbotapi.types.BotCommand
import dev.inmo.tgbotapi.types.InlineQueries.InlineQueryResult.InlineQueryResultArticle
import dev.inmo.tgbotapi.types.InlineQueries.InputMessageContent.InputTextMessageContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.berndpruenster.netlayer.tor.NativeTor
import org.berndpruenster.netlayer.tor.Tor
import org.berndpruenster.netlayer.tor.TorSocket
import org.json.JSONObject
import java.io.File
import java.math.BigInteger

//val dataSource: DataSource = try {
//    ClickHouseDataSource(System.getenv("CLICKHOUSE_URL"))
//} catch (e: SQLException) {
//    throw RuntimeException(e)
//}

fun getData(id: BigInteger): JSONObject {
    return khttp.get(" https://info.midpass.ru/api/request/${id}").jsonObject
}

fun getStatus(id: BigInteger): String {
    var res = getData(id)
    return """
        Статус паспорта: ${res.getJSONObject("passportStatus").get("name")}
        Статус заявки: ${res.getJSONObject("internalStatus").get("name")}
        Дата обновления: ${res.get("receptionDate")}
    """.trimIndent()
}



suspend fun main() {
    //set default instance, so it can be omitted whenever creating Tor (Server)Sockets
    //This will take some time
    Tor.default = NativeTor(/*Tor installation destination*/ File("tor-demo"))
    TorSocket("info.midpass.ru", 443, streamId = "FOO" /*this one is optional*/) //clear web

    telegramBotWithBehaviourAndLongPolling(System.getenv("BOT_TOKEN"),
        CoroutineScope(Dispatchers.IO,),
        defaultExceptionsHandler = { println(it) }){
            setMyCommands(
                BotCommand("q", "узнать статус заявления. Использование /q <номер заявки>")
            )
            onCommandWithArgs("q") {message, args ->
                println(message.text)

                if (args.size != 1){
                    sendTextMessage(message.chat, "Введите номер заявки")
                    return@onCommandWithArgs
                }

                val id = args.first().toBigIntegerOrNull()

                if (id == null){
                    sendTextMessage(message.chat, "Введите номер заявки")
                    return@onCommandWithArgs
                }

                sendTextMessage(message.chat, getStatus(id))
            }
            onAnyInlineQuery {
                val id = it.query.toBigIntegerOrNull()

                var res = ""
                if (id == null){
                    res = "Введите номер заявки"
                    return@onAnyInlineQuery
                } else {
                    res = getStatus(id)
                }

                answer(it,
                    listOf(InlineQueryResultArticle(
                        it.query,
                        it.query,
                        InputTextMessageContent(res)
                    ))
                )
            }
        }.second.join()

}