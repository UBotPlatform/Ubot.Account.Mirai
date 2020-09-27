package ubot.account.mirai

import io.ktor.client.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.Bot
import net.mamoe.mirai.closeAndJoin
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.event.subscribeAlways
import net.mamoe.mirai.getGroupOrNull
import net.mamoe.mirai.message.FriendMessageEvent
import net.mamoe.mirai.message.GroupMessageEvent
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.uploadImage
import net.mamoe.mirai.qqandroid.QQAndroid
import net.mamoe.mirai.utils.BotConfiguration
import ubot.common.*
import java.io.File
import java.io.InputStream
import kotlin.system.exitProcess

class MiraiAccount(private val event: UBotAccountEventEmitter,
                   private val bot: Bot)
    : BaseUBotAccount() {
    init {
        bot.subscribeAlways<MemberJoinEvent> {
            event.onMemberJoined(this.group.id.toString(), this.member.id.toString(), "")
        }
        bot.subscribeAlways<MemberLeaveEvent> {
            event.onMemberLeft(this.group.id.toString(), this.member.id.toString())
        }
        bot.subscribeAlways<FriendMessageEvent> {
            event.onReceiveChatMessage(ChatMessageType.Private,
                    "",
                    this.sender.id.toString(),
                    toUBotMessage(this.message),
                    ChatMessageInfo())
        }
        bot.subscribeAlways<GroupMessageEvent> {
            event.onReceiveChatMessage(ChatMessageType.Group,
                    this.group.id.toString(),
                    this.sender.id.toString(),
                    toUBotMessage(this.message),
                    ChatMessageInfo())
        }
        bot.subscribeAlways<BotInvitedJoinGroupRequestEvent> {
            val r = event.processGroupInvitation(this.invitorId.toString(),
                    this.groupId.toString(),
                    "")
            when (r.type) {
                10 -> this.accept()
                20 -> this.ignore()
            }
        }
        bot.subscribeAlways<MemberJoinRequestEvent> {
            val r = event.processMembershipRequest(this.groupId.toString(),
                    this.fromId.toString(),
                    "",
                    this.message)
            when (r.type) {
                10 -> this.accept()
                20 -> this.reject(message = r.reason ?: "")
            }
        }
        bot.subscribeAlways<NewFriendRequestEvent> {
            val r = event.processFriendRequest(this.fromId.toString(),
                    "")
            when (r.type) {
                10 -> this.accept()
                20 -> this.reject()
            }
        }
    }

    override suspend fun getGroupName(id: String): String {
        return bot.getGroup(id.toLong()).name
    }

    override suspend fun getMemberName(source: String, target: String): String {
        if (source.isEmpty()) {
            return bot.getFriend(target.toLong()).nick
        }
        return bot.getGroup(source.toLong())[target.toLong()].nameCardOrNick
    }

    override suspend fun getSelfID(): String {
        return bot.selfQQ.id.toString()
    }

    override suspend fun getUserAvatar(id: String): String {
        return "http://q1.qlogo.cn/g?b=qq&nk=${id}&s=640"
    }

    override suspend fun getUserName(id: String): String {
        // stupid but useful
        val idLong = id.toLong()
        for (group in bot.groups) {
            val member = group.getOrNull(idLong)
            if (member != null) {
                return member.nick
            }
        }
        return bot.getFriend(idLong).nick
    }

    override suspend fun removeMember(source: String, target: String) {
        return bot.getGroup(source.toLong())[target.toLong()].kick()
    }

    override suspend fun sendChatMessage(type: Int, source: String, target: String, message: String) {
        val contact = when (type) {
            ChatMessageType.Group ->
                bot.getGroup(source.toLong())
            ChatMessageType.Private ->
                bot.getFriend(target.toLong())
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
                        val member = bot.getGroupOrNull(source.toLong())?.getOrNull(it.data.toLong())
                        if (member != null) At(member) else PlainText("@无效")
                    }
                }
                "face" -> Face(it.data.toInt())
                "image_online" -> HttpClient().use { client ->
                    contact.uploadImage(client.get<InputStream>(it.data))
                }
                else -> PlainText("不支持的消息")
            }
        }
        contact.sendMessage(miraiMsgBuilder.build())
    }

    override suspend fun shutupAllMember(source: String, switch: Boolean) {
        bot.getGroup(source.toLong()).settings.isMuteAll = switch
    }

    override suspend fun shutupMember(source: String, target: String, duration: Int) {
        if (duration == 0) {
            bot.getGroup(source.toLong())[target.toLong()].unmute()
        } else {
            bot.getGroup(source.toLong())[target.toLong()].mute(duration * 60)
        }
    }

    override suspend fun getGroupList(): Array<String> {
        return bot.groups.map {
            it.id.toString()
        }.toTypedArray()
    }

    override suspend fun getMemberList(id: String): Array<String> {
        return bot.getGroup(id.toLong()).members.map {
            it.id.toString()
        }.toTypedArray()
    }

    override suspend fun getPlatformID(): String {
        return "QQ"
    }

    private suspend fun toUBotMessage(msg: Message): String {
        val builder = ChatMessageBuilder()
        for (it in msg.flatten()) {
            when (it) {
                is PlainText -> builder.add(it.content)
                is At -> builder.add("at", it.target.toString())
                is AtAll -> builder.add("at", "all")
                is Face -> builder.add("face", it.id.toString())
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
            var appFolder = File(MiraiAccount::class.java.protectionDomain.codeSource.location.toURI())
            if (appFolder.isFile) {
                appFolder = appFolder.parentFile
            }
            val bot = QQAndroid.Bot(args[2].toLong(), args[3], BotConfiguration().apply {
                fileBasedDeviceInfo(File(appFolder, "mirai.${args[2]}.device.json").absolutePath)
            })
            bot.login()
            UBotClientHost.hostAccount(args[0], args[1], "QQ${bot.selfQQ.id}") { event ->
                MiraiAccount(event, bot)
            }
            bot.closeAndJoin()
        } catch (e: Exception) {
            println("Error occurred")
            e.printStackTrace()
        }
        println("Session ended")
        exitProcess(0)
    }
}