package my_app.infra.balanca;

import jssc.SerialPort;
import jssc.SerialPortException;

import java.math.BigDecimal;
import java.util.function.Consumer;

/**
 * Lê o peso de um indicador de balança conectado via porta serial (RS-232), usando JSSC —
 * a mesma biblioteca já usada no plics-sw pra impressora térmica, mas aqui pra ler um
 * dispositivo em vez de escrever num.
 */
public class LeitorBalancaSerial implements LeitorBalanca {

    private final String portaCom;
    private final int baudRate;
    private SerialPort serialPort;

    public LeitorBalancaSerial(String portaCom, int baudRate) {
        this.portaCom = portaCom;
        this.baudRate = baudRate;
    }

    @Override
    public void iniciar(Consumer<BigDecimal> onPeso, Consumer<String> onErro) {
        try {
            serialPort = new SerialPort(portaCom);
            serialPort.openPort();
            serialPort.setParams(baudRate, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
            serialPort.addEventListener(event -> {
                if (!event.isRXCHAR() || event.getEventValue() <= 0) return;
                try {
                    byte[] bytes = serialPort.readBytes(event.getEventValue());
                    var peso = PesoParser.parse(new String(bytes));
                    if (peso != null) onPeso.accept(peso);
                } catch (SerialPortException e) {
                    onErro.accept("Erro ao ler porta " + portaCom + ": " + e.getMessage());
                }
            });
        } catch (SerialPortException e) {
            onErro.accept("Erro ao abrir porta " + portaCom + ": " + e.getMessage());
        }
    }

    @Override
    public void parar() {
        if (serialPort == null || !serialPort.isOpened()) return;
        try {
            serialPort.removeEventListener();
            serialPort.closePort();
        } catch (SerialPortException ignored) {
        }
    }
}
