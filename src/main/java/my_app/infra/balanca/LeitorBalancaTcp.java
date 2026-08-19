package my_app.infra.balanca;

import megalodonte.base.async.Async;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.math.BigDecimal;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.function.Consumer;

/**
 * Lê o peso de um indicador de balança conectado via rede (conversor serial-Ethernet), abrindo
 * um socket TCP cru e lendo o que a balança transmitir continuamente.
 * <p>
 * Sem handshake Telnet nem comando nenhum enviado de propósito: "Hércules" (citado no requisito
 * original da especificação) é só um terminal genérico de teste que se conecta assim — ver
 * evidência 7 em {@code /home/eliezer/Desktop/dev/outros/balanca-gobitech/docs/DECISIONS.md}.
 * O app antigo mandava um comando `"ls\n"` de shell Unix pra balança (claramente copiado de
 * algum tutorial de Telnet sem adaptar) — a maioria dos indicadores nem espera receber nada,
 * só transmite o peso continuamente, então esta versão não manda comando nenhum.
 */
public class LeitorBalancaTcp implements LeitorBalanca {

    private static final Logger log = LoggerFactory.getLogger(LeitorBalancaTcp.class);

    private final String ip;
    private final int porta;
    private volatile boolean rodando = false;
    private Socket socket;

    public LeitorBalancaTcp(String ip, int porta) {
        this.ip = ip;
        this.porta = porta;
    }

    @Override
    public void iniciar(Consumer<BigDecimal> onPeso, Consumer<String> onErro) {
        rodando = true;
        log.info("Conectando na balança via TCP: {}:{}", ip, porta);
        Async.Run(() -> {
            try {
                socket = new Socket(ip, porta);
                socket.setSoTimeout(5000);
                log.info("Conectado na balança via TCP: {}:{}", ip, porta);
                InputStream in = socket.getInputStream();
                byte[] buffer = new byte[256];

                while (rodando) {
                    try {
                        int lidos = in.read(buffer);
                        if (lidos == -1) break; // conexão fechada do outro lado

                        var peso = PesoParser.parse(new String(buffer, 0, lidos));
                        if (peso != null) onPeso.accept(peso);
                    } catch (SocketTimeoutException semDadoNovo) {
                        // balança não mandou nada nesse intervalo — só continua esperando
                    }
                }
            } catch (Exception e) {
                if (rodando) {
                    log.error("Erro na conexão TCP com a balança ({}:{})", ip, porta, e);
                    onErro.accept("Erro na conexão TCP com a balança (" + ip + ":" + porta + "): " + e.getMessage());
                }
            }
        });
    }

    @Override
    public void parar() {
        rodando = false;
        log.info("Encerrando conexão TCP com a balança: {}:{}", ip, porta);
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (Exception e) {
            log.warn("Erro ao fechar socket da balança ({}:{})", ip, porta, e);
        }
    }
}
