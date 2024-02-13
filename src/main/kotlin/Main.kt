import dev.inmo.tgbotapi.extensions.api.answers.answer
import dev.inmo.tgbotapi.extensions.api.bot.setMyCommands
import dev.inmo.tgbotapi.extensions.api.send.media.sendDocument
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onAnyInlineQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommandWithArgs
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.text
import dev.inmo.tgbotapi.extensions.utils.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.replyKeyboard
import dev.inmo.tgbotapi.requests.abstracts.InputFile
import dev.inmo.tgbotapi.types.BotCommand
import dev.inmo.tgbotapi.types.InlineQueries.InlineQueryResult.InlineQueryResultArticle
import dev.inmo.tgbotapi.types.InlineQueries.InputMessageContent.InputTextMessageContent
import dev.inmo.tgbotapi.utils.row
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.berndpruenster.netlayer.tor.NativeTor
import org.berndpruenster.netlayer.tor.Tor
import org.berndpruenster.netlayer.tor.TorSocket
import org.json.JSONObject
import java.io.File
import java.math.BigInteger

fun getData(id: BigInteger): Pair<JSONObject, String> {
    val res = khttp.get(" https://info.midpass.ru/api/request/${id}")
    return Pair(res.jsonObject, res.text)
}

fun getStatus(id: BigInteger): Pair<File, String> {
    var data = getData(id)
    val res = """
        Статус паспорта: ${data.first.getJSONObject("passportStatus").get("name")}
        Статус заявки: ${data.first.getJSONObject("internalStatus").get("name")}
        Дата обновления: ${data.first.get("receptionDate")}
    """.trimIndent()
    val file = File("$id.json")
    file.writeText(data.second)
    return Pair(file, res)
}


suspend fun main() {
    //set default instance, so it can be omitted whenever creating Tor (Server)Sockets
    //This will take some time
    Tor.default = NativeTor(/*Tor installation destination*/ File("tor-demo"))
    TorSocket("info.midpass.ru", 443, streamId = "FOO" /*this one is optional*/) //clear web

    telegramBotWithBehaviourAndLongPolling(System.getenv("BOT_TOKEN"),
        CoroutineScope(Dispatchers.IO),
        defaultExceptionsHandler = { println(it) }) {
        setMyCommands(
            BotCommand("q", "узнать статус заявления. Использование /q <номер заявки>")
        )
        onCommandWithArgs("q") { message, args ->
            println(message.text)

            if (args.size != 1) {
                sendTextMessage(message.chat, "Введите номер заявки")
                return@onCommandWithArgs
            }

            val id = args.first().toBigIntegerOrNull()

            if (id == null) {
                sendTextMessage(message.chat, "Введите номер заявки")
                return@onCommandWithArgs
            }

            val status = getStatus(id)
            sendTextMessage(message.chat, status.second)
            sendDocument(message.chat, InputFile.fromFile(status.first))
            status.first.delete()
        }
        onAnyInlineQuery {
            println(it.query)
            val id = it.query.toBigIntegerOrNull()

            var res = ""
            if (id == null) {
                res = "Введите номер заявки"
                return@onAnyInlineQuery
            } else {
                res = getStatus(id).second
            }

            answer(
                it,
                listOf(
                    InlineQueryResultArticle(
                        it.query,
                        it.query,
                        InputTextMessageContent(res)
                    )
                )
            )
        }
    }.second.join()

}