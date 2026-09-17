package my_app.infra.balanca;

import megalodonte.base.UI;
import megalodonte.base.state.State;
import my_app.db.services.ConexaoBalancaService;
import my_app.domain.components.Components;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

/**
 * Conexão única e compartilhada com a balança (singleton).
 * <p>
 * Antes desta refatoração, cada ViewModel ({@code DashboardViewModel} e
 * {@code PesagemFormViewModel}) abria sua própria conexão — causava conflito quando
 * a Dashboard mantinha a conexão aberta e a tela de Pesagem (aberta em nova janela via
 * {@code spawnWindow}) tentava abrir uma segunda conexão pro mesmo dispositivo.
 * Serial: porta COM é exclusiva → {@code openPort()} falhava. TCP: muitos conversores
 * serial-Ethernet aceitam um cliente só → segunda conexão recusada.
 * <p>
 * Agora existe uma única conexão. Tanto a Dashboard quanto as telas de Pesagem leem o
 * mesmo {@link #pesoAoVivo} e {@link #lendoBalanca}. A conexão é aberta na primeira
 * chamada de {@link #iniciar()} e só é fechada no encerramento da aplicação
 * ({@link #parar()}) ou quando o usuário troca a config da balança
 * ({@link #reconectar()}).
 */
public class BalancaService {

    private static final Logger log = LoggerFactory.getLogger(BalancaService.class);
    private static final BalancaService INSTANCE = new BalancaService();

    private final State<String> pesoAoVivo = State.of("—");
    private final State<Boolean> lendoBalanca = State.of(false);

    private volatile LeitorBalanca leitor;

    private BalancaService() {}

    public static BalancaService getInstance() {
        return INSTANCE;
    }

    public State<String> pesoAoVivo() {
        return pesoAoVivo;
    }

    public State<Boolean> lendoBalanca() {
        return lendoBalanca;
    }

    /** Liga a leitura contínua da balança. Seguro chamar mais de uma vez — se já está lendo, é no-op. */
    public synchronized void iniciar() {
        if (leitor != null) return;

        try (var conexaoService = new ConexaoBalancaService()) {
            var config = conexaoService.buscarUnico();
            var leitorNovo = LeitorBalancaFactory.criar(config);
            this.leitor = leitorNovo;

            leitorNovo.iniciar(
                    peso -> UI.runOnUi(() -> {
                        pesoAoVivo.set(arrInt(peso));
                        lendoBalanca.set(true);
                    }),
                    erro -> {
                        log.warn("Erro na leitura da balança: {}", erro);
                        // Descarta o leitor com defeito pra próxima montagem de tela reconectar
                        // (senão a conexão morta seguraria o singleton até reiniciar o app).
                        // O cleanup roda no UI thread, fora do thread do listener (evita deadlock no jSSC).
                        UI.runOnUi(() -> {
                            limparLeitor();
                            lendoBalanca.set(false);
                            Components.ShowAlertError(erro);
                        });
                    }
            );
            log.info("Leitura da balança iniciada (singleton)");
        } catch (Exception e) {
            log.error("Erro ao conectar com a balança", e);
            UI.runOnUi(() -> Components.ShowAlertError("Não foi possível conectar com a balança."));
        }
    }

    /** Para a leitura e fecha a conexão. */
    public synchronized void parar() {
        if (leitor != null) {
            log.info("Parando leitura da balança (singleton)");
            leitor.parar();
            leitor = null;
        }
        lendoBalanca.set(false);
    }

    /** Para, reconecta com a config atual do banco (ex.: após salvar nova conexão na tela de configuração). */
    public synchronized void reconectar() {
        parar();
        iniciar();
    }

    /** Descarta o leitor atual (já com defeito) pra próxima {@link #iniciar()} reconectar. */
    private synchronized void limparLeitor() {
        if (leitor != null) {
            leitor.parar();
            leitor = null;
        }
    }

    /** Converte um peso pra inteiro (sem casas decimais), como o André prefere. */
    private String arrInt(BigDecimal valor) {
        var inteiro = PesagemCalculo.arredondarInteiro(valor);
        return inteiro == null ? "" : inteiro.toBigInteger().toString();
    }
}
