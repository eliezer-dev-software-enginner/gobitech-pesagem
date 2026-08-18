package my_app.screens.empresaScreen;

import javafx.stage.FileChooser;
import megalodonte.base.state.State;
import megalodonte.base.UI;
import megalodonte.base.async.Async;
import megalodonte.router.v4.ScreenContext;
import my_app.db.models.EmpresaModel;
import my_app.db.services.EmpresaService;
import my_app.domain.Data;
import my_app.domain.components.Components;

import java.io.File;

public class EmpresaViewModel {
    private final ScreenContext ctx;
    private final EmpresaService empresaService;

    final State<String> nome = State.of("");
    final State<String> cpfCnpj = State.of("");
    final State<String> telefone = State.of("");
    final State<String> email = State.of("");
    final State<String> logoMarca = State.of("/logo_256x256.png");

    final State<String> cep = State.of("");
    final State<String> cidade = State.of("");
    final State<String> estadoSelected = State.of(Data.ufList.getFirst());
    final State<String> bairro = State.of("");
    final State<String> rua = State.of("");
    final State<String> numero = State.of("");

    public EmpresaViewModel(ScreenContext ctx) {
        this.ctx = ctx;
        this.empresaService = createOrReport(EmpresaService::new);
    }

    private static <T> T createOrReport(megalodonte.utils.ThrowingSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            megalodonte.application.ErrorReporter.handle(e);
            throw new IllegalStateException(e);
        }
    }

    public void fetchData() {
        Async.Run(() -> {
            try {
                var model = empresaService.buscarUnico();
                if (model != null) {
                    UI.runOnUi(() -> {
                        nome.set(model.getNome() == null ? "" : model.getNome());
                        cpfCnpj.set(model.getCpfCnpj() == null ? "" : model.getCpfCnpj());
                        telefone.set(model.getTelefone() == null ? "" : model.getTelefone());
                        email.set(model.getEmail() == null ? "" : model.getEmail());
                        logoMarca.set(model.getLogomarca() != null ? model.getLogomarca() : "/logo_256x256.png");
                        cep.set(model.getCep() == null ? "" : model.getCep());
                        cidade.set(model.getCidade() == null ? "" : model.getCidade());
                        estadoSelected.set(model.getEstado() == null ? Data.ufList.getFirst() : model.getEstado());
                        bairro.set(model.getBairro() == null ? "" : model.getBairro());
                        rua.set(model.getRua() == null ? "" : model.getRua());
                        numero.set(model.getNumero() == null ? "" : model.getNumero());
                    });
                }
            } catch (Exception e) {
                throw new RuntimeException("Erro ao carregar dados da empresa", e);
            }
        });
    }

    public void handleUpdateLogoMarca() {
        var fileChooser = new FileChooser();
        fileChooser.setTitle("Selecionar imagem");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Imagens", "*.png", "*.jpg", "*.jpeg"));

        File arquivo = fileChooser.showOpenDialog(this.ctx.selfStage());
        if (arquivo != null) {
            logoMarca.set(arquivo.toURI().toString());
        }
    }

    public void handleSave() {
        var model = new EmpresaModel();
        model.setNome(nome.get());
        model.setCpfCnpj(cpfCnpj.get());
        model.setTelefone(telefone.get());
        model.setEmail(email.get());
        model.setLogomarca(logoMarca.get());
        model.setCep(cep.get());
        model.setCidade(cidade.get());
        model.setEstado(estadoSelected.get());
        model.setBairro(bairro.get());
        model.setRua(rua.get());
        model.setNumero(numero.get());

        Async.Run(() -> {
            try {
                empresaService.salvarOuAtualizar(model);
                UI.runOnUi(() -> Components.ShowPopup(ctx, "Empresa atualizada com sucesso"));
            } catch (Exception e) {
                UI.runOnUi(() -> Components.ShowAlertError(e.getMessage()));
            }
        });
    }

    public void onDestroy() throws Exception {
        this.empresaService.close();
    }
}
