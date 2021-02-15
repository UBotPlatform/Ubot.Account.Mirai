package ubot.account.mirai
import io.ktor.client.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.Bot
import net.mamoe.mirai.BotFactory.INSTANCE.newBot
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsVoice
import net.mamoe.mirai.utils.MiraiInternalApi
import net.mamoe.mirai.utils.MiraiLogger
import net.mamoe.mirai.utils.PlatformLogger
import org.fusesource.jansi.AnsiConsole
import ubot.common.*
import java.io.File
import java.io.InputStream
import java.util.*
import kotlin.system.exitProcess

class MiraiAccount(private val event: UBotAccountEventEmitter,
                   private val bot: Bot)
    : BaseUBotAccount() {
    init {
        val e = bot.eventChannel
        e.subscribeAlways<MemberJoinEvent> {
            event.onMemberJoined(this.group.id.toString(), this.member.id.toString(), "")
        }
        e.subscribeAlways<MemberLeaveEvent> {
            event.onMemberLeft(this.group.id.toString(), this.member.id.toString())
        }
        e.subscribeAlways<FriendMessageEvent> {
            event.onReceiveChatMessage(ChatMessageType.Private,
                    "",
                    this.sender.id.toString(),
                    toUBotMessage(this.message),
                    ChatMessageInfo())
        }
        e.subscribeAlways<GroupMessageEvent> {
            event.onReceiveChatMessage(ChatMessageType.Group,
                    this.group.id.toString(),
                    this.sender.id.toString(),
                    toUBotMessage(this.message),
                    ChatMessageInfo())
        }
        e.subscribeAlways<BotInvitedJoinGroupRequestEvent> {
            val r = event.processGroupInvitation(this.invitorId.toString(),
                    this.groupId.toString(),
                    "")
            when (r.type) {
                10 -> this.accept()
                20 -> this.ignore()
            }
        }
        e.subscribeAlways<MemberJoinRequestEvent> {
            val r = event.processMembershipRequest(this.groupId.toString(),
                    this.fromId.toString(),
                    "",
                    this.message)
            when (r.type) {
                10 -> this.accept()
                20 -> this.reject(message = r.reason ?: "")
            }
        }
        e.subscribeAlways<NewFriendRequestEvent> {
            val r = event.processFriendRequest(this.fromId.toString(),
                    "")
            when (r.type) {
                10 -> this.accept()
                20 -> this.reject()
            }
        }
    }

    override suspend fun getGroupName(id: String): String {
        return bot.getGroupOrFail(id.toLong()).name
    }

    override suspend fun getMemberName(source: String, target: String): String {
        if (source.isEmpty()) {
            return bot.getFriendOrFail(target.toLong()).nick
        }
        return bot.getGroupOrFail(source.toLong())[target.toLong()]!!.nameCardOrNick
    }

    override suspend fun getSelfID(): String {
        return bot.id.toString()
    }

    override suspend fun getUserAvatar(id: String): String {
        return "http://q1.qlogo.cn/g?b=qq&nk=${id}&s=640"
    }

    override suspend fun getUserName(id: String): String {
        // stupid but useful
        val idLong = id.toLong()
        bot.getFriend(idLong)?.apply {
            return nick
        }
        bot.groups.forEach {
            it[idLong]?.apply {
                return nick
            }
        }
        return "unknown"
    }

    override suspend fun removeMember(source: String, target: String) {
        bot.getGroupOrFail(source.toLong()).getOrFail(target.toLong()).kick("")
    }

    override suspend fun sendChatMessage(type: Int, source: String, target: String, message: String) {
        val contact = when (type) {
            ChatMessageType.Group ->
                bot.getGroupOrFail(source.toLong())
            ChatMessageType.Private ->
                bot.getFriendOrFail(target.toLong())
            else ->
                throw IllegalArgumentException("invalid type")
        }
        val parsed = ChatMessageParser.Parse(message)
        val miraiMsgBuilder = MessageChainBuilder()
        for (entity in parsed) {
            miraiMsgBuilder += when (entity.type) {
                "text" -> PlainText(entity.data)
                "at" -> {
                    if (entity.data == "all")
                        AtAll
                    else {
                        val member = bot.getGroup(source.toLong())?.get(entity.data.toLong())
                        if (member != null) At(member) else PlainText("@无效")
                    }
                }
                "face" -> Face(entity.data.toInt())
                "image_online" -> HttpClient().use { client ->
                    client.get<InputStream>(entity.data).toExternalResource().use {
                        it.uploadAsImage(contact)
                    }
                }
                "image_base64" -> Base64.getDecoder().wrap(
                        entity.data.byteInputStream()
                ).toExternalResource().use {
                    it.uploadAsImage(contact)
                }
                "voice_online" -> HttpClient().use { client ->
                    client.get<InputStream>(entity.data).toExternalResource().use {
                        it.uploadAsVoice(contact)
                    }
                }
                "voice_base64" -> Base64.getDecoder().wrap(
                        entity.data.byteInputStream()
                ).toExternalResource().use {
                    it.uploadAsVoice(contact)
                }
                else -> PlainText("不支持的消息")
            }
        }
        contact.sendMessage(miraiMsgBuilder.build())
    }

    override suspend fun shutupAllMember(source: String, switch: Boolean) {
        bot.getGroupOrFail(source.toLong()).settings.isMuteAll = switch
    }

    override suspend fun shutupMember(source: String, target: String, duration: Int) {
        if (duration == 0) {
            bot.getGroupOrFail(source.toLong()).getOrFail(target.toLong()).unmute()
        } else {
            bot.getGroupOrFail(source.toLong()).getOrFail(target.toLong()).mute(duration * 60)
        }
    }

    override suspend fun getGroupList(): Array<String> {
        return bot.groups.map {
            it.id.toString()
        }.toTypedArray()
    }

    override suspend fun getMemberList(id: String): Array<String> {
        return bot.getGroupOrFail(id.toLong()).members.map {
            it.id.toString()
        }.toTypedArray()
    }

    override suspend fun getPlatformID(): String {
        return "QQ"
    }

    private suspend fun toUBotMessage(msg: Message): String {
        val builder = ChatMessageBuilder()
        for (it in msg.toMessageChain()) {
            when (it) {
                is PlainText -> builder.add(it.content)
                is At -> builder.add("at", it.target.toString())
                is AtAll -> builder.add("at", "all")
                is Face -> builder.add("face", it.id.toString())
                is Image -> builder.add("image_online", it.queryUrl())
                is Voice -> builder.add("voice_online", it.url ?: "")
                is MessageMetadata -> {
                }
                else -> builder.add(it.contentToString())
            }
        }
        return builder.build()
    }
}

@MiraiInternalApi
fun main(args: Array<String>) {
    MiraiLogger.setDefaultLoggerCreator { identity ->
        PlatformLogger(identity, AnsiConsole.out::println, true)
    }
    runBlocking<Unit> {
        try {
            var appFolder = File(MiraiAccount::class.java.protectionDomain.codeSource.location.toURI())
            if (appFolder.isFile) {
                appFolder = appFolder.parentFile
            }
            val instanceFolder = File(appFolder, "Mirai${args[2]}")
            instanceFolder.mkdir()
            assert(instanceFolder.isDirectory) {
                "Failed to create instance folder for ${args[2]}"
            }
            val bot = newBot(args[2].toLong(), args[3]) {
                workingDir = instanceFolder
                fileBasedDeviceInfo()
                enableContactCache()
            }
            bot.login()
            UBotClientHost.hostAccount(args[0], args[1], "QQ${bot.id}") { event ->
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