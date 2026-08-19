package my_app.infra.camera;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Servidor HTTP fake, sem nenhuma lib nova (com.sun.net.httpserver já vem no JDK), simulando o
 * endpoint /cgi-bin/snapshot.cgi de uma câmera Intelbras real: primeira requisição sem
 * Authorization recebe 401 + desafio Digest; a segunda (com Authorization) só devolve o JPEG se
 * o response calculado bater com o esperado pro usuário/senha configurados — mesma verificação
 * RFC 2617 que a câmera de verdade faria, então o teste pega de verdade um bug no cálculo do
 * digest (ordem errada de campos, aspas faltando, etc.), não só "alguma string chegou".
 */
class CameraSnapshotClientTest {

    private static final String REALM = "IPCam";
    private static final String NONCE = "abc123nonce";
    private static final String USUARIO = "admin";
    private static final String SENHA = "senha123";
    private static final byte[] JPEG_FALSO = "FAKE-JPEG-BYTES".getBytes(StandardCharsets.UTF_8);

    private HttpServer server;

    @AfterEach
    void pararServidor() {
        if (server != null) server.stop(0);
    }

    private int iniciarServidorFake() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/cgi-bin/snapshot.cgi", this::handleSnapshot);
        server.start();
        return server.getAddress().getPort();
    }

    private void handleSnapshot(HttpExchange exchange) throws java.io.IOException {
        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        if (auth == null || !auth.startsWith("Digest ")) {
            exchange.getResponseHeaders().add("WWW-Authenticate",
                    "Digest realm=\"" + REALM + "\", nonce=\"" + NONCE + "\", qop=\"auth\", opaque=\"xyz\"");
            exchange.sendResponseHeaders(401, -1);
            exchange.close();
            return;
        }

        var params = parseAuthParams(auth);
        String esperado = calcularDigestEsperado(params, exchange.getRequestURI().toString());
        if (!esperado.equals(params.get("response"))) {
            exchange.sendResponseHeaders(401, -1);
            exchange.close();
            return;
        }

        exchange.sendResponseHeaders(200, JPEG_FALSO.length);
        try (var os = exchange.getResponseBody()) {
            os.write(JPEG_FALSO);
        }
    }

    private Map<String, String> parseAuthParams(String header) {
        var params = new java.util.HashMap<String, String>();
        var matcher = Pattern.compile("(\\w+)=\"?([^\",]+)\"?").matcher(header);
        while (matcher.find()) params.put(matcher.group(1), matcher.group(2));
        return params;
    }

    private String calcularDigestEsperado(Map<String, String> params, String uriPath) {
        String ha1 = md5(USUARIO + ":" + REALM + ":" + SENHA);
        String ha2 = md5("GET:" + uriPath);
        return md5(ha1 + ":" + NONCE + ":" + params.get("nc") + ":" + params.get("cnonce") + ":auth:" + ha2);
    }

    private String md5(String input) {
        try {
            var digest = MessageDigest.getInstance("MD5").digest(input.getBytes(StandardCharsets.UTF_8));
            var sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void deveCapturarSnapshotComCredenciaisCorretas() throws Exception {
        int porta = iniciarServidorFake();
        var client = new CameraSnapshotClient();

        byte[] resultado = client.capturarSnapshot("127.0.0.1", porta, USUARIO, SENHA, 1);

        assertArrayEquals(JPEG_FALSO, resultado);
    }

    @Test
    void deveLancarExcecaoComSenhaErrada() throws Exception {
        int porta = iniciarServidorFake();
        var client = new CameraSnapshotClient();

        assertThrows(java.io.IOException.class,
                () -> client.capturarSnapshot("127.0.0.1", porta, USUARIO, "senhaErrada", 1));
    }

    @Test
    void deveLancarExcecaoQuandoCameraNaoResponde() {
        var client = new CameraSnapshotClient();
        // Porta 1 é privilegiada e não deve estar escutando localmente — conexão recusada.
        assertThrows(Exception.class,
                () -> client.capturarSnapshot("127.0.0.1", 1, USUARIO, SENHA, 1));
    }
}
