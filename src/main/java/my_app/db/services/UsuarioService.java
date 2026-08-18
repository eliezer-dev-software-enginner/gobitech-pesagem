package my_app.db.services;

import my_app.db.DB;
import my_app.db.models.UsuarioModel;
import my_app.db.repositories.UsuarioRepository;
import net.sf.persism.Session;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class UsuarioService extends BaseService<UsuarioModel> {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService() throws SQLException {
        this(DB.getPersismSession());
    }

    public UsuarioService(Session session) {
        super(new UsuarioRepository(session));
        this.usuarioRepository = (UsuarioRepository) repository;
    }

    @Override
    public UsuarioModel salvar(UsuarioModel model) throws SQLException {
        validarCampos(model);
        if (model.getAtivo() == null) model.setAtivo(true);
        if (model.getAdmin() == null) model.setAdmin(false);
        model.setDataCriacao(LocalDateTime.now());
        return repository.salvar(model);
    }

    @Override
    public void atualizar(UsuarioModel model) throws SQLException {
        validarCampos(model);
        repository.atualizar(model);
    }

    public void inativar(long id) throws SQLException {
        var usuario = repository.buscarById(id);
        if (usuario == null) throw new IllegalArgumentException("Usuário não encontrado");
        usuario.setAtivo(false);
        repository.atualizar(usuario);
    }

    /**
     * Autentica pelo login informado — nunca contra um login fixo/hardcoded.
     */
    public UsuarioModel autenticar(String login, String senha) throws SQLException {
        var usuario = usuarioRepository.buscarPorLogin(login);
        if (usuario == null || !Boolean.TRUE.equals(usuario.getAtivo())) return null;
        if (usuario.getSenha() == null || !usuario.getSenha().equals(senha)) return null;
        return usuario;
    }

    public List<UsuarioModel> listarAtivos() throws SQLException {
        return usuarioRepository.listarAtivos();
    }

    public UsuarioModel buscarPorLogin(String login) throws SQLException {
        return usuarioRepository.buscarPorLogin(login);
    }

    private void validarCampos(UsuarioModel model) throws SQLException {
        if (model.getLogin() == null || model.getLogin().isBlank())
            throw new IllegalArgumentException("Login é obrigatório");
        if (model.getSenha() == null || model.getSenha().isBlank())
            throw new IllegalArgumentException("Senha é obrigatória");
        if (model.getNome() == null || model.getNome().isBlank())
            throw new IllegalArgumentException("Nome é obrigatório");

        var existente = usuarioRepository.buscarPorLogin(model.getLogin());
        if (existente != null && !existente.getId().equals(model.getId()))
            throw new IllegalArgumentException("Login já em uso por outro usuário");
    }
}
