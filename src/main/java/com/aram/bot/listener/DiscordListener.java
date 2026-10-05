package com.aram.bot.listener;

import com.aram.bot.domain.Member;
import com.aram.bot.domain.SettlementService;
import com.aram.bot.repository.MemberRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.Color;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

public class DiscordListener extends ListenerAdapter {

    private final SettlementService settlementService = new SettlementService();
    private final MemberRepository memberRepository = new MemberRepository();

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        // [디버깅] 디스코드에서 메시지를 받으면 콘솔에 출력
        System.out.println("📩 [메시지 수신] " + event.getAuthor().getName() + ": " + event.getMessage().getContentRaw());

        String message = event.getMessage().getContentRaw().trim();

        if (message.equals("!도움말")) {
            sendHelpMessage(event);
        } else if (message.startsWith("!정산 ")) {
            handleSettle(event, message);
        } else if (message.startsWith("!송금 ")) {
            handleTransfer(event, message);
        } else if (message.startsWith("!완료 ")) {
            handleComplete(event, message);
        } else if (message.startsWith("!등록 ")) {
            handleRegister(event, message);
        } else if (message.startsWith("!삭제 ")) {
            handleDelete(event, message);
        } else if (message.equals("!잔액")) {
            handleBalance(event);
        } else if (message.equals("!초기화")) {
            handleReset(event);
        }
    }

    // ----------------------------------------------------
    // !도움말
    // ----------------------------------------------------
    private void sendHelpMessage(MessageReceivedEvent event) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("📜 롤 딜량 정산 봇 사용 안내");
        embed.setColor(Color.PINK);

        embed.addField("🤍 `!정산 [1등] [꼴등] [금액]`", "1등은 (+금액), 꼴등은 (-금액)으로 장부에 기록됩니다.\n*예) !정산 김도원 임정규 5000*", false);
        embed.addField("🤍 `!송금 [이름] [금액]`", "송금하여 빚을 탕감합니다 (+금액 증가).\n*예) !송금 임정규 3000*", false);
        embed.addField("🤍 `!완료 [이름]`", "해당 인원의 남은 정산액을 0원으로 완납 처리합니다.", false);
        embed.addField("🤍 `!잔액`", "모든 인원의 번 돈(+)과 잃은 돈(-) 현황을 출력합니다.", false);
        embed.addField("🤍 `!등록 [이름]`", "정산 명단에 새로운 인원을 추가합니다.", false);
        embed.addField("🤍 `!삭제 [이름]`", "정산 명단에서 특정 인원을 삭제합니다.", false);
        embed.addField("🤍 `!초기화`", "모든 인원의 정산 금액을 0원으로 초기화합니다.", false);

        event.getChannel().sendMessageEmbeds(embed.build()).queue();
    }

    // ----------------------------------------------------
    // !정산 [1등] [꼴등] [금액]
    // ----------------------------------------------------
    private void handleSettle(MessageReceivedEvent event, String message) {
        String[] parts = message.split("\\s+");
        if (parts.length < 4) {
            sendErrorEmbed(event, "⚠️️ 올바른 형식으로 입력해 주세요.\n사용법: `!정산 [1등] [꼴등] [금액]`");
            return;
        }

        String winner = parts[1];
        String loser = parts[2];
        int amount;

        try {
            amount = Integer.parseInt(parts[3]);
            if (amount <= 0) {
                sendErrorEmbed(event, "⚠️️ 금액은 0보다 큰 정수여야 합니다.");
                return;
            }
        } catch (NumberFormatException e) {
            sendErrorEmbed(event, "⚠️ 금액은 숫자로만 입력해 주세요.");
            return;
        }

        try {
            settlementService.settle(winner, loser, amount);

            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("🎲 칼바람 딜량 내기 정산 완료!");
            embed.setColor(new Color(46, 204, 113));

            StringBuilder sb = new StringBuilder();
            sb.append("이번 게임 정산 결과가 장부에 저장되었습니다.\n\n");
            sb.append("───────────────────────\n");
            sb.append("🏆 **1등 (획득)** : `").append(winner).append("` ➔ **+").append(formatMoney(amount)).append("원**\n");
            sb.append("💀 **꼴등 (지출)** : `").append(loser).append("` ➔ **-").append(formatMoney(amount)).append("원**\n");
            sb.append("───────────────────────\n\n");
            sb.append("💸 **청구 내역**: `").append(loser).append("` ➡️ `").append(winner).append("` 에게 **").append(formatMoney(amount)).append("원** 송금 필요");

            embed.setDescription(sb.toString());
            embed.setFooter("!잔액 명령어로 누적 잔액을 확인해 보세요.");
            embed.setTimestamp(Instant.now());

            event.getChannel().sendMessageEmbeds(embed.build()).queue();

        } catch (IllegalArgumentException e) {
            sendErrorEmbed(event, "⚠️ " + e.getMessage());
        } catch (Exception e) {
            sendErrorEmbed(event, "❌ 정산 처리 중 오류가 발생했습니다.");
        }
    }

    // ----------------------------------------------------
    // !송금 [이름] [금액]
    // ----------------------------------------------------
    private void handleTransfer(MessageReceivedEvent event, String message) {
        String[] parts = message.split("\\s+");
        if (parts.length < 3) {
            sendErrorEmbed(event, "⚠️ 올바른 형식으로 입력해 주세요.\n사용법: `!송금 [이름] [금액]`");
            return;
        }

        String name = parts[1];
        int payAmount;

        try {
            payAmount = Integer.parseInt(parts[2]);
            if (payAmount <= 0) {
                sendErrorEmbed(event, "⚠️ 송금 금액은 0보다 커야 합니다.");
                return;
            }
        } catch (NumberFormatException e) {
            sendErrorEmbed(event, "⚠️ 금액은 숫자로만 입력해 주세요.");
            return;
        }

        try {
            int currentAmount = settlementService.transfer(name, payAmount);

            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("💸 송금 확인 완료!");
            embed.setColor(Color.CYAN);
            embed.addField("👤 이름", name, true);
            embed.addField("💰 송금액", "+" + formatMoney(payAmount) + "원", true);
            embed.addField("📉 변제 후 잔액", formatSignedMoney(currentAmount) + "원", false);

            event.getChannel().sendMessageEmbeds(embed.build()).queue();

        } catch (IllegalArgumentException e) {
            sendErrorEmbed(event, "⚠️ " + e.getMessage());
        } catch (Exception e) {
            sendErrorEmbed(event, "❌ 송금 처리 중 오류가 발생했습니다.");
        }
    }

    // ----------------------------------------------------
    // !완료 [이름]
    // ----------------------------------------------------
    private void handleComplete(MessageReceivedEvent event, String message) {
        String[] parts = message.split("\\s+");
        if (parts.length < 2) {
            sendErrorEmbed(event, "⚠️ 올바른 형식으로 입력해 주세요.\n사용법: `!완료 [이름]`");
            return;
        }

        String name = parts[1];

        try {
            settlementService.complete(name);

            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("✅ 완납 처리 완료!");
            embed.setColor(Color.GREEN);
            embed.setDescription("**`" + name + "`**님의 남은 정산액을 **0원**으로 완납 처리했습니다!");

            event.getChannel().sendMessageEmbeds(embed.build()).queue();

        } catch (IllegalArgumentException e) {
            sendErrorEmbed(event, "⚠️ " + e.getMessage());
        } catch (Exception e) {
            sendErrorEmbed(event, "❌ 완납 처리 중 오류가 발생했습니다.");
        }
    }

    // ----------------------------------------------------
    // !등록 [이름]
    // ----------------------------------------------------
    private void handleRegister(MessageReceivedEvent event, String message) {
        String[] parts = message.split("\\s+");
        if (parts.length < 2) {
            sendErrorEmbed(event, "⚠️ 등록할 이름을 입력해 주세요.\n사용법: `!등록 [이름]`");
            return;
        }

        String name = parts[1];

        try {
            settlementService.registerMember(name);

            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("👤 멤버 등록 완료");
            embed.setColor(Color.BLUE);
            embed.setDescription("**`" + name + "`**님이 정산 명단에 추가되었습니다 (기본 잔액: 0원).");

            event.getChannel().sendMessageEmbeds(embed.build()).queue();

        } catch (IllegalArgumentException e) {
            sendErrorEmbed(event, "⚠️️ " + e.getMessage());
        } catch (Exception e) {
            sendErrorEmbed(event, "❌ 멤버 등록 중 오류가 발생했습니다.");
        }
    }

    // ----------------------------------------------------
    // !삭제 [이름]
    // ----------------------------------------------------
    private void handleDelete(MessageReceivedEvent event, String message) {
        String[] parts = message.split("\\s+");
        if (parts.length < 2) {
            sendErrorEmbed(event, "⚠️ 삭제할 이름을 입력해 주세요.\n사용법: `!삭제 [이름]`");
            return;
        }

        String name = parts[1];

        try {
            settlementService.deleteMember(name);

            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("🗑️ 멤버 삭제 완료");
            embed.setColor(Color.DARK_GRAY);
            embed.setDescription("**`" + name + "`**님을 정산 명단에서 삭제했습니다.");

            event.getChannel().sendMessageEmbeds(embed.build()).queue();

        } catch (IllegalArgumentException e) {
            sendErrorEmbed(event, "⚠️ " + e.getMessage());
        } catch (Exception e) {
            sendErrorEmbed(event, "❌ 멤버 삭제 중 오류가 발생했습니다.");
        }
    }

    // ----------------------------------------------------
    // !잔액
    // ----------------------------------------------------
    private void handleBalance(MessageReceivedEvent event) {
        List<Member> members = memberRepository.findAll();

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("📊 멤버별 정산 잔액 현황");
        embed.setColor(Color.ORANGE);

        if (members.isEmpty()) {
            embed.setDescription("등록된 멤버가 없습니다. `!등록 [이름]` 명령어로 추가해 보세요.");
        } else {
            StringBuilder sb = new StringBuilder();
            for (Member member : members) {
                String name = member.getName();
                int amount = member.getAmount();

                if (amount > 0) {
                    sb.append(String.format("🟢 **%s**: `+%s원` (받을 돈 🤑)\n", name, formatMoney(amount)));
                } else if (amount < 0) {
                    sb.append(String.format("🔴 **%s**: `%s원` (보낼 돈 ☠️)\n", name, formatMoney(amount)));
                } else {
                    sb.append(String.format("⚪ **%s**: `0원` \n", name));
                }
            }
            embed.setDescription(sb.toString());
        }

        embed.setFooter("양수(+): 벌은 돈 / 음수(-): 잃은 돈");
        embed.setTimestamp(Instant.now());

        event.getChannel().sendMessageEmbeds(embed.build()).queue();
    }

    // ----------------------------------------------------
    // !초기화
    // ----------------------------------------------------
    private void handleReset(MessageReceivedEvent event) {
        try {
            settlementService.resetAll();

            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("🧹 정산 잔액 초기화");
            embed.setColor(Color.RED);
            embed.setDescription("모든 인원의 정산 금액이 **0원**으로 리셋되었습니다.");

            event.getChannel().sendMessageEmbeds(embed.build()).queue();

        } catch (Exception e) {
            sendErrorEmbed(event, "❌ 초기화 중 오류가 발생했습니다.");
        }
    }

    // ----------------------------------------------------
    // 헬퍼 메소드
    // ----------------------------------------------------
    private void sendErrorEmbed(MessageReceivedEvent event, String description) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(Color.RED);
        embed.setDescription(description);
        event.getChannel().sendMessageEmbeds(embed.build()).queue();
    }

    private String formatMoney(int money) {
        return NumberFormat.getNumberInstance(Locale.KOREA).format(money);
    }

    private String formatSignedMoney(int money) {
        String formatted = formatMoney(money);
        if (money > 0) return "+" + formatted;
        return formatted;
    }
}