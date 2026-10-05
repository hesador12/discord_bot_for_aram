// import net.dv8tion.jda.api.EmbedBuilder;
// import net.dv8tion.jda.api.JDABuilder;
// import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
// import net.dv8tion.jda.api.hooks.ListenerAdapter;
// import net.dv8tion.jda.api.requests.GatewayIntent;

// import com.sun.net.httpserver.HttpServer;
// import io.github.cdimascio.dotenv.Dotenv;

// import java.awt.Color;
// import java.net.InetSocketAddress;
// import java.sql.*;
// import java.text.NumberFormat;
// import java.time.Instant;
// import java.util.Locale;

// public class DiscordBot extends ListenerAdapter {

//     private static final String DB_URL = "jdbc:sqlite:settlement.db";
//     private static final String[] DEFAULT_MEMBERS = {"김도원", "임정규", "이선규", "안장현", "박민화", "이지호", "박준현"};

    
//     public static void main(String[] args) throws Exception {

//         //  render 상태 점검용 웹서버 임시 추가
//         int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
//         HttpServer server = HttpServer.create(new InetSocketAddress(port),0);
//         server.createContext("/", exchange -> {
//             String response = "Discord Bot is Online:)";
//             exchange.sendResponseHeaders(200, response.length());
//             exchange.getResponseBody().write(response.getBytes());
//             exchange.getResponseBody().close();
//         });
//         server.start();
//         System.out.println("Web server running on port"+ port);

//         //.env 파일을 못찾아도 ignore
//         Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
//         String discordToken = System.getenv("DISCORD_TOKEN");
        
//         if(discordToken == null || discordToken.isEmpty()) {
//             discordToken = dotenv.get("DISCORD_TOKEN");
//         }

//         if(discordToken == null || discordToken.isEmpty()) {
//             System.err.println("☠️ ERROR : DISCORD_TOKEN 이 설정되지 않았습니다. .env 파일 또는 환경변수를 확인하세요");
//             return;
//         }

//         // DB초기화 및 JDA 봇 실행
//           initDatabase();

//         JDABuilder.createDefault(discordToken)
//                 .enableIntents(GatewayIntent.MESSAGE_CONTENT)
//                 .addEventListeners(new DiscordBot())
//                 .build();

//         System.out.println("🤖 롤 딜량 정산 봇이 성공적으로 실행되었습니다!");
//     }

//      // ----------------------------------------------------
//     // SQLite 데이터베이스 및 고정 멤버 초기화
//     // ----------------------------------------------------
//     private static void initDatabase() {
//         String createTableSql = "CREATE TABLE IF NOT EXISTS settlement (" +
//                                 " name TEXT PRIMARY KEY," +
//                                 " amount INTEGER DEFAULT 0" +
//                                 ");";

//         String insertDefaultSql = "INSERT OR IGNORE INTO settlement (name, amount) VALUES (?, 0);";

//         try (Connection conn = DriverManager.getConnection(DB_URL);
//              Statement stmt = conn.createStatement();
//              PreparedStatement pstmt = conn.prepareStatement(insertDefaultSql)) {

//             stmt.execute(createTableSql);

//             // 고정 인원 등록 (이미 존재하면 금액 변경 없이 유지)
//             for (String member : DEFAULT_MEMBERS) {
//                 pstmt.setString(1, member);
//                 pstmt.executeUpdate();
//             }

//         } catch (SQLException e) {
//             System.err.println("❌ DB 초기화 실패: " + e.getMessage());
//         }
//     }

//     @Override
//     public void onMessageReceived(MessageReceivedEvent event) {
//         if (event.getAuthor().isBot()) return;

//         String message = event.getMessage().getContentRaw().trim();

//         if (message.equals("!도움말")) {
//             sendHelpMessage(event);
//         } else if (message.startsWith("!정산 ")) {
//             handleSettle(event, message);
//         } else if (message.startsWith("!송금 ")) {
//             handleTransfer(event, message);
//         } else if (message.startsWith("!완료 ")) {
//             handleComplete(event, message);
//         } else if (message.startsWith("!등록 ")) {
//             handleRegister(event, message);
//         } else if (message.startsWith("!삭제 ")) {
//             handleDelete(event, message);
//         } else if (message.equals("!잔액")) {
//             handleBalance(event);
//         } else if (message.equals("!초기화")) {
//             handleReset(event);
//         }
//     }

//     // ----------------------------------------------------
//     // !도움말
//     // ----------------------------------------------------
//     private void sendHelpMessage(MessageReceivedEvent event) {
//         EmbedBuilder embed = new EmbedBuilder();
//         embed.setTitle("📜 롤 딜량 정산 봇 사용 안내");
//         embed.setColor(Color.PINK);

//         embed.addField("🤍`!정산 | [1등] [꼴등] [금액]`", "게임 결과를 기록하고 꼴등의 빚을 누적합니다.\n*예) !정산 김도원 임정규 5000*", false);
//         embed.addField("🤍`!송금 | [이름] [금액]`", "송금받은 금액만큼 빚을 차감합니다.\n*예) !송금 임정규 3000*", false);
//         embed.addField("🤍`!완료 | [이름]`", "해당 인원의 남은 정산액을 0원으로 완납 처리합니다.", false);
//         embed.addField("🤍`!잔액`", "모든 등록된 인원의 잔액 현황을 확인합니다.", false);
//         embed.addField("🤍`!등록 | [이름]`", "정산 명단에 새로운 인원을 추가합니다.", false);
//         embed.addField("🤍`!삭제 | [이름]`", "정산 명단에서 특정 인원을 삭제합니다.", false);
//         embed.addField("🤍`!초기화`", "모든 인원의 정산 금액을 0원으로 초기화합니다.", false);

//         event.getChannel().sendMessageEmbeds(embed.build()).queue();
//     }
// // ----------------------------------------------------
//     // !정산 [1등] [꼴등] [금액] (등록 여부 검증 추가)
//     // ----------------------------------------------------
//     private void handleSettle(MessageReceivedEvent event, String message) {
//         String[] parts = message.split("\\s+");
//         if (parts.length < 4) {
//             sendErrorEmbed(event, "⚠️ 올바른 형식으로 입력해 주세요.\n사용법: `!정산 [1등] [꼴등] [금액]`");
//             return;
//         }

//         String winner = parts[1];
//         String loser = parts[2];
//         int amount;

//         try {
//             amount = Integer.parseInt(parts[3]);
//             if (amount <= 0) {
//                 sendErrorEmbed(event, "⚠️ 금액은 0보다 큰 정수여야 합니다.");
//                 return;
//             }
//         } catch (NumberFormatException e) {
//             sendErrorEmbed(event, "⚠️ 금액은 숫자로만 입력해 주세요.");
//             return;
//         }

//         try (Connection conn = DriverManager.getConnection(DB_URL)) {

//             // 1. [검증] 꼴등(빚을 질 사람)이 등록된 인원인지 확인
//             if (!isUserRegistered(conn, loser)) {
//                 sendErrorEmbed(event, "⚠️ **`" + loser + "`**님은 등록되지 않은 인원입니다.\n`!등록 " + loser + "` 명령어로 먼저 등록해 주세요.");
//                 return;
//             }

//             // 2. [검증] 1등도 등록된 인원인지 확인 (선택 사항이지만 안전을 위해 검증)
//             if (!isUserRegistered(conn, winner)) {
//                 sendErrorEmbed(event, "⚠️️ **`" + winner + "`**님은 등록되지 않은 인원입니다.\n`!등록 " + winner + "` 명령어로 먼저 등록해 주세요.");
//                 return;
//             }

//             // 3. 등록된 인원임이 확인되면 금액 업데이트 (UPDATE 전용 구문 사용)
//           String updateWinnerSql = "UPDATE settlement SET amount = amount + ? WHERE name = ?;";
//             String updateLoserSql = "UPDATE settlement SET amount = amount - ? WHERE name = ?;";

//             try (PreparedStatement pstmt1 = conn.prepareStatement(updateWinnerSql);
//                  PreparedStatement pstmt2 = conn.prepareStatement(updateLoserSql)) {
                
//                 pstmt1.setInt(1, amount);
//                 pstmt1.setString(2, winner);
//                 pstmt1.executeUpdate();

//                 pstmt2.setInt(1, amount);
//                 pstmt2.setString(2, loser);
//                 pstmt2.executeUpdate();
//             }

//             EmbedBuilder embed = new EmbedBuilder();
//             embed.setTitle("🎲 칼바람 딜량 내기 정산 완료!");
//             embed.setColor(Color.GREEN);
//             embed.addField("🏆 1등 (+ " + formatMoney(amount) + "원)", winner, true);
//             embed.addField("💀 꼴등 (- " + formatMoney(amount) + "원)", loser, true);
//             embed.addField("💸 내역", loser + " ➡️ " + winner + " 한테 **" + formatMoney(amount) + "원** 돈보내", false);

//             event.getChannel().sendMessageEmbeds(embed.build()).queue();

//         } catch (SQLException e) {
//             sendErrorEmbed(event, "❌ 정산 기록 중 DB 오류가 발생했습니다.");
//             e.printStackTrace();
//         }
//     }

//     // ----------------------------------------------------
//     // [헬퍼] 유저가 DB에 등록되어 있는지 확인하는 메소드
//     // ----------------------------------------------------
//     private boolean isUserRegistered(Connection conn, String name) throws SQLException {
//         String sql = "SELECT COUNT(*) FROM settlement WHERE name = ?;";
//         try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
//             pstmt.setString(1, name);
//             try (ResultSet rs = pstmt.executeQuery()) {
//                 if (rs.next()) {
//                     return rs.getInt(1) > 0;
//                 }
//             }
//         }
//         return false;
//     }

//     // ----------------------------------------------------
//     // !송금 [이름] [금액]
//     // ----------------------------------------------------
//     private void handleTransfer(MessageReceivedEvent event, String message) {
//         String[] parts = message.split("\\s+");
//         if (parts.length < 3) {
//             sendErrorEmbed(event, "⚠️ 올바른 형식으로 입력해 주세요.\n사용법: `!송금 [이름] [금액]`");
//             return;
//         }

//         String name = parts[1];
//         int payAmount;

//         try {
//             payAmount = Integer.parseInt(parts[2]);
//             if (payAmount <= 0) {
//                 sendErrorEmbed(event, "⚠️ 송금 금액은 0보다 커야 합니다.");
//                 return;
//             }
//         } catch (NumberFormatException e) {
//             sendErrorEmbed(event, "⚠️ 금액은 숫자로만 입력해 주세요.");
//             return;
//         }

//         String sql = "UPDATE settlement SET amount = amount - ? WHERE name = ?;";

//         try (Connection conn = DriverManager.getConnection(DB_URL);
//              PreparedStatement pstmt = conn.prepareStatement(sql)) {

//             pstmt.setInt(1, payAmount);
//             pstmt.setString(2, name);
//             int updatedRows = pstmt.executeUpdate();

//             if (updatedRows == 0) {
//                 sendErrorEmbed(event, "⚠️ `" + name + "`님의 정산 기록이 존재하지 않습니다.");
//             } else {
//                 int currentAmount = getAmount(conn, name);

//                 EmbedBuilder embed = new EmbedBuilder();
//                 embed.setTitle("💸 송금 확인 완료!");
//                 embed.setColor(Color.CYAN);
//                 embed.addField("👤 이름", name, true);
//                 embed.addField("💰 송금 금액", formatMoney(payAmount) + "원", true);
//                 embed.addField("📉 남은 잔액", formatMoney(currentAmount) + "원", false);

//                 event.getChannel().sendMessageEmbeds(embed.build()).queue();
//             }

//         } catch (SQLException e) {
//             sendErrorEmbed(event, "❌ 송금 처리 중 오류가 발생했습니다.");
//             e.printStackTrace();
//         }
//     }

//     // ----------------------------------------------------
//     // !완료 [이름]
//     // ----------------------------------------------------
//     private void handleComplete(MessageReceivedEvent event, String message) {
//         String[] parts = message.split("\\s+");
//         if (parts.length < 2) {
//             sendErrorEmbed(event, "⚠️ 올바른 형식으로 입력해 주세요.\n사용법: `!완료 [이름]`");
//             return;
//         }

//         String name = parts[1];
//         String sql = "UPDATE settlement SET amount = 0 WHERE name = ?;";

//         try (Connection conn = DriverManager.getConnection(DB_URL);
//              PreparedStatement pstmt = conn.prepareStatement(sql)) {

//             pstmt.setString(1, name);
//             int updatedRows = pstmt.executeUpdate();

//             if (updatedRows == 0) {
//                 sendErrorEmbed(event, "⚠️ `" + name + "`님의 정산 기록이 존재하지 않습니다.");
//             } else {
//                 EmbedBuilder embed = new EmbedBuilder();
//                 embed.setTitle("✅ 완납 처리 완료!");
//                 embed.setColor(Color.GREEN);
//                 embed.setDescription("**`" + name + "`**님의 남은 정산액을 **0원**으로 완납 처리했습니다!");

//                 event.getChannel().sendMessageEmbeds(embed.build()).queue();
//             }

//         } catch (SQLException e) {
//             sendErrorEmbed(event, "❌ 완납 처리 중 오류가 발생했습니다.");
//             e.printStackTrace();
//         }
//     }

//     // ----------------------------------------------------
//     // !등록 [이름] (신규 인원 등록)
//     // ----------------------------------------------------
//     private void handleRegister(MessageReceivedEvent event, String message) {
//         String[] parts = message.split("\\s+");
//         if (parts.length < 2) {
//             sendErrorEmbed(event, "⚠️ 등록할 이름을 입력해 주세요.\n사용법: `!등록 [이름]`");
//             return;
//         }

//         String name = parts[1];
//         String sql = "INSERT INTO settlement (name, amount) VALUES (?, 0);";

//         try (Connection conn = DriverManager.getConnection(DB_URL);
//              PreparedStatement pstmt = conn.prepareStatement(sql)) {

//             pstmt.setString(1, name);
//             pstmt.executeUpdate();

//             EmbedBuilder embed = new EmbedBuilder();
//             embed.setTitle("👤 멤버 등록 완료");
//             embed.setColor(Color.BLUE);
//             embed.setDescription("**`" + name + "`**님이 정산 명단에 추가되었습니다 (기본 잔액: 0원).");

//             event.getChannel().sendMessageEmbeds(embed.build()).queue();

//         } catch (SQLException e) {
//             sendErrorEmbed(event, "⚠️ 이미 등록되어 있는 이름이거나 등록 실패했습니다.");
//         }
//     }

//     // ----------------------------------------------------
//     // !삭제 [이름] (인원 삭제)
//     // ----------------------------------------------------
//     private void handleDelete(MessageReceivedEvent event, String message) {
//         String[] parts = message.split("\\s+");
//         if (parts.length < 2) {
//             sendErrorEmbed(event, "⚠️ 삭제할 이름을 입력해 주세요.\n사용법: `!삭제 [이름]`");
//             return;
//         }

//         String name = parts[1];
//         String sql = "DELETE FROM settlement WHERE name = ?;";

//         try (Connection conn = DriverManager.getConnection(DB_URL);
//              PreparedStatement pstmt = conn.prepareStatement(sql)) {

//             pstmt.setString(1, name);
//             int updatedRows = pstmt.executeUpdate();

//             if (updatedRows == 0) {
//                 sendErrorEmbed(event, "⚠️ `" + name + "`님은 등록되어 있지 않습니다.");
//             } else {
//                 EmbedBuilder embed = new EmbedBuilder();
//                 embed.setTitle("🗑️ 멤버 삭제 완료");
//                 embed.setColor(Color.DARK_GRAY);
//                 embed.setDescription("**`" + name + "`**님을 정산 명단에서 삭제했습니다.");

//                 event.getChannel().sendMessageEmbeds(embed.build()).queue();
//             }

//         } catch (SQLException e) {
//             sendErrorEmbed(event, "❌ 멤버 삭제 중 오류가 발생했습니다.");
//             e.printStackTrace();
//         }
//     }

//     // ----------------------------------------------------
//     // !잔액 (0원인 인원도 모두 표시)
//     // ----------------------------------------------------
//     private void handleBalance(MessageReceivedEvent event) {
//         // 모든 인원 출력 (금액 내림차순, 동일 금액 시 이름 순)
//         String sql = "SELECT name, amount FROM settlement ORDER BY amount DESC, name ASC;";

//         try (Connection conn = DriverManager.getConnection(DB_URL);
//              Statement stmt = conn.createStatement();
//              ResultSet rs = stmt.executeQuery(sql)) {

//             EmbedBuilder embed = new EmbedBuilder();
//             embed.setTitle("📊 잔액 현황");
//             embed.setColor(Color.ORANGE);

//             boolean hasData = false;
//             StringBuilder sb = new StringBuilder();

//             while (rs.next()) {
//                 hasData = true;
//                 String name = rs.getString("name");
//                 int amount = rs.getInt("amount");

//                 if (amount >= 0) {
//                     sb.append(String.format("• **%s**: `%s원` 🤑\n", name, formatMoney(amount)));
//                 } else {
//                     sb.append(String.format("• **%s**: `%s원` ☠️\n", name, formatMoney(amount)));
//                 }
//             }

//             if (!hasData) {
//                 embed.setDescription("등록된 멤버가 없습니다. `!등록 [이름]` 명령어로 추가해 보세요.");
//             } else {
//                 embed.setDescription(sb.toString());
//             }

//             event.getChannel().sendMessageEmbeds(embed.build()).queue();

//         } catch (SQLException e) {
//             sendErrorEmbed(event, "❌ 잔액 조회 중 오류가 발생했습니다.");
//             e.printStackTrace();
//         }
//     }

//     // ----------------------------------------------------
//     // !초기화 (모든 금액을 0원으로 변경)
//     // ----------------------------------------------------
//     private void handleReset(MessageReceivedEvent event) {
//         String sql = "UPDATE settlement SET amount = 0;";

//         try (Connection conn = DriverManager.getConnection(DB_URL);
//              Statement stmt = conn.createStatement()) {

//             stmt.executeUpdate(sql);

//             EmbedBuilder embed = new EmbedBuilder();
//             embed.setTitle("🧹 정산 잔액 초기화");
//             embed.setColor(Color.RED);
//             embed.setDescription("모든 인원의 정산 금액이 **0원**으로 리셋되었습니다.");

//             event.getChannel().sendMessageEmbeds(embed.build()).queue();

//         } catch (SQLException e) {
//             sendErrorEmbed(event, "❌ 초기화 중 오류가 발생했습니다.");
//             e.printStackTrace();
//         }
//     }

//     // ----------------------------------------------------
//     // 헬퍼 메소드
//     // ----------------------------------------------------
//     private void sendErrorEmbed(MessageReceivedEvent event, String description) {
//         EmbedBuilder embed = new EmbedBuilder();
//         embed.setColor(Color.RED);
//         embed.setDescription(description);
//         event.getChannel().sendMessageEmbeds(embed.build()).queue();
//     }

//     private int getAmount(Connection conn, String name) throws SQLException {
//         String sql = "SELECT amount FROM settlement WHERE name = ?;";
//         try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
//             pstmt.setString(1, name);
//             try (ResultSet rs = pstmt.executeQuery()) {
//                 if (rs.next()) {
//                     return rs.getInt("amount");
//                 }
//             }
//         }
//         return 0;
//     }

//     private String formatMoney(int money) {
//         return NumberFormat.getNumberInstance(Locale.KOREA).format(money);
//     }

// }