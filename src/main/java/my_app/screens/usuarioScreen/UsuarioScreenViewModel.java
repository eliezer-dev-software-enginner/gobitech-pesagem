package my_app.screens.usuarioScreen;

import megalodonte.base.state.State;
import megalodonte.base.UI;
import megalodonte.base.async.Async;
import megalodonte.router.v4.ScreenContext;
import my_app.core.AppRoutes;
import my_app.db.models.UsuarioModel;
import my_app.db.services.UsuarioService;
import my_app.core.events.UsuarioEvent;
import my_app.core.events.EventBus;
import my_app.domain.Data;
import my_app.domain.ViewModelScreenContract;
import my_app.domain.components.Components;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class UsuarioScreenViewModel extends ViewModelScreenContract<UsuarioModel> {
    private static final Logger log = LoggerFactory.getLogger(UsuarioScreenViewModel.class);

    private final UsuarioService usuarioService;

    @SuppressWarnings("rawtypes")
    private final Consumer<Object> eventListener = this::onEntityEvent;

    final State<String> login = new State<>("");
    final State<String> senha = new State<>("");
    final State<String> nome = new State<>("");
    final State<String> telefone = new State<>("");
    final State<String> ehAdminSelected = new State<>(Data.simNaoList.getLast());

    public UsuarioScreenViewModel(ScreenContext ctx) {
        super(ctx);
        this.usuarioService = createOrReport(UsuarioService::new);
        screenNameSpawn = AppRoutes.Screens.ADD_OR_EDIT_USUARIO.name();

        EventBus.getInstance().subscribe(eventListener);
    }

    private void onEntityEvent(Object event) {
        if (event instanceof UsuarioEvent) {
            fetchListData();
        }
    }

    @Override
    protected boolean matchesSearch(UsuarioModel model, String query) {
        return contains(model.getLogin(), query) || contains(model.getNome(), query);
    }

    private boolean contains(String field, String query) {
        return field != null && field.toLowerCase().contains(query);
    }

    @Override
    public void populateFieldsFromModel() {
        final var data = selected.get();
        if (data == null) return;
        login.set(data.getLogin());
        senha.set(data.getSenha());
        nome.set(data.getNome());
        telefone.set(data.getTelefone() == null ? "" : data.getTelefone());
        ehAdminSelected.set(Boolean.TRUE.equals(data.getAdmin()) ? Data.simNaoList.getFirst() : Data.simNaoList.getLast());
    }

    @Override
    public UsuarioModel populateModelFromFields() {
        var model = modoEdicao.get() && selected.get() != null
                ? selected.get()
                : new UsuarioModel();

        model.setLogin(login.get().trim());
        model.setSenha(senha.get().trim());
        model.setNome(nome.get().trim());
        model.setTelefone(telefone.get().trim());
        model.setAdmin(ehAdminSelected.get().equals(Data.simNaoList.getFirst()));

        return model;
    }

    @Override
    public void fetchListData() {
        Async.Run(() -> {
            try {
                var list = usuarioService.listarAtivos();
                UI.runOnUi(() -> allDataList.set(list));
            } catch (Exception e) {
                log.error("Erro ao buscar usuários", e);
                UI.runOnUi(() -> Components.ShowAlertError("Erro ao buscar usuários: " + e.getMessage()));
            }
        });
    }

    @Override
    public void handleClickMenuDelete() {
        final var model = selected.get();
        if (model == null) return;

        Components.ShowAlertAdvice("Deseja inativar o usuário " + model.getNome(), () -> Async.Run(() -> {
            try {
                usuarioService.inativar(model.getId());
                UI.runOnUi(() -> {
                    allDataList.removeIf(it -> it.getId().equals(model.getId()));
                    Components.ShowPopup(ctx, "Usuário inativado com sucesso");
                    EventBus.getInstance().publish(UsuarioEvent.excluido());
                });
            } catch (Exception e) {
                log.error("Erro ao inativar usuário id={}", model.getId(), e);
                UI.runOnUi(() -> Components.ShowAlertError("Erro ao tentar inativar: " + e.getMessage()));
            }
        }));
    }

    @Override
    public void handleAddOrUpdate() {
        if (modoEdicao.get() && selected.get() == null) return;
        if (!tryBeginSalvar()) return;

        boolean editando = modoEdicao.get();
        var model = populateModelFromFields();

        Async.Run(() -> {
            try {
                if (editando) {
                    usuarioService.atualizar(model);
                    UI.runOnUi(() -> {
                        allDataList.updateIf(it -> it.getId().equals(model.getId()), it -> model);
                        Components.ShowPopup(ctx, "Usuário atualizado com sucesso");
                        EventBus.getInstance().publish(UsuarioEvent.editado());
                    });
                } else {
                    usuarioService.salvar(model);
                    UI.runOnUi(() -> {
                        allDataList.add(model);
                        Components.ShowPopup(ctx, "Usuário cadastrado com sucesso");
                        EventBus.getInstance().publish(UsuarioEvent.criado());
                    });
                }
            } catch (IllegalArgumentException e) {
                UI.runOnUi(() -> Components.ShowAlertError(e.getMessage()));
            } catch (Exception e) {
                log.error("Erro inesperado ao salvar usuário", e);
                UI.runOnUi(() -> Components.ShowAlertError("Erro inesperado: " + e.getMessage()));
            } finally {
                endSalvar();
            }
        });
    }

    @Override
    public void clearForm() {
        login.set("");
        senha.set("");
        nome.set("");
        telefone.set("");
        ehAdminSelected.set(Data.simNaoList.getLast());
    }

    @Override
    public void onDestroy() throws Exception {
        this.usuarioService.close();
        EventBus.getInstance().unsubscribe(eventListener);
    }
}
