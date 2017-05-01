package org.abimon.butlerBot;

import com.github.xaanit.d4j.oauth.handle.IConnection;
import com.github.xaanit.d4j.oauth.handle.IDiscordOAuth;
import com.github.xaanit.d4j.oauth.handle.IOAuthUser;
import com.github.xaanit.d4j.oauth.util.MissingScopeException;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.Event;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.BotInviteBuilder;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.RequestBuffer;

import java.util.function.Consumer;

public class ButlerBot implements IListener<Event> {
	public static IDiscordClient client;
	public static IDiscordOAuth oauth;

	@Override
	public void handle(Event event) {
		if (event instanceof ReadyEvent) {
			System.out.println("Hello sire, invite me into your home here: " + new BotInviteBuilder(client).build());
			client.changePlayingText("butler to " + client.getUsers().size() + " users.");
		} else if (event instanceof MessageReceivedEvent) {
			MessageReceivedEvent messageEvent = (MessageReceivedEvent) event;
			IMessage msg = messageEvent.getMessage();
			String order = msg.getContent();
			if (order.equalsIgnoreCase("Butler, do you know who I am?")) {
				performAuthorisedAction(msg.getAuthor(), msg.getChannel(), user -> msg.getChannel().sendMessage(new EmbedBuilder()
						.withAuthorName("Butler")
						.withThumbnail(client.getOurUser().getAvatarURL())
						.withDesc("Of course, sire. You are " + user.getName() + "#" + user.getDiscriminator() + ", and you have elected " + (user.is2FAEnabled() ? "to increase your personal security." : " to live dangerously. Please be careful, my lord."))
						.build()));
			} else if (order.equalsIgnoreCase("Butler, I authorise you")) {
				RequestBuffer.request(() -> msg.getChannel().sendMessage(new EmbedBuilder()
						.withAuthorName("Butler")
						.withThumbnail(client.getOurUser().getAvatarURL())
						.withDesc("Very kind of you, master. Now if you could please fill out [this form here](" + oauth.buildAuthUrl() + "), this will allow me to work on your behalf.")
						.build()));
			} else if (order.equalsIgnoreCase("Butler, show me my connections")) {
				performAuthorisedAction(msg.getAuthor(), msg.getChannel(), user -> {
					EmbedBuilder builder = new EmbedBuilder()
							.withAuthorName("Butler")
							.withThumbnail(client.getOurUser().getAvatarURL())
							.withDesc("As you wish sire. Here are your connections.");

					user.getConnections().stream().filter(IConnection::isVisible).forEach(connection -> builder.appendField(connection.getType(), connection.getName(), true));
					msg.getChannel().sendMessage(builder.build());
				});
			} else if (order.equalsIgnoreCase("Butler, what have I allowed you to do?")) {
				performAuthorisedAction(msg.getAuthor(), msg.getChannel(), user -> {
					EmbedBuilder builder = new EmbedBuilder()
							.withAuthorName("Butler")
							.withThumbnail(client.getOurUser().getAvatarURL())
							.withDesc("As your butler, you have allowed me to perform the following actions.")
							.appendField("\u200B", "\u200B", false);

					user.getAuthorizedScopes().forEach(scope -> {
						switch (scope) {
							case IDENTIFY:
								builder.appendField("\u200B", "You have allowed me to identify who you are, as well as your security detail", false);
								return;
							case CONNECTIONS:
								builder.appendField("\u200B", "You have allowed me to identify your connections with others.", false);
								return;
						}

						builder.appendField("\u200B", "There is something I am allowed to do that I do not know how to do.", false);
					});

					msg.getChannel().sendMessage(builder.build());
				});
			}
		}
	}

	public static void performAuthorisedAction(IUser original, IChannel response, Consumer<IOAuthUser> consumer) {
		IOAuthUser user = oauth.getOAuthUser(original);
		if (user == null)
			RequestBuffer.request(() -> response.sendMessage(new EmbedBuilder()
					.withAuthorName("Butler")
					.withThumbnail(client.getOurUser().getAvatarURL())
					.withDesc("Apologies, sire, I do not know who you are. To become acquainted with me, however, please reply with `Butler, I authorise you`.")
					.build()));
		else
			RequestBuffer.request(() -> {
				try {
					consumer.accept(user);
				} catch (MissingScopeException e) {
					response.sendMessage(new EmbedBuilder()
							.withAuthorName("Butler")
							.withThumbnail(client.getOurUser().getAvatarURL())
							.withDesc("I apologise, sire, but I do not seem to be authorised to do that. I will need the following permissions from you: " + e.getMissingScope().getName())
							.build());
				} catch (DiscordException e) {
					response.sendMessage(new EmbedBuilder()
							.withAuthorName("Butler")
							.withThumbnail(client.getOurUser().getAvatarURL())
							.withDesc("Apologies, sire, but I seem to have run into a problem while trying to do that: ```\n" + e.getErrorMessage() + "``` Please try again later if you so desire.")
							.build());
				}
			});
	}
}
