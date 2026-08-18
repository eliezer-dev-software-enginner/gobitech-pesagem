package my_app.db.services;

import my_app.db.models.ClienteModel;
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
        c.setLoja("Fazenda Santa Rita");
        c.setRazaoSocial("Santa Rita Agropecuária Ltda");
        c.setCpfCnpj("");
        c.setTelefone("");
        c.setCep("");
        return c;
    }

    @Test
    void deveLancarExcecaoQuandoLojaVazia() {
        var c = clienteValido();
        c.setLoja("");
        assertThrows(IllegalArgumentException.class, () -> clienteService.salvar(c));
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
        c.setLoja("Fazenda Atualizada");
        clienteService.atualizar(c);
        var buscado = clienteService.buscarById(c.getId());
        assertEquals("Fazenda Atualizada", buscado.getLoja());
    }

    @Test
    void deveLancarExcecaoQuandoLojaDuplicada() throws Exception {
        clienteService.salvar(clienteValido());

        var c2 = clienteValido();
        assertThrows(IllegalArgumentException.class, () -> clienteService.salvar(c2));
    }

    @Test
    void deveLancarExcecaoQuandoCpfCnpjDuplicado() throws Exception {
        var c1 = clienteValido();
        c1.setLoja("Loja 1");
        c1.setCpfCnpj("123.456.789-00");
        clienteService.salvar(c1);

        var c2 = clienteValido();
        c2.setLoja("Loja 2");
        c2.setCpfCnpj("123.456.789-00");
        assertThrows(IllegalArgumentException.class, () -> clienteService.salvar(c2));
    }

    @Test
    void devePermitirSalvarClienteComCpfCnpjUnico() throws Exception {
        var c1 = clienteValido();
        c1.setLoja("Loja 1");
        c1.setCpfCnpj("123.456.789-00");
        clienteService.salvar(c1);

        var c2 = clienteValido();
        c2.setLoja("Loja 2");
        c2.setCpfCnpj("987.654.321-00");
        assertDoesNotThrow(() -> clienteService.salvar(c2));
    }

    @Test
    void devePermitirAtualizarClienteMantendoCpfCnpjELoja() throws Exception {
        var c = clienteValido();
        c.setCpfCnpj("123.456.789-00");
        var salvo = clienteService.salvar(c);

        salvo.setRazaoSocial("Razão Social Editada");
        assertDoesNotThrow(() -> clienteService.atualizar(salvo));
    }
}
