package my_app.infra.balanca;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testa o LeitorBalancaTcp contra um servidor fake de balança (ServerSocket em loopback, sem lib
 * nova — mesmo padrão do fake server da câmera): o "indicador" aceita a conexão e fica
 * transmitindo o peso continuamente, como um conversor serial-Ethernet real faz.
 */
class LeitorBalancaTcpTest {

    @Test
    void deveRepassarPesoLidoDoServidorFake() throws Exception {
        try (var serverSocket = new ServerSocket(0, 1, java.net.InetAddress.getByName("127.0.0.1"))) {
            int porta = serverSocket.getLocalPort();

            Thread servidor = new Thread(() -> {
                try (var socket = serverSocket.accept()) {
                    socket.getOutputStream().write("ST,GS, 250.5 kg\n".getBytes(StandardCharsets.UTF_8));
                    socket.getOutputStream().flush();
                    // mantém aberto pra simular transmissão contínua; o teste chama parar()
                    Thread.sleep(3000);
                } catch (Exception ignored) {
                }
            });
            servidor.setDaemon(true);
            servidor.start();

            var pesoRecebido = new AtomicReference<BigDecimal>();
            var erros = new AtomicReference<String>();
            var latch = new CountDownLatch(1);

            var leitor = new LeitorBalancaTcp("127.0.0.1", porta);
            leitor.iniciar(
                    peso -> {
                        if (pesoRecebido.compareAndSet(null, peso)) latch.countDown();
                    },
                    erro -> erros.set(erro));

            try {
                assertTrue(latch.await(5, TimeUnit.SECONDS), "peso não chegou em 5s (erros: " + erros.get() + ")");
                assertNotNull(erros.get() == null ? pesoRecebido.get() : null);
                assertEquals(new BigDecimal("250.5"), pesoRecebido.get());
            } finally {
                leitor.parar();
            }
        }
    }

    @Test
    void deveNotificarErroQuandoConexaoForRecusada() throws Exception {
        // Porta efêmera liberada na hora: loopback pronto pra recusar conexão.
        int porta;
        try (var socket = new ServerSocket(0, 1, java.net.InetAddress.getByName("127.0.0.1"))) {
            porta = socket.getLocalPort();
        }

        var erros = new AtomicReference<String>();
        var latch = new CountDownLatch(1);

        var leitor = new LeitorBalancaTcp("127.0.0.1", porta);
        leitor.iniciar(peso -> {
        }, erro -> {
            erros.set(erro);
            latch.countDown();
        });

        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS), "onErro não foi chamado em 5s");
            assertNotNull(erros.get());
            assertTrue(erros.get().contains(porta + ""));
        } finally {
            leitor.parar();
        }
    }
}