package my_app.db.services;

import my_app.db.models.UsuarioModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UsuarioServiceTest extends BaseServiceTest {

    private UsuarioService usuarioService;

    @Override
    protected void initService() {
        usuarioService = new UsuarioService(session);
    }

    private UsuarioModel usuarioValido() {
        var u = new UsuarioModel();
        u.setLogin("maria");
        u.setSenha("1234");
        u.setNome("Maria Silva");
        return u;
    }

    @Test
    void deveLancarExcecaoQuandoLoginVazio() {
        var u = usuarioValido();
        u.setLogin("");
        assertThrows(IllegalArgumentException.class, () -> usuarioService.salvar(u));
    }

    @Test
    void deveLancarExcecaoQuandoSenhaVazia() {
        var u = usuarioValido();
        u.setSenha("");
        assertThrows(IllegalArgumentException.class, () -> usuarioService.salvar(u));
    }

    @Test
    void deveLancarExcecaoQuandoNomeVazio() {
        var u = usuarioValido();
        u.setNome("");
        assertThrows(IllegalArgumentException.class, () -> usuarioService.salvar(u));
    }

    @Test
    void deveSalvarUsuarioComoAtivoENaoAdminPorPadrao() throws Exception {
        var salvo = usuarioService.salvar(usuarioValido());
        assertNotNull(salvo.getId());
        assertEquals(true, salvo.getAtivo());
        assertEquals(false, salvo.getAdmin());
    }

    @Test
    void deveLancarExcecaoQuandoLoginDuplicado() throws Exception {
        usuarioService.salvar(usuarioValido());

        var u2 = usuarioValido();
        u2.setNome("Outro Nome");
        assertThrows(IllegalArgumentException.class, () -> usuarioService.salvar(u2));
    }

    @Test
    void devePermitirAtualizarUsuarioMantendoLogin() throws Exception {
        var salvo = usuarioService.salvar(usuarioValido());
        salvo.setNome("Maria Editada");
        assertDoesNotThrow(() -> usuarioService.atualizar(salvo));
    }

    @Test
    void deveAutenticarComCredenciaisCorretas() throws Exception {
        usuarioService.salvar(usuarioValido());
        var autenticado = usuarioService.autenticar("maria", "1234");
        assertNotNull(autenticado);
        assertEquals("Maria Silva", autenticado.getNome());
    }

    @Test
    void naoDeveAutenticarComSenhaErrada() throws Exception {
        usuarioService.salvar(usuarioValido());
        assertNull(usuarioService.autenticar("maria", "senhaErrada"));
    }

    @Test
    void naoDeveAutenticarLoginInexistente() throws Exception {
        assertNull(usuarioService.autenticar("não existe", "1234"));
    }

    @Test
    void naoDeveAutenticarUsuarioInativo() throws Exception {
        usuarioService.salvar(usuarioValido());
        usuarioService.inativar(usuarioService.buscarPorLogin("maria").getId());
        assertNull(usuarioService.autenticar("maria", "1234"));
    }

    @Test
    void deveInativarUsuario() throws Exception {
        var salvo = usuarioService.salvar(usuarioValido());
        usuarioService.inativar(salvo.getId());
        var buscado = usuarioService.buscarById(salvo.getId());
        assertEquals(false, buscado.getAtivo());
    }

    @Test
    void deveLancarExcecaoAoInativarUsuarioInexistente() {
        assertThrows(IllegalArgumentException.class, () -> usuarioService.inativar(9999));
    }

    @Test
    void deveRetornarLoginESenhaEmTextoPuroAoBuscarPorId() throws Exception {
        var salvo = usuarioService.salvar(usuarioValido());
        var buscado = usuarioService.buscarById(salvo.getId());
        assertEquals("maria", buscado.getLogin());
        assertEquals("1234", buscado.getSenha());
    }
}
