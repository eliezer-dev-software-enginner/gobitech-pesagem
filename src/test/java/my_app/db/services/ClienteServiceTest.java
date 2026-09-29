package my_app.db.services;

import my_app.db.models.ClienteModel;
import my_app.db.models.UsuarioModel;
import my_app.domain.SessaoUsuario;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClienteServiceTest extends BaseServiceTest {

    private ClienteService clienteService;

    @Override
    protected void initService() {
        clienteService = new ClienteService(session);
    }

    private ClienteModel clienteValido() {
        var c = new ClienteModel();
        c.setRazaoSocial("Santa Rita Agropecuária Ltda");
        c.setCpfCnpj("");
        c.setTelefone("");
        c.setCep("");
        return c;
    }

    @Test
    void deveLancarExcecaoQuandoRazaoSocialVazia() {
        var c = clienteValido();
        c.setRazaoSocial("");
        assertThrows(IllegalArgumentException.class, () -> clienteService.salvar(c));
    }

    @Test
    void deveLancarExcecaoQuandoTelefoneInvalido() {
        var c = clienteValido();
        c.setTelefone("123");
        assertThrows(IllegalArgumentException.class, () -> clienteService.salvar(c));
    }

    @Test
    void deveLancarExcecaoQuandoCepInvalido() {
        var c = clienteValido();
        c.setCep("abc");
        assertThrows(IllegalArgumentException.class, () -> clienteService.salvar(c));
    }

    @Test
    void deveSalvarCliente() throws Exception {
        var salvo = clienteService.salvar(clienteValido());
        assertNotNull(salvo.getId());
        assertNotNull(salvo.getDataCriacao());
        assertEquals(true, salvo.getAtivo());
    }

    @Test
    void deveAceitarTelefoneValido() throws Exception {
        var c = clienteValido();
        c.setTelefone("11999999999");
        var salvo = clienteService.salvar(c);
        assertEquals("11999999999", salvo.getTelefone());
    }

    @Test
    void devePermitirTelefoneVazio() throws Exception {
        var c = clienteValido();
        c.setTelefone("");
        assertDoesNotThrow(() -> clienteService.salvar(c));
    }

    @Test
    void deveAtualizarCliente() throws Exception {
        var c = clienteService.salvar(clienteValido());
        c.setRazaoSocial("Fazenda Atualizada");
        clienteService.atualizar(c);
        var buscado = clienteService.buscarById(c.getId());
        assertEquals("Fazenda Atualizada", buscado.getRazaoSocial());
    }

    @Test
    void deveLancarExcecaoQuandoCpfCnpjDuplicado() throws Exception {
        var c1 = clienteValido();
        c1.setRazaoSocial("Cliente 1");
        c1.setCpfCnpj("529.982.247-25");
        clienteService.salvar(c1);

        var c2 = clienteValido();
        c2.setRazaoSocial("Cliente 2");
        c2.setCpfCnpj("529.982.247-25");
        assertThrows(IllegalArgumentException.class, () -> clienteService.salvar(c2));
    }

    @Test
    void devePermitirSalvarClienteComCpfCnpjUnico() throws Exception {
        var c1 = clienteValido();
        c1.setRazaoSocial("Cliente 1");
        c1.setCpfCnpj("75789180000131");
        clienteService.salvar(c1);

        var c2 = clienteValido();
        c2.setRazaoSocial("Cliente 2");
        c2.setCpfCnpj("55938445000109");
        assertDoesNotThrow(() -> clienteService.salvar(c2));
    }

    @Test
    void devePermitirAtualizarClienteMantendoCpfCnpj() throws Exception {
        var c = clienteValido();
        c.setCpfCnpj("58.469.407/0001-42");
        var salvo = clienteService.salvar(c);

        salvo.setRazaoSocial("Razão Social Editada");
        assertDoesNotThrow(() -> clienteService.atualizar(salvo));
    }

    @Test
    void operadorPodeCadastrarEEditarMasNaoExcluirCliente() throws Exception {
        var operador = new UsuarioModel();
        operador.setAdmin(false);
        SessaoUsuario.login(operador);

        var salvo = clienteService.salvar(clienteValido());
        salvo.setRazaoSocial("Razão editada pelo operador");
        assertDoesNotThrow(() -> clienteService.atualizar(salvo));
        assertThrows(IllegalArgumentException.class, () -> clienteService.excluirById(salvo.getId()));
    }
}
