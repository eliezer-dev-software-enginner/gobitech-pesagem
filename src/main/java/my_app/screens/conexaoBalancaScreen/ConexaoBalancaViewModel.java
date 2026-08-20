package my_app.screens.conexaoBalancaScreen;

import jssc.SerialPortList;
import megalodonte.ComputedState;
import megalodonte.base.state.State;
import megalodonte.base.UI;
import megalodonte.base.async.Async;
import megalodonte.router.v4.ScreenContext;
import megalodonte.v2.ListState;
import my_app.db.models.ConexaoBalancaModel;
import my_app.db.services.ConexaoBalancaService;
import my_app.domain.components.Components;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import com.fazecast.jSerialComm.SerialPort;

public class ConexaoBalancaViewModel {
    private static final Logger log = LoggerFactory.getLogger(ConexaoBalancaViewModel.class);

    private final ScreenContext ctx;
    private final ConexaoBalancaService conexaoBalancaService;

    public static final List<String> tiposConexaoList = List.of("Serial", "TCP");

    final State<String> tipoConexaoSelected = State.of(tiposConexaoList.getFirst());
    final ListState<String> portasComState = new ListState<>(List.of());
    final State<String> portaComSelected = State.of("");
    final State<String> baudRate = State.of("9600");
    final State<String> ipAddress = State.of("");
    final State<String> ipPort = State.of("");

    public final ComputedState<Boolean> ehSerial = ComputedState.of(
            () -> tipoConexaoSelected.get().equals(tiposConexaoList.getFirst()),
            tipoConexaoSelected
    );

    private ConexaoBalancaModel existente;

    public ConexaoBalancaViewModel(ScreenContext ctx) {
        this.ctx = ctx;
        this.conexaoBalancaService = createOrReport(ConexaoBalancaService::new);
    }

    private static <T> T createOrReport(megalodonte.utils.ThrowingSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            megalodonte.application.ErrorReporter.handle(e);
            throw new IllegalStateException(e);
        }
    }

    public void load() {
        Async.Run(() -> {
            try {
                String[] portNames = SerialPortList.getPortNames();
                UI.runOnUi(() -> {
                    for (String name : portNames) portasComState.add(name);
                });

//                SerialPort[] ports = SerialPort.getCommPorts();
//                System.out.println("Portas encontradas: " + ports.length);
//                for (SerialPort port : ports) {
//                    String name = port.getSystemPortName() + " - " + port.getDescriptivePortName();
//                    System.out.println(name);
//                }
            } catch (Throwable e) {
                log.error("Erro ao carregar portas seriais: {}", e.getMessage(), e);
                UI.runOnUi(() -> Components.ShowAlertError("Erro ao carregar portas seriais: " + e.getMessage()));
            }

            try {
                existente = conexaoBalancaService.buscarUnico();
                if (existente != null) {
                    UI.runOnUi(() -> {
                        tipoConexaoSelected.set(existente.getTipoConexao());
                        portaComSelected.set(existente.getPortaCom() == null ? "" : existente.getPortaCom());
                        baudRate.set(existente.getBaudRate() == null ? "9600" : String.valueOf(existente.getBaudRate()));
                        ipAddress.set(existente.getIpAddress() == null ? "" : existente.getIpAddress());
                        ipPort.set(existente.getIpPort() == null ? "" : String.valueOf(existente.getIpPort()));
                    });
                }
            } catch (Exception e) {
                log.error("Erro ao carregar conexão da balança", e);
                UI.runOnUi(() -> Components.ShowAlertError("Erro ao carregar conexão da balança: " + e.getMessage()));
            }
        });
    }

    public void salvar() {
        var model = new ConexaoBalancaModel();
        model.setTipoConexao(tipoConexaoSelected.get());

        if (ehSerial.get()) {
            model.setPortaCom(portaComSelected.get());
            model.setBaudRate(parseIntOrNull(baudRate.get()));
        } else {
            model.setIpAddress(ipAddress.get());
            model.setIpPort(parseIntOrNull(ipPort.get()));
        }

        Async.Run(() -> {
            try {
                conexaoBalancaService.salvarOuAtualizar(model);
                UI.runOnUi(() -> Components.ShowPopup(ctx, "Conexão da balança salva com sucesso"));
            } catch (IllegalArgumentException e) {
                UI.runOnUi(() -> Components.ShowAlertError(e.getMessage()));
            } catch (Exception e) {
                log.error("Erro ao salvar conexão da balança", e);
                UI.runOnUi(() -> Components.ShowAlertError("Erro ao salvar: " + e.getMessage()));
            }
        });
    }

    private Integer parseIntOrNull(String value) {
        try {
            return value == null || value.isBlank() ? null : Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public void onDestroy() throws Exception {
        this.conexaoBalancaService.close();
    }
}
