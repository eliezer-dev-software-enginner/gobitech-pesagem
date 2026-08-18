package my_app.db.services;

import my_app.db.DB;
import my_app.db.models.UsuarioModel;
import my_app.db.repositories.UsuarioRepository;
import my_app.security.CryptoManager;
import net.sf.persism.Session;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Login e senha ficam sempre criptografados em repouso (coluna {@code login}/{@code senha} da
 * tabela {@code usuarios}) — nunca em texto puro. Essa classe é a única fronteira que conhece
 * {@link CryptoManager}: quem chama (ViewModels/telas) sempre manda e recebe texto puro,
 * {@code UsuarioRepository} sempre lê/grava o que já está no banco (criptografado) sem saber
 * disso. Como a criptografia é determinística (AES/ECB), dá pra comparar/buscar por igualdade
 * de texto cifrado sem nunca precisar decriptar o que está armazenado.
 */
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
        validarCamposObrigatorios(model);
        if (model.getAtivo() == null) model.setAtivo(true);
        if (model.getAdmin() == null) model.setAdmin(false);
        model.setDataCriacao(LocalDateTime.now());

        var crypto = new CryptoManager();
        String loginPlain = model.getLogin();
        String senhaPlain = model.getSenha();
        String loginEnc = crypto.encrypt(loginPlain);

        if (usuarioRepository.buscarPorLogin(loginEnc) != null)
            throw new IllegalArgumentException("Login já em uso por outro usuário");

        model.setLogin(loginEnc);
        model.setSenha(crypto.encrypt(senhaPlain));
        try {
            var salvo = repository.salvar(model);
            salvo.setLogin(loginPlain);
            salvo.setSenha(senhaPlain);
            return salvo;
        } finally {
            model.setLogin(loginPlain);
            model.setSenha(senhaPlain);
        }
    }

    @Override
    public void atualizar(UsuarioModel model) throws SQLException {
        validarCamposObrigatorios(model);

        var crypto = new CryptoManager();
        String loginPlain = model.getLogin();
        String senhaPlain = model.getSenha();
        String loginEnc = crypto.encrypt(loginPlain);

        var existente = usuarioRepository.buscarPorLogin(loginEnc);
        if (existente != null && !existente.getId().equals(model.getId()))
            throw new IllegalArgumentException("Login já em uso por outro usuário");

        model.setLogin(loginEnc);
        model.setSenha(crypto.encrypt(senhaPlain));
        try {
            repository.atualizar(model);
        } finally {
            model.setLogin(loginPlain);
            model.setSenha(senhaPlain);
        }
    }

    public void inativar(long id) throws SQLException {
        var usuario = repository.buscarById(id);
        if (usuario == null) throw new IllegalArgumentException("Usuário não encontrado");
        usuario.setAtivo(false);
        repository.atualizar(usuario);
    }

    /**
     * Autentica pelo login informado — nunca contra um login fixo/hardcoded. {@code login} e
     * {@code senha} chegam em texto puro (o que o usuário digitou); são criptografados aqui pra
     * comparar contra o que está gravado, sem nunca precisar decriptar a senha armazenada.
     */
    public UsuarioModel autenticar(String login, String senha) throws SQLException {
        var crypto = new CryptoManager();
        String loginEnc = crypto.encrypt(login);
        String senhaEnc = crypto.encrypt(senha);

        var usuario = usuarioRepository.buscarPorLogin(loginEnc);
        if (usuario == null || !Boolean.TRUE.equals(usuario.getAtivo())) return null;
        if (usuario.getSenha() == null || !usuario.getSenha().equals(senhaEnc)) return null;

        usuario.setLogin(login);
        usuario.setSenha(senha);
        return usuario;
    }

    public List<UsuarioModel> listarAtivos() throws SQLException {
        var lista = usuarioRepository.listarAtivos();
        var crypto = new CryptoManager();
        for (var usuario : lista) {
            usuario.setLogin(crypto.decrypt(usuario.getLogin()));
            usuario.setSenha(crypto.decrypt(usuario.getSenha()));
        }
        return lista;
    }

    /**
     * @param login texto puro — criptografado internamente pra buscar.
     */
    public UsuarioModel buscarPorLogin(String login) throws SQLException {
        var crypto = new CryptoManager();
        var usuario = usuarioRepository.buscarPorLogin(crypto.encrypt(login));
        if (usuario == null) return null;
        usuario.setLogin(login);
        usuario.setSenha(crypto.decrypt(usuario.getSenha()));
        return usuario;
    }

    private void validarCamposObrigatorios(UsuarioModel model) {
        if (model.getLogin() == null || model.getLogin().isBlank())
            throw new IllegalArgumentException("Login é obrigatório");
        if (model.getSenha() == null || model.getSenha().isBlank())
            throw new IllegalArgumentException("Senha é obrigatória");
        if (model.getNome() == null || model.getNome().isBlank())
            throw new IllegalArgumentException("Nome é obrigatório");
    }
}
