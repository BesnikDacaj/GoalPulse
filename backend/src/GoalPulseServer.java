import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class GoalPulseServer {
    private static final int PORT = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
    private static final String TOKEN_SECRET = System.getenv().getOrDefault("GOALPULSE_TOKEN_SECRET", "dev-secret-change-before-production");
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final Store store = new Store();

    public static void main(String[] args) throws Exception {
        store.seed();
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", GoalPulseServer::route);
        server.setExecutor(null);
        server.start();
        System.out.println("GoalPulse running at http://localhost:" + PORT);
    }

    private static void route(HttpExchange ex) throws IOException {
        try {
            addCors(ex);
            if ("OPTIONS".equals(ex.getRequestMethod())) {
                send(ex, 204, "");
                return;
            }
            String path = ex.getRequestURI().getPath();
            if (path.startsWith("/api/v1/")) {
                handleApi(ex, path.substring("/api/v1".length()));
                return;
            }
            serveStatic(ex, path);
        } catch (HttpError err) {
            jsonError(ex, err.status, err.getMessage());
        } catch (Exception err) {
            err.printStackTrace();
            jsonError(ex, 500, "Server error: " + err.getMessage());
        }
    }

    private static void handleApi(HttpExchange ex, String path) throws Exception {
        String method = ex.getRequestMethod();
        if (method.equals("GET") && path.equals("/health")) json(ex, 200, map("status", "ok", "time", LocalDateTime.now().format(ISO)));
        else if (method.equals("GET") && path.equals("/assets/crest")) proxyCrest(ex);
        else if (method.equals("POST") && path.equals("/auth/login")) login(ex);
        else if (method.equals("POST") && path.equals("/auth/register")) register(ex);
        else if (method.equals("POST") && path.equals("/auth/refresh")) refresh(ex);
        else if (method.equals("GET") && path.equals("/auth/me")) me(ex);
        else if (method.equals("GET") && path.equals("/matches")) json(ex, 200, store.matchesJson());
        else if (method.equals("GET") && path.equals("/matches/live")) json(ex, 200, store.matchesByStatus("live"));
        else if (method.equals("GET") && path.matches("/matches/date/.+")) json(ex, 200, store.matchesForDate(path.substring("/matches/date/".length())));
        else if (method.equals("GET") && path.matches("/matches/\\d+/events")) json(ex, 200, store.eventsFor(idFrom(path, "/matches/")).stream().map(Event::json).toList());
        else if (method.equals("GET") && path.matches("/matches/\\d+/lineups")) json(ex, 200, store.lineupFor(idFrom(path, "/matches/")).stream().map(Player::json).toList());
        else if (method.equals("GET") && path.matches("/matches/\\d+")) matchDetail(ex, idFrom(path, "/matches/"));
        else if (method.equals("PUT") && path.matches("/matches/\\d+/score")) updateScore(ex, idFrom(path, "/matches/"));
        else if (method.equals("POST") && path.matches("/matches/\\d+/events")) addEvent(ex, idFrom(path, "/matches/"));
        else if (method.equals("GET") && path.equals("/standings")) json(ex, 200, store.standingsJson());
        else if (method.equals("GET") && path.equals("/leagues")) json(ex, 200, store.leaguesJson());
        else if (method.equals("GET") && path.matches("/leagues/\\d+/standings")) json(ex, 200, store.standingsJson());
        else if (method.equals("GET") && path.matches("/leagues/\\d+/matches")) json(ex, 200, store.matchesJson());
        else if (method.equals("GET") && path.equals("/teams")) json(ex, 200, store.teamsJson());
        else if (method.equals("GET") && path.matches("/teams/\\d+")) teamDetail(ex, idFrom(path, "/teams/"));
        else if (method.equals("GET") && path.matches("/teams/\\d+/players")) json(ex, 200, store.playersJson(String.valueOf(idFrom(path, "/teams/"))));
        else if (method.equals("GET") && path.matches("/teams/\\d+/matches")) json(ex, 200, store.matchesForTeam(idFrom(path, "/teams/")));
        else if (method.equals("GET") && path.equals("/players")) json(ex, 200, store.playersJson(query(ex).get("teamId")));
        else if (method.equals("GET") && path.matches("/players/\\d+")) playerDetail(ex, idFrom(path, "/players/"));
        else if (method.equals("GET") && path.equals("/favorites")) favorites(ex);
        else if (method.equals("POST") && path.equals("/favorites")) addFavorite(ex);
        else if (method.equals("POST") && path.matches("/favorites/teams/\\d+")) addFavoriteTeam(ex, idFrom(path, "/favorites/teams/"));
        else if (method.equals("DELETE") && path.matches("/favorites/teams/\\d+")) deleteFavorite(ex, idFrom(path, "/favorites/teams/"));
        else if (method.equals("DELETE") && path.startsWith("/favorites/")) deleteFavorite(ex, idFrom(path, "/favorites/"));
        else if (method.equals("GET") && path.equals("/notifications")) notifications(ex);
        else if (method.equals("GET") && path.equals("/audit")) audit(ex);
        else if (method.equals("GET") && path.equals("/admin/audit-logs")) audit(ex);
        else if (method.equals("GET") && path.equals("/admin/leagues")) adminRead(ex, store.leaguesJson());
        else if (method.equals("GET") && path.equals("/admin/seasons")) adminRead(ex, store.seasonsJson());
        else if (method.equals("GET") && path.equals("/admin/teams")) adminRead(ex, store.teamsJson());
        else if (method.equals("POST") && path.equals("/admin/teams")) adminCreateTeam(ex);
        else if (method.equals("PUT") && path.matches("/admin/teams/\\d+")) adminUpdateTeam(ex, idFrom(path, "/admin/teams/"));
        else if (method.equals("DELETE") && path.matches("/admin/teams/\\d+")) adminDeleteTeam(ex, idFrom(path, "/admin/teams/"));
        else if (method.equals("GET") && path.equals("/admin/players")) adminRead(ex, store.playersJson(""));
        else if (method.equals("POST") && path.equals("/admin/players")) adminCreatePlayer(ex);
        else if (method.equals("PUT") && path.matches("/admin/players/\\d+")) adminUpdatePlayer(ex, idFrom(path, "/admin/players/"));
        else if (method.equals("DELETE") && path.matches("/admin/players/\\d+")) adminDeletePlayer(ex, idFrom(path, "/admin/players/"));
        else if (method.equals("GET") && path.equals("/admin/users")) adminRead(ex, store.users.stream().map(User::json).toList());
        else if (method.equals("PUT") && path.matches("/admin/users/\\d+")) adminUpdateUser(ex, idFrom(path, "/admin/users/"));
        else if (method.equals("DELETE") && path.matches("/admin/users/\\d+")) adminDeleteUser(ex, idFrom(path, "/admin/users/"));
        else if (method.equals("POST") && path.equals("/admin/matches")) adminCreateMatch(ex);
        else if (method.equals("PUT") && path.matches("/admin/matches/\\d+")) updateScore(ex, idFrom(path, "/admin/matches/"));
        else if (method.equals("DELETE") && path.matches("/admin/matches/\\d+")) adminDeleteMatch(ex, idFrom(path, "/admin/matches/"));
        else if (method.equals("POST") && path.matches("/admin/matches/\\d+/events")) addEvent(ex, idFrom(path, "/admin/matches/"));
        else if (method.equals("GET") && path.equals("/export/standings.csv")) exportStandings(ex);
        else if (method.equals("GET") && path.equals("/search")) search(ex, query(ex).getOrDefault("q", ""));
        else throw new HttpError(404, "Endpoint not found");
    }

    private static void login(HttpExchange ex) throws Exception {
        Map<String, String> body = parseJsonObject(readBody(ex));
        User user = store.findUserByEmail(body.getOrDefault("email", "")).orElseThrow(() -> new HttpError(401, "Invalid email or password"));
        if (!Password.verify(body.getOrDefault("password", ""), user.passwordHash)) throw new HttpError(401, "Invalid email or password");
        json(ex, 200, map("token", token(user), "refreshToken", token(user, 7L * 24L * 60L * 60L * 1000L), "user", user.json()));
    }

    private static void register(HttpExchange ex) throws Exception {
        Map<String, String> body = parseJsonObject(readBody(ex));
        String email = body.getOrDefault("email", "").trim().toLowerCase(Locale.ROOT);
        String username = body.getOrDefault("username", "").trim();
        String password = body.getOrDefault("password", "");
        if (!email.contains("@") || username.length() < 3 || password.length() < 8) throw new HttpError(400, "Provide username, valid email, and password of at least 8 characters");
        User user = store.createUser(username, email, Password.hash(password), "registered");
        json(ex, 201, map("token", token(user), "refreshToken", token(user, 7L * 24L * 60L * 60L * 1000L), "user", user.json()));
    }

    private static void refresh(HttpExchange ex) throws Exception {
        User user = requireUser(ex);
        json(ex, 200, map("token", token(user), "refreshToken", token(user, 7L * 24L * 60L * 60L * 1000L), "user", user.json()));
    }

    private static void me(HttpExchange ex) throws IOException {
        json(ex, 200, requireUser(ex).json());
    }

    private static void matchDetail(HttpExchange ex, int id) throws IOException {
        Match match = store.findMatch(id).orElseThrow(() -> new HttpError(404, "Match not found"));
        Map<String, Object> data = match.json();
        data.put("events", store.eventsFor(id).stream().map(Event::json).toList());
        data.put("lineups", store.lineupFor(id).stream().map(Player::json).toList());
        data.put("stats", map("possessionHome", 57, "shotsHome", 14, "shotsAway", 8, "cornersHome", 6, "cornersAway", 3));
        data.put("h2h", store.matches.stream().filter(m -> (m.homeTeamId == match.homeTeamId || m.awayTeamId == match.homeTeamId) && (m.homeTeamId == match.awayTeamId || m.awayTeamId == match.awayTeamId)).map(Match::json).toList());
        json(ex, 200, data);
    }

    private static void teamDetail(HttpExchange ex, int id) throws IOException {
        Team team = store.team(id);
        Map<String, Object> data = team.json();
        data.put("players", store.players.stream().filter(p -> p.teamId == id).map(Player::json).toList());
        data.put("matches", store.matchesForTeam(id));
        data.put("form", List.of("W", "D", "L", "W", "W"));
        json(ex, 200, data);
    }

    private static void playerDetail(HttpExchange ex, int id) throws IOException {
        Player player = store.player(id);
        if (player == null) throw new HttpError(404, "Player not found");
        Map<String, Object> data = player.json();
        data.put("seasonStats", map("appearances", 23, "goals", player.position.equals("Forward") ? 16 : 4, "assists", player.position.equals("Midfielder") ? 11 : 3, "rating", "7.6"));
        data.put("recentInvolvement", store.events.stream().filter(e -> e.playerId == id).map(Event::json).toList());
        json(ex, 200, data);
    }

    private static void updateScore(HttpExchange ex, int id) throws Exception {
        User user = requireRole(ex, "admin");
        Map<String, String> body = parseJsonObject(readBody(ex));
        int home = Integer.parseInt(body.getOrDefault("homeScore", "0"));
        int away = Integer.parseInt(body.getOrDefault("awayScore", "0"));
        String status = body.getOrDefault("status", "live");
        store.updateScore(id, home, away, status, user, clientIp(ex));
        json(ex, 200, store.findMatch(id).orElseThrow().json());
    }

    private static void addEvent(HttpExchange ex, int id) throws Exception {
        User user = requireRole(ex, "admin");
        Map<String, String> body = parseJsonObject(readBody(ex));
        Event event = store.addEvent(id, Integer.parseInt(body.getOrDefault("playerId", "0")), body.getOrDefault("eventType", "goal"), Integer.parseInt(body.getOrDefault("minute", "0")), body.getOrDefault("detail", ""), user, clientIp(ex));
        json(ex, 201, event.json());
    }

    private static void favorites(HttpExchange ex) throws IOException {
        User user = requireUser(ex);
        json(ex, 200, store.favoriteTeams(user.id).stream().map(Team::json).toList());
    }

    private static void addFavorite(HttpExchange ex) throws IOException {
        User user = requireUser(ex);
        int teamId = Integer.parseInt(parseJsonObject(readBody(ex)).getOrDefault("teamId", "0"));
        store.addFavorite(user.id, teamId);
        json(ex, 201, map("message", "Favorite saved"));
    }

    private static void addFavoriteTeam(HttpExchange ex, int teamId) throws IOException {
        User user = requireUser(ex);
        store.addFavorite(user.id, teamId);
        json(ex, 201, map("message", "Favorite saved"));
    }

    private static void deleteFavorite(HttpExchange ex, int teamId) throws IOException {
        User user = requireUser(ex);
        store.removeFavorite(user.id, teamId);
        json(ex, 200, map("message", "Favorite removed"));
    }

    private static void notifications(HttpExchange ex) throws IOException {
        User user = requireUser(ex);
        json(ex, 200, store.notificationsFor(user.id).stream().map(Notification::json).toList());
    }

    private static void audit(HttpExchange ex) throws IOException {
        requireRole(ex, "admin");
        json(ex, 200, store.audit.stream().map(Audit::json).toList());
    }

    private static void adminRead(HttpExchange ex, Object data) throws IOException {
        requireRole(ex, "admin");
        json(ex, 200, data);
    }

    private static void adminCreateTeam(HttpExchange ex) throws IOException {
        User user = requireRole(ex, "admin");
        Team team = store.createTeam(parseJsonObject(readBody(ex)), user, clientIp(ex));
        json(ex, 201, team.json());
    }

    private static void adminUpdateTeam(HttpExchange ex, int id) throws IOException {
        User user = requireRole(ex, "admin");
        Team team = store.updateTeam(id, parseJsonObject(readBody(ex)), user, clientIp(ex));
        json(ex, 200, team.json());
    }

    private static void adminDeleteTeam(HttpExchange ex, int id) throws IOException {
        User user = requireRole(ex, "admin");
        store.deleteTeam(id, user, clientIp(ex));
        json(ex, 200, map("message", "Team deleted"));
    }

    private static void adminCreatePlayer(HttpExchange ex) throws IOException {
        User user = requireRole(ex, "admin");
        Player player = store.createPlayer(parseJsonObject(readBody(ex)), user, clientIp(ex));
        json(ex, 201, player.json());
    }

    private static void adminUpdatePlayer(HttpExchange ex, int id) throws IOException {
        User user = requireRole(ex, "admin");
        Player player = store.updatePlayer(id, parseJsonObject(readBody(ex)), user, clientIp(ex));
        json(ex, 200, player.json());
    }

    private static void adminDeletePlayer(HttpExchange ex, int id) throws IOException {
        User user = requireRole(ex, "admin");
        store.deletePlayer(id, user, clientIp(ex));
        json(ex, 200, map("message", "Player deleted"));
    }

    private static void adminUpdateUser(HttpExchange ex, int id) throws IOException {
        User user = requireRole(ex, "admin");
        User updated = store.updateUserRole(id, parseJsonObject(readBody(ex)).getOrDefault("role", "registered"), user, clientIp(ex));
        json(ex, 200, updated.json());
    }

    private static void adminDeleteUser(HttpExchange ex, int id) throws IOException {
        User user = requireRole(ex, "admin");
        store.deleteUser(id, user, clientIp(ex));
        json(ex, 200, map("message", "User deactivated"));
    }

    private static void adminCreateMatch(HttpExchange ex) throws IOException {
        User user = requireRole(ex, "admin");
        Match match = store.createMatch(parseJsonObject(readBody(ex)), user, clientIp(ex));
        json(ex, 201, match.json());
    }

    private static void adminDeleteMatch(HttpExchange ex, int id) throws IOException {
        User user = requireRole(ex, "admin");
        store.deleteMatch(id, user, clientIp(ex));
        json(ex, 200, map("message", "Match deleted"));
    }

    private static void exportStandings(HttpExchange ex) throws IOException {
        requireUser(ex);
        StringBuilder csv = new StringBuilder("rank,team,played,won,drawn,lost,goals_for,goals_against,goal_difference,points\n");
        int rank = 1;
        for (Standing standing : store.computeStandings()) {
            csv.append(rank++).append(",").append(standing.team.name).append(",").append(standing.played).append(",").append(standing.won).append(",").append(standing.drawn).append(",").append(standing.lost).append(",").append(standing.gf).append(",").append(standing.ga).append(",").append(standing.gf - standing.ga).append(",").append(standing.points).append("\n");
        }
        ex.getResponseHeaders().set("Content-Type", "text/csv; charset=utf-8");
        ex.getResponseHeaders().set("Content-Disposition", "attachment; filename=goalpulse-standings.csv");
        send(ex, 200, csv.toString());
    }

    private static void search(HttpExchange ex, String raw) throws IOException {
        String q = raw.toLowerCase(Locale.ROOT);
        List<Object> results = new ArrayList<>();
        store.teams.stream().filter(t -> t.name.toLowerCase(Locale.ROOT).contains(q) || t.shortName.toLowerCase(Locale.ROOT).contains(q)).forEach(t -> results.add(map("type", "team", "item", t.json())));
        store.players.stream().filter(p -> p.fullName().toLowerCase(Locale.ROOT).contains(q)).forEach(p -> results.add(map("type", "player", "item", p.json())));
        store.matches.stream().filter(m -> m.home().name.toLowerCase(Locale.ROOT).contains(q) || m.away().name.toLowerCase(Locale.ROOT).contains(q)).forEach(m -> results.add(map("type", "match", "item", m.json())));
        json(ex, 200, results);
    }

    private static void proxyCrest(HttpExchange ex) throws Exception {
        String url = query(ex).getOrDefault("url", "");
        if (!url.startsWith("https://crests.football-data.org/")) throw new HttpError(400, "Unsupported image host");
        HttpResponse<byte[]> response = HTTP.send(
            HttpRequest.newBuilder(URI.create(url)).header("Accept", "image/svg+xml,image/png,image/*").build(),
            HttpResponse.BodyHandlers.ofByteArray()
        );
        if (response.statusCode() >= 400) throw new HttpError(404, "Crest image not found");
        ex.getResponseHeaders().set("Content-Type", response.headers().firstValue("Content-Type").orElse(url.endsWith(".svg") ? "image/svg+xml" : "image/png"));
        ex.getResponseHeaders().set("Cache-Control", "public, max-age=86400");
        byte[] bytes = response.body();
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    private static User requireUser(HttpExchange ex) {
        String auth = ex.getRequestHeaders().getFirst("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) throw new HttpError(401, "Login required");
        return verify(auth.substring(7));
    }

    private static User requireRole(HttpExchange ex, String role) {
        User user = requireUser(ex);
        if (!user.role.equals(role)) {
            store.audit.add(new Audit(user.id, "DENY", "security", "-", "role=" + user.role, "required=" + role, clientIp(ex)));
            throw new HttpError(403, "Admin role required");
        }
        return user;
    }

    private static String token(User user) throws Exception {
        return token(user, 60L * 60L * 1000L);
    }

    private static String token(User user, long ttlMillis) throws Exception {
        String header = b64("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        long exp = System.currentTimeMillis() + ttlMillis;
        String payload = b64("{\"sub\":" + user.id + ",\"role\":\"" + user.role + "\",\"exp\":" + exp + "}");
        return header + "." + payload + "." + sign(header + "." + payload);
    }

    private static User verify(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3 || !sign(parts[0] + "." + parts[1]).equals(parts[2])) throw new HttpError(401, "Invalid token");
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            long exp = Long.parseLong(payload.replaceAll(".*\"exp\":(\\d+).*", "$1"));
            int id = Integer.parseInt(payload.replaceAll(".*\"sub\":(\\d+).*", "$1"));
            if (System.currentTimeMillis() > exp) throw new HttpError(401, "Token expired");
            return store.findUser(id).orElseThrow(() -> new HttpError(401, "Unknown user"));
        } catch (Exception err) {
            throw new HttpError(401, "Invalid token");
        }
    }

    private static String sign(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(TOKEN_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }

    private static String b64(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static void serveStatic(HttpExchange ex, String path) throws IOException {
        if (path.equals("/")) path = "/index.html";
        java.nio.file.Path file = java.nio.file.Path.of("frontend" + path).normalize();
        if (!file.startsWith(java.nio.file.Path.of("frontend")) || !java.nio.file.Files.exists(file)) {
            file = java.nio.file.Path.of("frontend/index.html");
        }
        String type = path.endsWith(".css") ? "text/css" : path.endsWith(".js") ? "application/javascript" : path.endsWith(".svg") ? "image/svg+xml" : "text/html";
        ex.getResponseHeaders().set("Content-Type", type + "; charset=utf-8");
        byte[] bytes = java.nio.file.Files.readAllBytes(file);
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    private static String readBody(HttpExchange ex) throws IOException {
        return new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private static void json(HttpExchange ex, int status, Object data) throws IOException {
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        send(ex, status, Json.stringify(map("success", true, "data", data, "message", "Request completed successfully")));
    }

    private static void jsonError(HttpExchange ex, int status, String message) throws IOException {
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        send(ex, status, Json.stringify(map("success", false, "message", message, "status", status)));
    }

    private static void send(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    private static void addCors(HttpExchange ex) {
        Headers h = ex.getResponseHeaders();
        h.set("Access-Control-Allow-Origin", "*");
        h.set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        h.set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
    }

    private static int idFrom(String path, String prefix) {
        String rest = path.substring(prefix.length());
        int slash = rest.indexOf('/');
        return Integer.parseInt(slash >= 0 ? rest.substring(0, slash) : rest);
    }

    private static Map<String, String> query(HttpExchange ex) {
        Map<String, String> out = new HashMap<>();
        String raw = ex.getRequestURI().getRawQuery();
        if (raw == null || raw.isBlank()) return out;
        for (String pair : raw.split("&")) {
            String[] kv = pair.split("=", 2);
            out.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8), kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "");
        }
        return out;
    }

    private static String clientIp(HttpExchange ex) {
        return ex.getRemoteAddress() == null ? "unknown" : ex.getRemoteAddress().getAddress().getHostAddress();
    }

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) map.put(String.valueOf(kv[i]), kv[i + 1]);
        return map;
    }

    private static Map<String, String> parseJsonObject(String json) {
        Map<String, String> out = new HashMap<>();
        String body = json.trim();
        if (body.startsWith("{")) body = body.substring(1);
        if (body.endsWith("}")) body = body.substring(0, body.length() - 1);
        for (String part : body.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)")) {
            String[] kv = part.split(":", 2);
            if (kv.length == 2) out.put(clean(kv[0]), clean(kv[1]));
        }
        return out;
    }

    private static String clean(String value) {
        value = value.trim();
        if (value.startsWith("\"")) value = value.substring(1);
        if (value.endsWith("\"")) value = value.substring(0, value.length() - 1);
        return value.replace("\\\"", "\"").replace("\\n", "\n");
    }

    static class Password {
        static final int ITERATIONS = 30000;
        static String hash(String password) throws Exception {
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            byte[] hash = pbkdf(password, salt);
            return "PBKDF2$" + ITERATIONS + "$" + Base64.getEncoder().encodeToString(salt) + "$" + Base64.getEncoder().encodeToString(hash);
        }
        static boolean verify(String password, String stored) throws Exception {
            String[] parts = stored.split("\\$");
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] actual = pbkdf(password, salt);
            return Base64.getEncoder().encodeToString(actual).equals(parts[3]);
        }
        static byte[] pbkdf(String password, byte[] salt) throws Exception {
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, 256);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        }
    }

    static class Store {
        List<Team> teams = new ArrayList<>();
        List<Player> players = new ArrayList<>();
        List<Match> matches = new ArrayList<>();
        List<Event> events = new ArrayList<>();
        List<User> users = new ArrayList<>();
        List<Notification> notifications = new ArrayList<>();
        List<Audit> audit = new ArrayList<>();
        Map<Integer, Set<Integer>> favorites = new HashMap<>();
        int nextUser = 1, nextEvent = 1, nextNotification = 1, nextTeam = 7, nextPlayer = 9, nextMatch = 4;

        void seed() throws Exception {
            teams.add(new Team(1, "Premier League", "Manchester City", "MCI", "Manchester", "Etihad Stadium", "Pep Guardiola", 1880, "#6cabdd", ""));
            teams.add(new Team(2, "Premier League", "Liverpool", "LIV", "Liverpool", "Anfield", "Arne Slot", 1892, "#c8102e", ""));
            teams.add(new Team(3, "La Liga", "Real Madrid", "RMA", "Madrid", "Santiago Bernabeu", "Carlo Ancelotti", 1902, "#f7f7f7", ""));
            teams.add(new Team(4, "La Liga", "Barcelona", "BAR", "Barcelona", "Camp Nou", "Hansi Flick", 1899, "#a50044", ""));
            teams.add(new Team(5, "Bundesliga", "Bayern Munich", "BAY", "Munich", "Allianz Arena", "Vincent Kompany", 1900, "#dc052d", ""));
            teams.add(new Team(6, "Bundesliga", "Borussia Dortmund", "BVB", "Dortmund", "Signal Iduna Park", "Niko Kovac", 1909, "#fdeb00", ""));
            players.add(new Player(1, 1, "Erling", "Haaland", "Forward", "Norway", 9));
            players.add(new Player(2, 1, "Kevin", "De Bruyne", "Midfielder", "Belgium", 17));
            players.add(new Player(3, 2, "Mohamed", "Salah", "Forward", "Egypt", 11));
            players.add(new Player(4, 2, "Virgil", "van Dijk", "Defender", "Netherlands", 4));
            players.add(new Player(5, 3, "Vinicius", "Junior", "Forward", "Brazil", 7));
            players.add(new Player(6, 4, "Lamine", "Yamal", "Forward", "Spain", 19));
            players.add(new Player(7, 5, "Harry", "Kane", "Forward", "England", 9));
            players.add(new Player(8, 6, "Julian", "Brandt", "Midfielder", "Germany", 10));
            matches.add(new Match(1, 1, 2, LocalDateTime.of(2026, 4, 24, 20, 45), "live", 2, 1, "Etihad Stadium", "Michael Oliver"));
            matches.add(new Match(2, 3, 4, LocalDateTime.of(2026, 4, 25, 21, 0), "scheduled", 0, 0, "Santiago Bernabeu", "Jose Sanchez"));
            matches.add(new Match(3, 5, 6, LocalDateTime.of(2026, 4, 20, 18, 30), "finished", 3, 2, "Allianz Arena", "Felix Zwayer"));
            addSeedEvent(1, 1, "goal", 18, "Left-foot finish from inside the box");
            addSeedEvent(1, 3, "goal", 37, "Penalty converted");
            addSeedEvent(1, 2, "goal", 61, "Free kick into the top corner");
            addSeedEvent(3, 7, "goal", 12, "Header from corner");
            addSeedEvent(3, 8, "goal", 44, "Long-range shot");
            createUser("Admin", "admin@goalpulse.test", Password.hash("Admin123!"), "admin");
            User user = createUser("Mia", "mia@goalpulse.test", Password.hash("User123!"), "registered");
            addFavorite(user.id, 1);
            notifications.add(new Notification(nextNotification++, user.id, 1, "goal", "Manchester City scored against Liverpool.", false));
            loadOnlineMatchesIfConfigured();
        }

        void loadOnlineMatchesIfConfigured() {
            String token = System.getenv("FOOTBALL_DATA_API_TOKEN");
            if (token == null || token.isBlank()) return;
            try {
                HttpResponse<String> response = HTTP.send(
                    HttpRequest.newBuilder(URI.create("https://api.football-data.org/v4/matches"))
                        .header("X-Auth-Token", token)
                        .header("Accept", "application/json")
                        .build(),
                    HttpResponse.BodyHandlers.ofString()
                );
                if (response.statusCode() >= 400) throw new IOException("football-data.org returned HTTP " + response.statusCode());
                String body = response.body();
                List<Match> onlineMatches = new ArrayList<>();
                Map<String, Team> onlineTeams = new LinkedHashMap<>();
                int matchId = 1000;
                for (String rawMatch : JsonBits.objectsInArray(body, "matches")) {
                    String league = JsonBits.string(rawMatch, "competition", "name", "European Competition");
                    String venue = JsonBits.string(rawMatch, "", "venue", "Venue TBA");
                    String status = normalizeStatus(JsonBits.string(rawMatch, "", "status", "scheduled"));
                    LocalDateTime date = parseUtc(JsonBits.string(rawMatch, "", "utcDate", LocalDateTime.now().format(ISO)));
                    String homeName = JsonBits.string(rawMatch, "homeTeam", "name", "Home Team");
                    String awayName = JsonBits.string(rawMatch, "awayTeam", "name", "Away Team");
                    String homeShort = JsonBits.string(rawMatch, "homeTeam", "tla", abbreviate(homeName));
                    String awayShort = JsonBits.string(rawMatch, "awayTeam", "tla", abbreviate(awayName));
                    String homeLogo = JsonBits.string(rawMatch, "homeTeam", "crest", "");
                    String awayLogo = JsonBits.string(rawMatch, "awayTeam", "crest", "");
                    Team home = onlineTeams.computeIfAbsent(homeName, name -> new Team(1000 + onlineTeams.size(), league, homeName, homeShort, "", "Stadium TBA", "Coach TBA", 1900, colorFor(homeShort), homeLogo));
                    Team away = onlineTeams.computeIfAbsent(awayName, name -> new Team(1000 + onlineTeams.size(), league, awayName, awayShort, "", "Stadium TBA", "Coach TBA", 1900, colorFor(awayShort), awayLogo));
                    int homeScore = JsonBits.score(rawMatch, "home", 0);
                    int awayScore = JsonBits.score(rawMatch, "away", 0);
                    onlineMatches.add(new Match(matchId++, home.id, away.id, date, status, homeScore, awayScore, venue, "TBA"));
                }
                if (!onlineMatches.isEmpty()) {
                    teams = new ArrayList<>(onlineTeams.values());
                    matches = onlineMatches;
                    players = new ArrayList<>();
                    events = new ArrayList<>();
                    favorites.clear();
                    notifications.clear();
                    System.out.println("Loaded " + matches.size() + " matches from football-data.org");
                }
            } catch (Exception err) {
                System.out.println("Using seeded data because online football data failed: " + err.getMessage());
            }
        }

        String normalizeStatus(String value) {
            value = value.toUpperCase(Locale.ROOT);
            if (value.equals("IN_PLAY") || value.equals("PAUSED")) return "live";
            if (value.equals("FINISHED")) return "finished";
            return "scheduled";
        }

        LocalDateTime parseUtc(String value) {
            try {
                return LocalDateTime.parse(value.replace("Z", ""));
            } catch (Exception err) {
                return LocalDateTime.now();
            }
        }

        String abbreviate(String name) {
            String letters = name.replaceAll("[^A-Za-z]", "");
            return letters.length() <= 3 ? letters.toUpperCase(Locale.ROOT) : letters.substring(0, 3).toUpperCase(Locale.ROOT);
        }

        String colorFor(String text) {
            String[] colors = {"#2cff9b", "#33d6ff", "#ff4f5e", "#ffd166", "#a78bfa", "#f472b6"};
            return colors[Math.abs(text.hashCode()) % colors.length];
        }

        void addSeedEvent(int matchId, int playerId, String type, int minute, String detail) {
            events.add(new Event(nextEvent++, matchId, playerId, type, minute, detail));
        }

        User createUser(String username, String email, String hash, String role) {
            if (users.stream().anyMatch(u -> u.email.equalsIgnoreCase(email))) throw new HttpError(409, "Email already exists");
            User user = new User(nextUser++, username, email, hash, role);
            users.add(user);
            return user;
        }

        Team createTeam(Map<String, String> body, User user, String ip) {
            Team team = new Team(nextTeam++, body.getOrDefault("league", "Premier League"), body.getOrDefault("name", "New Team"), body.getOrDefault("shortName", "NEW"), body.getOrDefault("city", ""), body.getOrDefault("stadium", "TBA"), body.getOrDefault("coach", "TBA"), Integer.parseInt(body.getOrDefault("founded", "1900")), body.getOrDefault("color", "#33d6ff"), body.getOrDefault("logoUrl", ""));
            teams.add(team);
            audit.add(new Audit(user.id, "INSERT", "team", String.valueOf(team.id), "", team.name, ip));
            return team;
        }

        Team updateTeam(int id, Map<String, String> body, User user, String ip) {
            Team old = team(id);
            Team updated = new Team(id, body.getOrDefault("league", old.league), body.getOrDefault("name", old.name), body.getOrDefault("shortName", old.shortName), body.getOrDefault("city", old.city), body.getOrDefault("stadium", old.stadium), body.getOrDefault("coach", old.coach), Integer.parseInt(body.getOrDefault("founded", String.valueOf(old.founded))), body.getOrDefault("color", old.color), body.getOrDefault("logoUrl", old.logoUrl));
            teams.replaceAll(t -> t.id == id ? updated : t);
            audit.add(new Audit(user.id, "UPDATE", "team", String.valueOf(id), old.name, updated.name, ip));
            return updated;
        }

        void deleteTeam(int id, User user, String ip) {
            Team old = team(id);
            teams.removeIf(t -> t.id == id);
            players.removeIf(p -> p.teamId == id);
            audit.add(new Audit(user.id, "DELETE", "team", String.valueOf(id), old.name, "", ip));
        }

        Player createPlayer(Map<String, String> body, User user, String ip) {
            Player player = new Player(nextPlayer++, Integer.parseInt(body.getOrDefault("teamId", "1")), body.getOrDefault("firstName", "New"), body.getOrDefault("lastName", "Player"), body.getOrDefault("position", "Forward"), body.getOrDefault("nationality", "Unknown"), Integer.parseInt(body.getOrDefault("shirtNumber", "99")));
            players.add(player);
            audit.add(new Audit(user.id, "INSERT", "player", String.valueOf(player.id), "", player.fullName(), ip));
            return player;
        }

        Player updatePlayer(int id, Map<String, String> body, User user, String ip) {
            Player old = player(id);
            if (old == null) throw new HttpError(404, "Player not found");
            Player updated = new Player(id, Integer.parseInt(body.getOrDefault("teamId", String.valueOf(old.teamId))), body.getOrDefault("firstName", old.firstName), body.getOrDefault("lastName", old.lastName), body.getOrDefault("position", old.position), body.getOrDefault("nationality", old.nationality), Integer.parseInt(body.getOrDefault("shirtNumber", String.valueOf(old.shirtNumber))));
            players.replaceAll(p -> p.id == id ? updated : p);
            audit.add(new Audit(user.id, "UPDATE", "player", String.valueOf(id), old.fullName(), updated.fullName(), ip));
            return updated;
        }

        void deletePlayer(int id, User user, String ip) {
            Player old = player(id);
            if (old == null) throw new HttpError(404, "Player not found");
            players.removeIf(p -> p.id == id);
            audit.add(new Audit(user.id, "DELETE", "player", String.valueOf(id), old.fullName(), "", ip));
        }

        User updateUserRole(int id, String role, User admin, String ip) {
            User old = findUser(id).orElseThrow(() -> new HttpError(404, "User not found"));
            User updated = new User(old.id, old.username, old.email, old.passwordHash, role);
            users.replaceAll(u -> u.id == id ? updated : u);
            audit.add(new Audit(admin.id, "UPDATE", "app_user", String.valueOf(id), old.role, role, ip));
            return updated;
        }

        void deleteUser(int id, User admin, String ip) {
            User old = findUser(id).orElseThrow(() -> new HttpError(404, "User not found"));
            users.removeIf(u -> u.id == id);
            audit.add(new Audit(admin.id, "DELETE", "app_user", String.valueOf(id), old.email, "", ip));
        }

        Match createMatch(Map<String, String> body, User user, String ip) {
            Match match = new Match(nextMatch++, Integer.parseInt(body.getOrDefault("homeTeamId", "1")), Integer.parseInt(body.getOrDefault("awayTeamId", "2")), LocalDateTime.parse(body.getOrDefault("date", LocalDateTime.now().format(ISO))), body.getOrDefault("status", "scheduled"), Integer.parseInt(body.getOrDefault("homeScore", "0")), Integer.parseInt(body.getOrDefault("awayScore", "0")), body.getOrDefault("venue", "TBA"), body.getOrDefault("referee", "TBA"));
            matches.add(match);
            audit.add(new Audit(user.id, "INSERT", "match", String.valueOf(match.id), "", match.home().shortName + " vs " + match.away().shortName, ip));
            return match;
        }

        void deleteMatch(int id, User user, String ip) {
            Match old = findMatch(id).orElseThrow(() -> new HttpError(404, "Match not found"));
            matches.removeIf(m -> m.id == id);
            events.removeIf(e -> e.matchId == id);
            audit.add(new Audit(user.id, "DELETE", "match", String.valueOf(id), old.home().shortName + " vs " + old.away().shortName, "", ip));
        }

        Optional<User> findUserByEmail(String email) { return users.stream().filter(u -> u.email.equalsIgnoreCase(email)).findFirst(); }
        Optional<User> findUser(int id) { return users.stream().filter(u -> u.id == id).findFirst(); }
        Optional<Match> findMatch(int id) { return matches.stream().filter(m -> m.id == id).findFirst(); }
        Team team(int id) { return teams.stream().filter(t -> t.id == id).findFirst().orElseThrow(); }
        Player player(int id) { return players.stream().filter(p -> p.id == id).findFirst().orElse(null); }
        List<Event> eventsFor(int matchId) { return events.stream().filter(e -> e.matchId == matchId).sorted(Comparator.comparingInt(e -> e.minute)).toList(); }
        List<Player> lineupFor(int matchId) {
            Match match = findMatch(matchId).orElseThrow();
            return players.stream().filter(p -> p.teamId == match.homeTeamId || p.teamId == match.awayTeamId).toList();
        }

        void updateScore(int id, int home, int away, String status, User user, String ip) {
            Match match = findMatch(id).orElseThrow(() -> new HttpError(404, "Match not found"));
            String old = match.homeScore + "-" + match.awayScore + " " + match.status;
            match.homeScore = home; match.awayScore = away; match.status = status;
            audit.add(new Audit(user.id, "UPDATE", "match", String.valueOf(id), old, home + "-" + away + " " + status, ip));
            notifyFavorites(match, "score", match.home().shortName + " " + home + "-" + away + " " + match.away().shortName);
        }

        Event addEvent(int matchId, int playerId, String type, int minute, String detail, User user, String ip) {
            findMatch(matchId).orElseThrow(() -> new HttpError(404, "Match not found"));
            Event event = new Event(nextEvent++, matchId, playerId, type, minute, detail);
            events.add(event);
            audit.add(new Audit(user.id, "INSERT", "match_event", String.valueOf(event.id), "", type + " minute " + minute, ip));
            notifyFavorites(findMatch(matchId).orElseThrow(), type, detail);
            return event;
        }

        void notifyFavorites(Match match, String type, String detail) {
            for (Map.Entry<Integer, Set<Integer>> fav : favorites.entrySet()) {
                if (fav.getValue().contains(match.homeTeamId) || fav.getValue().contains(match.awayTeamId)) {
                    notifications.add(new Notification(nextNotification++, fav.getKey(), match.id, type, detail, false));
                }
            }
        }

        void addFavorite(int userId, int teamId) {
            if (teams.stream().noneMatch(t -> t.id == teamId)) throw new HttpError(404, "Team not found");
            favorites.computeIfAbsent(userId, id -> new HashSet<>()).add(teamId);
        }
        void removeFavorite(int userId, int teamId) { favorites.computeIfAbsent(userId, id -> new HashSet<>()).remove(teamId); }
        List<Team> favoriteTeams(int userId) { return favorites.getOrDefault(userId, Set.of()).stream().map(this::team).toList(); }
        List<Notification> notificationsFor(int userId) { return notifications.stream().filter(n -> n.userId == userId).sorted(Comparator.comparing((Notification n) -> n.id).reversed()).toList(); }

        List<Standing> computeStandings() {
            Map<Integer, Standing> map = new LinkedHashMap<>();
            teams.forEach(t -> map.put(t.id, new Standing(t)));
            for (Match m : matches.stream().filter(m -> m.status.equals("finished")).toList()) {
                Standing h = map.get(m.homeTeamId), a = map.get(m.awayTeamId);
                h.played++; a.played++; h.gf += m.homeScore; h.ga += m.awayScore; a.gf += m.awayScore; a.ga += m.homeScore;
                if (m.homeScore > m.awayScore) { h.won++; h.points += 3; a.lost++; }
                else if (m.homeScore < m.awayScore) { a.won++; a.points += 3; h.lost++; }
                else { h.drawn++; a.drawn++; h.points++; a.points++; }
            }
            return map.values().stream().sorted(Comparator.comparingInt((Standing s) -> s.points).thenComparingInt(s -> s.gf - s.ga).thenComparingInt(s -> s.gf).reversed()).toList();
        }

        Object matchesJson() { return matches.stream().map(Match::json).toList(); }
        Object matchesByStatus(String status) { return matches.stream().filter(m -> m.status.equals(status)).map(Match::json).toList(); }
        Object matchesForDate(String date) { return matches.stream().filter(m -> m.date.toLocalDate().toString().equals(date)).map(Match::json).toList(); }
        Object matchesForTeam(int teamId) { return matches.stream().filter(m -> m.homeTeamId == teamId || m.awayTeamId == teamId).map(Match::json).toList(); }
        Object leaguesJson() {
            List<Object> leagues = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            int id = 1;
            for (Team team : teams) {
                if (seen.add(team.league)) leagues.add(map("id", id++, "name", team.league, "country", team.league.equals("Premier League") ? "England" : team.league.equals("La Liga") ? "Spain" : "Germany", "currentSeason", "2025/26"));
            }
            return leagues;
        }
        Object seasonsJson() {
            return List.of(map("id", 1, "name", "2025/26", "startDate", "2025-08-01", "endDate", "2026-05-31"));
        }
        Object teamsJson() { return teams.stream().map(Team::json).toList(); }
        Object playersJson(String teamId) {
            return players.stream().filter(p -> teamId == null || teamId.isBlank() || p.teamId == Integer.parseInt(teamId)).map(Player::json).toList();
        }
        Object standingsJson() {
            List<Object> rows = new ArrayList<>();
            int rank = 1;
            for (Standing s : computeStandings()) rows.add(s.json(rank++));
            return rows;
        }
    }

    record Team(int id, String league, String name, String shortName, String city, String stadium, String coach, int founded, String color, String logoUrl) {
        Map<String, Object> json() { return map("id", id, "league", league, "name", name, "shortName", shortName, "city", city, "stadium", stadium, "coach", coach, "founded", founded, "color", color, "logoUrl", logoUrl); }
    }
    record Player(int id, int teamId, String firstName, String lastName, String position, String nationality, int shirtNumber) {
        String fullName() { return firstName + " " + lastName; }
        Map<String, Object> json() { return map("id", id, "teamId", teamId, "team", store.team(teamId).name, "name", fullName(), "position", position, "nationality", nationality, "shirtNumber", shirtNumber); }
    }
    static class Match {
        int id, homeTeamId, awayTeamId, homeScore, awayScore; LocalDateTime date; String status, venue, referee;
        Match(int id, int homeTeamId, int awayTeamId, LocalDateTime date, String status, int homeScore, int awayScore, String venue, String referee) {
            this.id = id; this.homeTeamId = homeTeamId; this.awayTeamId = awayTeamId; this.date = date; this.status = status; this.homeScore = homeScore; this.awayScore = awayScore; this.venue = venue; this.referee = referee;
        }
        Team home() { return store.team(homeTeamId); }
        Team away() { return store.team(awayTeamId); }
        Map<String, Object> json() { return map("id", id, "homeTeam", home().json(), "awayTeam", away().json(), "date", date.format(ISO), "status", status, "homeScore", homeScore, "awayScore", awayScore, "venue", venue, "referee", referee); }
    }
    record Event(int id, int matchId, int playerId, String eventType, int minute, String detail) {
        Map<String, Object> json() { Player p = store.player(playerId); return map("id", id, "matchId", matchId, "playerId", playerId, "player", p == null ? "Unknown" : p.fullName(), "eventType", eventType, "minute", minute, "detail", detail); }
    }
    record User(int id, String username, String email, String passwordHash, String role) {
        Map<String, Object> json() { return map("id", id, "username", username, "email", email, "role", role); }
    }
    record Notification(int id, int userId, int matchId, String eventType, String message, boolean isRead) {
        Map<String, Object> json() { return map("id", id, "matchId", matchId, "eventType", eventType, "message", message, "isRead", isRead); }
    }
    record Audit(int userId, String action, String tableName, String recordId, String oldValue, String newValue, String ipAddress) {
        Map<String, Object> json() { return map("userId", userId, "action", action, "tableName", tableName, "recordId", recordId, "oldValue", oldValue, "newValue", newValue, "timestamp", LocalDateTime.now().format(ISO), "ipAddress", ipAddress); }
    }
    static class Standing {
        Team team; int played, won, drawn, lost, gf, ga, points;
        Standing(Team team) { this.team = team; }
        Map<String, Object> json(int rank) { return map("rank", rank, "team", team.json(), "played", played, "won", won, "drawn", drawn, "lost", lost, "goalsFor", gf, "goalsAgainst", ga, "goalDifference", gf - ga, "points", points); }
    }
    static class HttpError extends RuntimeException {
        final int status;
        HttpError(int status, String message) { super(message); this.status = status; }
    }
    static class Json {
        static String stringify(Object value) {
            if (value == null) return "null";
            if (value instanceof String s) return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
            if (value instanceof Number || value instanceof Boolean) return String.valueOf(value);
            if (value instanceof Map<?, ?> m) {
                List<String> parts = new ArrayList<>();
                for (Map.Entry<?, ?> e : m.entrySet()) parts.add(stringify(String.valueOf(e.getKey())) + ":" + stringify(e.getValue()));
                return "{" + String.join(",", parts) + "}";
            }
            if (value instanceof Iterable<?> it) {
                List<String> parts = new ArrayList<>();
                for (Object item : it) parts.add(stringify(item));
                return "[" + String.join(",", parts) + "]";
            }
            return stringify(String.valueOf(value));
        }
    }

    static class JsonBits {
        static List<String> objectsInArray(String json, String key) {
            List<String> objects = new ArrayList<>();
            int keyAt = json.indexOf("\"" + key + "\"");
            if (keyAt < 0) return objects;
            int start = json.indexOf('[', keyAt);
            if (start < 0) return objects;
            int depth = 0;
            int objectStart = -1;
            boolean inString = false;
            for (int i = start + 1; i < json.length(); i++) {
                char c = json.charAt(i);
                char prev = i > 0 ? json.charAt(i - 1) : 0;
                if (c == '"' && prev != '\\') inString = !inString;
                if (inString) continue;
                if (c == '{') {
                    if (depth == 0) objectStart = i;
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0 && objectStart >= 0) objects.add(json.substring(objectStart, i + 1));
                } else if (c == ']' && depth == 0) {
                    break;
                }
            }
            return objects;
        }

        static String string(String json, String objectKey, String fieldKey, String fallback) {
            String block = objectKey.isBlank() ? json : objectBlock(json, objectKey);
            if (block.isBlank()) return fallback;
            String needle = "\"" + fieldKey + "\"";
            int keyAt = block.indexOf(needle);
            if (keyAt < 0) return fallback;
            int colon = block.indexOf(':', keyAt + needle.length());
            if (colon < 0) return fallback;
            int firstQuote = block.indexOf('"', colon + 1);
            if (firstQuote < 0) return fallback;
            int endQuote = firstQuote + 1;
            while (endQuote < block.length()) {
                if (block.charAt(endQuote) == '"' && block.charAt(endQuote - 1) != '\\') break;
                endQuote++;
            }
            return block.substring(firstQuote + 1, endQuote).replace("\\/", "/").replace("\\\"", "\"");
        }

        static int score(String json, String key, int fallback) {
            String block = objectBlock(json, "fullTime");
            if (block.isBlank()) return fallback;
            String needle = "\"" + key + "\"";
            int keyAt = block.indexOf(needle);
            if (keyAt < 0) return fallback;
            int colon = block.indexOf(':', keyAt + needle.length());
            if (colon < 0) return fallback;
            int end = colon + 1;
            while (end < block.length() && (Character.isWhitespace(block.charAt(end)) || block.charAt(end) == 'n' || block.charAt(end) == 'u' || block.charAt(end) == 'l')) end++;
            int start = end;
            while (end < block.length() && Character.isDigit(block.charAt(end))) end++;
            if (start == end) return fallback;
            return Integer.parseInt(block.substring(start, end));
        }

        static String objectBlock(String json, String key) {
            int keyAt = json.indexOf("\"" + key + "\"");
            if (keyAt < 0) return "";
            int start = json.indexOf('{', keyAt);
            if (start < 0) return "";
            int depth = 0;
            boolean inString = false;
            for (int i = start; i < json.length(); i++) {
                char c = json.charAt(i);
                char prev = i > 0 ? json.charAt(i - 1) : 0;
                if (c == '"' && prev != '\\') inString = !inString;
                if (inString) continue;
                if (c == '{') depth++;
                if (c == '}') {
                    depth--;
                    if (depth == 0) return json.substring(start, i + 1);
                }
            }
            return "";
        }
    }
}
