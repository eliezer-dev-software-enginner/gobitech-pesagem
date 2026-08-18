package my_app.screens.clienteScreen;

import megalodonte.base.state.State;
import megalodonte.base.UI;
import megalodonte.base.async.Async;
import megalodonte.router.v4.ScreenContext;
import my_app.db.models.ClienteModel;
import my_app.db.services.ClienteService;
import my_app.core.events.EntityEvent;
import my_app.core.events.EventBus;
import my_app.domain.ViewModelScreenContract;
import my_app.domain.components.Components;
import my_app.domain.states.EnderecoState;

public class ClienteViewModel extends ViewModelScreenContract<ClienteModel> {
    private final ClienteService clienteService;

    final State<ClienteModel> clienteSelecionado = State.of(null);

    final State<String> loja = new State<>("");
    final State<String> razaoSocial = new State<>("");
    final State<String> cnpjCpf = new State<>("");
    final State<String> telefone = new State<>("");
    final State<String> complemento = new State<>("");

    final State<EnderecoState> enderecoState = new State<>(new EnderecoState());

    public ClienteViewModel(ScreenContext ctx) {
        super(ctx);
        this.clienteService = createOrReport(ClienteService::new);
    }

    @Override
    protected boolean matchesSearch(ClienteModel model, String query) {
        return contains(model.getLoja(), query)
                || contains(model.getRazaoSocial(), query)
                || contains(model.getCpfCnpj(), query)
                || contains(model.getTelefone(), query);
    }

    private boolean contains(String field, String query) {
        return field != null && field.toLowerCase().contains(query);
    }

    @Override
    public void populateFieldsFromModel() {
        final var data = clienteSelecionado.get();
        if (data == null) return;
        loja.set(data.getLoja());
        razaoSocial.set(data.getRazaoSocial());
        cnpjCpf.set(data.getCpfCnpj() == null ? "" : data.getCpfCnpj());
        telefone.set(data.getTelefone() == null ? "" : data.getTelefone());
        complemento.set(data.getComplemento() == null ? "" : data.getComplemento());
        enderecoState.get().populateFromClienteModel(data);
    }

    @Override
    public ClienteModel populateModelFromFields() {
        var model = modoEdicao.get() && clienteSelecionado.get() != null
                ? clienteSelecionado.get()
                : new ClienteModel();

        model.setLoja(loja.get().trim());
        model.setRazaoSocial(razaoSocial.get().trim());
        model.setCpfCnpj(cnpjCpf.get().trim());
        model.setTelefone(telefone.get().trim());
        model.setComplemento(complemento.get().trim());

        var enderecoStateValue = enderecoState.get();
        model.setCep(enderecoStateValue.cep.get());
        model.setUf(enderecoStateValue.ufSelected.get());
        model.setCidade(enderecoStateValue.cidade.get());
        model.setBairro(enderecoStateValue.bairro.get());
        model.setRua(enderecoStateValue.rua.get());
        model.setNumero(enderecoStateValue.numero.get());

        return model;
    }

    @Override
    public void fetchListData() {
        Async.Run(() -> {
            try {
                var list = clienteService.listar();
                UI.runOnUi(() -> allDataList.set(list));
            } catch (Exception e) {
                e.printStackTrace();
                UI.runOnUi(() -> Components.ShowAlertError("Erro ao buscar clientes: " + e.getMessage()));
            }
        });
    }

    @Override
    public void handleClickMenuDelete() {
        final var model = clienteSelecionado.get();
        if (model == null) return;

        Components.ShowAlertAdvice("Deseja excluir cliente " + model.getLoja(), () -> Async.Run(() -> {
            try {
                clienteService.excluirById(model.getId());
                UI.runOnUi(() -> {
                    allDataList.removeIf(it -> it.getId().equals(model.getId()));
                    Components.ShowPopup(ctx, "Cliente excluído com sucesso");
                    EventBus.getInstance().publish(EntityEvent.excluido(model.getId()));
                });
            } catch (Exception e) {
                UI.runOnUi(() -> Components.ShowAlertError("Erro ao tentar excluir: " + e.getMessage()));
            }
        }));
    }

    @Override
    public void handleAddOrUpdate() {
        if (modoEdicao.get() && clienteSelecionado.get() == null) return;

        // capturado síncrono, antes do Async.Run — ver nota da mesma correção em
        // outras telas (ContratoTelaCrudV3.handleAddOrUpdate reseta modoEdicao logo
        // em seguida, de forma síncrona).
        boolean editando = modoEdicao.get();
        var model = populateModelFromFields();

        Async.Run(() -> {
            try {
                if (editando) {
                    clienteService.atualizar(model);
                    UI.runOnUi(() -> {
                        allDataList.updateIf(it -> it.getId().equals(model.getId()), it -> model);
                        Components.ShowPopup(ctx, "Cliente atualizado com sucesso");
                        clearForm();
                        EventBus.getInstance().publish(EntityEvent.editado(model));
                    });
                } else {
                    clienteService.salvar(model);
                    UI.runOnUi(() -> {
                        allDataList.add(model);
                        Components.ShowPopup(ctx, "Cliente cadastrado com sucesso");
                        clearForm();
                        EventBus.getInstance().publish(EntityEvent.criado(model));
                    });
                }
            } catch (IllegalArgumentException e) {
                UI.runOnUi(() -> Components.ShowAlertError(e.getMessage()));
            } catch (Exception e) {
                UI.runOnUi(() -> Components.ShowAlertError("Erro inesperado: " + e.getMessage()));
            }
        });
    }

    @Override
    public void clearForm() {
        loja.set("");
        razaoSocial.set("");
        cnpjCpf.set("");
        telefone.set("");
        complemento.set("");
        enderecoState.get().clear();
    }

    @Override
    public void onDestroy() throws Exception {
        this.clienteService.close();
    }
}
