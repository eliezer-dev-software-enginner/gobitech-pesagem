package my_app.infra.camera;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Captura um snapshot JPEG de uma câmera IP Intelbras (linha VIP) via o endpoint HTTP CGI
 * {@code /cgi-bin/snapshot.cgi} — mesmo padrão herdado da Dahua (fabricante original das
 * câmeras VIP, ver docs/DECISIONS.md). Não existe suporte nativo a Digest Authentication em
 * {@link java.net.http.HttpClient} (ao contrário de Basic, que ele resolve sozinho via
 * {@link java.net.Authenticator}) — a câmera só aceita Digest, então o desafio/resposta (RFC
 * 2617) é implementado aqui na mão, sem depender de nenhuma lib nova.
 * <p>
 * Sem suporte a ONVIF nem ao SDK proprietário (exige assinar termo de confidencialidade com a
 * Intelbras) — o endpoint CGI documentado no fórum oficial já cobre o caso de uso (uma foto por
 * pesagem), sem precisar de RTSP/streaming.
 */
public class CameraSnapshotClient {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public byte[] capturarSnapshot(String host, int porta, String usuario, String senha, int canal) throws IOException, InterruptedException {
        var uri = URI.create("http://" + host + ":" + porta + "/cgi-bin/snapshot.cgi?channel=" + canal);

        var respostaSemAuth = httpClient.send(
                HttpRequest.newBuilder(uri).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray());

        if (respostaSemAuth.statusCode() == 200) {
            return respostaSemAuth.body();
        }
        if (respostaSemAuth.statusCode() != 401) {
            throw new IOException("Câmera " + host + " retornou status inesperado: " + respostaSemAuth.statusCode());
        }

        String wwwAuthenticate = respostaSemAuth.headers().firstValue("WWW-Authenticate")
                .orElseThrow(() -> new IOException("Câmera " + host + " pediu autenticação (401) mas não enviou o header WWW-Authenticate"));
        if (!wwwAuthenticate.toLowerCase().contains("digest")) {
            throw new IOException("Câmera " + host + " exige autenticação não suportada (esperado Digest): " + wwwAuthenticate);
        }

        String uriPath = uri.getRawPath() + (uri.getRawQuery() != null ? "?" + uri.getRawQuery() : "");
        String authorization = montarDigestAuthorization(usuario, senha, "GET", uriPath, parseDigestParams(wwwAuthenticate));

        var respostaComAuth = httpClient.send(
                HttpRequest.newBuilder(uri).header("Authorization", authorization).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray());

        if (respostaComAuth.statusCode() != 200) {
            throw new IOException("Câmera " + host + " recusou a autenticação (status " + respostaComAuth.statusCode() + ") — confira usuário/senha");
        }
        return respostaComAuth.body();
    }

    private Map<String, String> parseDigestParams(String header) {
        var params = new HashMap<String, String>();
        var matcher = Pattern.compile("(\\w+)=\"?([^\",]+)\"?").matcher(header);
        while (matcher.find()) {
            params.put(matcher.group(1), matcher.group(2));
        }
        return params;
    }

    private String montarDigestAuthorization(String usuario, String senha, String metodo, String uriPath, Map<String, String> params) {
        String realm = params.get("realm");
        String nonce = params.get("nonce");
        String qop = params.get("qop"); // pode não vir (RFC 2069, servidor mais antigo)
        String opaque = params.get("opaque");
        String nc = "00000001";
        String cnonce = gerarCnonce();

        String ha1 = md5(usuario + ":" + realm + ":" + senha);
        String ha2 = md5(metodo + ":" + uriPath);
        String response = qop != null
                ? md5(ha1 + ":" + nonce + ":" + nc + ":" + cnonce + ":" + qop + ":" + ha2)
                : md5(ha1 + ":" + nonce + ":" + ha2);

        var sb = new StringBuilder("Digest ");
        sb.append("username=\"").append(usuario).append("\", ");
        sb.append("realm=\"").append(realm).append("\", ");
        sb.append("nonce=\"").append(nonce).append("\", ");
        sb.append("uri=\"").append(uriPath).append("\", ");
        sb.append("response=\"").append(response).append("\"");
        if (qop != null) {
            sb.append(", qop=").append(qop);
            sb.append(", nc=").append(nc);
            sb.append(", cnonce=\"").append(cnonce).append("\"");
        }
        if (opaque != null) {
            sb.append(", opaque=\"").append(opaque).append("\"");
        }
        return sb.toString();
    }

    private String gerarCnonce() {
        byte[] bytes = new byte[8];
        new SecureRandom().nextBytes(bytes);
        return paraHex(bytes);
    }

    private String md5(String input) {
        try {
            return paraHex(MessageDigest.getInstance("MD5").digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("MD5 indisponível na JVM", e);
        }
    }

    private String paraHex(byte[] bytes) {
        var sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
