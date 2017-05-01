package org.abimon.butlerBot;

import com.github.xaanit.d4j.oauth.Scope;
import com.github.xaanit.d4j.oauth.util.DiscordOAuthBuilder;
import io.vertx.core.http.HttpServerOptions;
import sx.blah.discord.api.ClientBuilder;

public class Main {
    public static void main(String[] args) {
        ButlerBot.client = new ClientBuilder().withToken(args[0]).registerListener(new ButlerBot()).login();
        ButlerBot.oauth = new DiscordOAuthBuilder(ButlerBot.client)
                .withClientID(args[1])
                .withClientSecret(args[2])
                .withRedirectUrl("http://104.199.6.227:8080/callback")
                .withHttpServerOptions(new HttpServerOptions().setPort(8080))
                .withScopes(Scope.IDENTIFY, Scope.CONNECTIONS)
                .build();
    }
}
