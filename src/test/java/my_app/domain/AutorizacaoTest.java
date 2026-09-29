package my_app.domain;

import my_app.db.models.UsuarioModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AutorizacaoTest {
    @AfterEach
    void limparSessao() {
        SessaoUsuario.logout();
    }

    @Test
    void operadorNaoPodeAdministrar() {
        var operador = new UsuarioModel();
        operador.setId(10);
        operador.setAdmin(false);
        SessaoUsuario.login(operador);

        assertFalse(Autorizacao.podeAdministrar());
        assertThrows(IllegalArgumentException.class,
                () -> Autorizacao.exigirAdmin("executar esta ação"));
    }

    @Test
    void adminPodeAdministrarEUsuarioLogadoEhIdentificado() {
        var admin = new UsuarioModel();
        admin.setId(20);
        admin.setAdmin(true);
        SessaoUsuario.login(admin);

        assertTrue(Autorizacao.podeAdministrar());
        assertDoesNotThrow(() -> Autorizacao.exigirAdmin("executar esta ação"));
        assertTrue(Autorizacao.usuarioLogado(20));
        assertFalse(Autorizacao.usuarioLogado(21));
    }
}
