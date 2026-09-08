package my_app.infra.balanca;

import jssc.SerialPort;
import jssc.SerialPortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.function.Consumer;

/**
 * Lê o peso de um indicador de balança conectado via porta serial (RS-232), usando JSSC —
 * a mesma biblioteca já usada no plics-sw pra impressora térmica, mas aqui pra ler um
 * dispositivo em vez de escrever num.
 */
public class LeitorBalancaSerial implements LeitorBalanca {

    private static final Logger log = LoggerFactory.getLogger(LeitorBalancaSerial.class);

    private final String portaCom;
    private final int baudRate;
    private SerialPort serialPort;

    public LeitorBalancaSerial(String portaCom, int baudRate) {
        this.portaCom = portaCom;
        this.baudRate = baudRate;
    }

    @Override
    public void iniciar(Consumer<BigDecimal> onPeso, Consumer<String> onErro) {
        log.info("Conectando na balança via Serial: {} ({} baud)", portaCom, baudRate);
        try {
            serialPort = new SerialPort(portaCom);
            serialPort.openPort();
            serialPort.setParams(baudRate, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
            log.info("Conectado na balança via Serial: {}", portaCom);
            serialPort.addEventListener(event -> {
                if (!event.isRXCHAR() || event.getEventValue() <= 0) return;
                try {
                    byte[] bytes = serialPort.readBytes(event.getEventValue());
                    var peso = PesoParser.parse(new String(bytes));
                    if (peso != null) onPeso.accept(peso);
                } catch (SerialPortException e) {
                    log.error("Erro ao ler porta {}", portaCom, e);
                    onErro.accept("Falha na comunicação com a balança na porta " + portaCom + ".");
                }
            });
        } catch (SerialPortException e) {
            log.error("Erro ao abrir porta {}", portaCom, e);
            onErro.accept("Não foi possível abrir a porta " + portaCom + ". Verifique o cabo e se nenhum outro programa está usando a balança.");
        }
    }

    @Override
    public void parar() {
        if (serialPort == null || !serialPort.isOpened()) return;
        log.info("Encerrando conexão Serial com a balança: {}", portaCom);
        try {
            serialPort.removeEventListener();
            serialPort.closePort();
        } catch (SerialPortException e) {
            log.warn("Erro ao fechar porta {}", portaCom, e);
        }
    }
}
