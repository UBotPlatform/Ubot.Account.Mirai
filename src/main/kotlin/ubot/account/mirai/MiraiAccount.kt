package ubot.account.mirai

import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.Bot
import net.mamoe.mirai.closeAndJoin
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.events.MemberJoinEvent
import net.mamoe.mirai.event.events.MemberLeaveEvent
import net.mamoe.mirai.event.subscribeAlways
import net.mamoe.mirai.getGroupOrNull
import net.mamoe.mirai.message.FriendMessageEvent
import net.mamoe.mirai.message.GroupMessageEvent
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.uploadImage
import ubot.common.*
import java.net.URL
import kotlin.system.exitProcess

class MiraiAccount(private val event: UBotAccountEventEmitter,
                   private val username: String,
                   private val password: String)
    : BaseUBotAccount() {
    private var bot: Bot? = null
    override suspend fun getGroupName(id: String): String {
        return bot!!.getGroup(id.toLong()).name
    }

    override suspend fun getMemberName(source: String, target: String): String {
        if (source.isEmpty()) {
            return bot!!.getFriend(target.toLong()).nick
        }
        return bot!!.getGroup(source.toLong())[target.toLong()].nameCardOrNick
    }

    override suspend fun getSelfID(): String {
        return bot!!.selfQQ.id.toString()
    }

    override suspend fun getUserAvatar(id: String): String {
        TODO("Not yet implemented")
    }

    override suspend fun getUserName(id: String): String {
        TODO("Not yet implemented")
    }

    override suspend fun login() {
        val b = Bot(username.toLong(), password)
        bot = b
        b.subscribeAlways<MemberJoinEvent> {
            event.onMemberJoined(this.group.id.toString(), this.member.id.toString(), "")
        }
        b.subscribeAlways<MemberLeaveEvent> {
            event.onMemberLeft(this.group.id.toString(), this.member.id.toString())
        }
        b.subscribeAlways<FriendMessageEvent> {
            event.onReceiveChatMessage(ChatMessageType.Private,
                    "",
                    this.sender.id.toString(),
                    toUBotMessage(this.message),
                    ChatMessageInfo())
        }
        b.subscribeAlways<GroupMessageEvent> {
            event.onReceiveChatMessage(ChatMessageType.Group,
                    this.group.id.toString(),
                    this.sender.id.toString(),
                    toUBotMessage(this.message),
                    ChatMessageInfo())
        }
        b.login()
    }

    override suspend fun logout() {
        val b = bot
        bot = null
        b?.closeAndJoin()
    }

    override suspend fun removeMember(source: String, target: String) {
        return bot!!.getGroup(source.toLong())[target.toLong()].kick()
    }

    override suspend fun sendChatMessage(type: Int, source: String, target: String, message: String) {
        val contact = when (type) {
            ChatMessageType.Group ->
                bot!!.getGroup(source.toLong())
            ChatMessageType.Private ->
                bot!!.getFriend(target.toLong())
            else ->
                throw IllegalArgumentException("invalid type")
        }
        val parsed = ChatMessageParser.Parse(message)
        val miraiMsgBuilder = MessageChainBuilder()
        for (it in parsed) {
            miraiMsgBuilder += when (it.type) {
                "text" -> PlainText(it.data)
                "at" -> {
                    if (it.data == "all")
                        AtAll
                    else {
                        val member = bot?.getGroupOrNull(source.toLong())?.getOrNull(it.data.toLong())
                        if (member != null) At(member) else PlainText("@无效")
                    }
                }
                "face" -> Face(it.data.toInt())
                "image_online" -> contact.uploadImage(URL(it.data))
                else -> PlainText("不支持的消息")
            }
        }
        contact.sendMessage(miraiMsgBuilder.build())
    }

    override suspend fun shutupAllMember(source: String, switch: Boolean) {
        bot!!.getGroup(source.toLong()).settings.isMuteAll = switch
    }

    override suspend fun shutupMember(source: String, target: String, duration: Int) {
        if (duration == 0) {
            bot!!.getGroup(source.toLong())[target.toLong()].unmute()
        } else {
            bot!!.getGroup(source.toLong())[target.toLong()].mute(duration * 60)
        }
    }

    private suspend fun toUBotMessage(msg: Message): String {
        val builder = ChatMessageBuilder()
        for (it in msg.flatten()) {
            when (it) {
                is PlainText -> builder.add(it.content)
                is At -> builder.add("at", it.target.toString())
                is AtAll -> builder.add("at", "all")
                is Image -> builder.add("image_online", it.queryUrl())
                is MessageMetadata -> {
                }
                else -> builder.add("不支持的消息")
            }
        }
        return builder.build()
    }
}

fun main(args: Array<String>) {
    runBlocking<Unit> {
        try {
            UBotClientHost.hostAccount(args[0], args[1], args[2]) { event ->
                MiraiAccount(event, args[2], args[3]).also { it.login() }
            }
        } catch (e: Exception) {
            println("Error occurred")
            e.printStackTrace()
        }
        println("Session ended")
        exitProcess(0)
    }
}