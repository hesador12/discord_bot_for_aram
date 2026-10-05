package com.aram.bot;

import com.aram.bot.config.DatabaseConfig;
import com.aram.bot.listener.DiscordListener;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class Main {
    public static void main(String[] args) {
        try {
            System.setProperty("file.encoding", "UTF-8");

            // .env 파일 로드
            Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
            String token = System.getenv("DISCORD_TOKEN");
            if (token == null || token.isEmpty()) {
                token = dotenv.get("DISCORD_TOKEN");
            }

            if (token == null || token.isEmpty()) {
                System.err.println("❌ ERROR: DISCORD_TOKEN이 설정되지 않았습니다.");
                return;
            }

            // DB 초기화
            DatabaseConfig.initDatabase();

            // Web Server 구동
            WebServer.startServer();

            // JDA 봇 빌드 (EventListener 및 GatewayIntent 명시)
            JDA jda = JDABuilder.createDefault(token)
                    .enableIntents(
                            GatewayIntent.GUILD_MESSAGES,
                            GatewayIntent.DIRECT_MESSAGES,
                            GatewayIntent.MESSAGE_CONTENT
                    )
                    .addEventListeners(new DiscordListener())
                    .build();

            jda.awaitReady();
            System.out.println("✅ 디스코드 봇이 성공적으로 작동 중입니다!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}