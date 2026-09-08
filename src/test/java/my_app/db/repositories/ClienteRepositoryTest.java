package my_app.db.repositories;

import my_app.db.models.ClienteModel;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClienteRepositoryTest extends BaseRepositoryTest {

    private static final Logger log =
            LoggerFactory.getLogger(ClienteRepositoryTest.class);

    ClienteRepository repository;

    @Override
    protected void initRepository() {
        repository = new ClienteRepository(session);
    }

    private ClienteModel novoCliente(String loja) {
        var cliente = new ClienteModel();
        cliente.setLoja(loja);
        cliente.setRazaoSocial("Razão Social " + loja);
        cliente.setCpfCnpj("123.456.789-00");
        cliente.setTelefone("(31) 99999-0000");
        cliente.setCep("30140-071");
        cliente.setUf("MG");
        cliente.setCidade("Belo Horizonte");
        cliente.setBairro("Centro");
        cliente.setRua("Rua Principal");
        cliente.setNumero("100");
        cliente.setAtivo(true);
        cliente.setDataCriacao(LocalDateTime.now());
        return cliente;
    }

    @Test
    void salvar() throws SQLException {
        ClienteModel salvo = repository.salvar(novoCliente("Fazenda Santa Rita"));

        log.info("Cliente salvo com id={}", salvo.getId());

        assertNotNull(salvo);
        assertNotNull(salvo.getId());
        assertEquals("Fazenda Santa Rita", salvo.getLoja());
        assertEquals("123.456.789-00", salvo.getCpfCnpj());
    }

    @Test
    void listar() throws SQLException {
        repository.salvar(novoCliente("Sítio Boa Vista"));

        List<ClienteModel> lista = repository.listar();

        assertNotNull(lista);
        assertFalse(lista.isEmpty());

        boolean encontrou = lista.stream()
                .anyMatch(it -> it.getLoja().equals("Sítio Boa Vista"));

        assertTrue(encontrou);
    }

    @Test
    void atualizar() throws SQLException {
        ClienteModel salvo = repository.salvar(novoCliente("Original"));

        salvo.setLoja("Atualizado");
        salvo.setTelefone("(31) 98888-0000");

        repository.atualizar(salvo);

        ClienteModel atualizado = repository.buscarById(salvo.getId());

        assertNotNull(atualizado);
        assertEquals("Atualizado", atualizado.getLoja());
        assertEquals("(31) 98888-0000", atualizado.getTelefone());
    }

    @Test
    void excluirById() throws SQLException {
        ClienteModel salvo = repository.salvar(novoCliente("Excluir"));

        repository.excluirById(salvo.getId());

        assertNull(repository.buscarById(salvo.getId()));
    }

    @Test
    void buscarById() throws SQLException {
        ClienteModel salvo = repository.salvar(novoCliente("Busca"));

        ClienteModel encontrado = repository.buscarById(salvo.getId());

        assertNotNull(encontrado);
        assertEquals(salvo.getId(), encontrado.getId());
        assertEquals("Busca", encontrado.getLoja());
    }

    @Test
    void buscarPorLoja() throws SQLException {
        repository.salvar(novoCliente("Loja Única"));

        ClienteModel encontrado = repository.buscarPorLoja("Loja Única");

        assertNotNull(encontrado);
        assertEquals("Loja Única", encontrado.getLoja());
    }

    @Test
    void buscarPorCpfCnpj() throws SQLException {
        repository.salvar(novoCliente("Cliente CPF"));

        ClienteModel encontrado = repository.buscarPorCpfCnpj("123.456.789-00");

        assertNotNull(encontrado);
        assertEquals("Cliente CPF", encontrado.getLoja());
    }

    @Test
    void count() throws SQLException {
        repository.salvar(novoCliente("Um"));
        repository.salvar(novoCliente("Dois"));

        assertEquals(2, repository.count());
    }

    @Test
    void buscarPorIds() throws SQLException {
        var a = repository.salvar(novoCliente("A"));
        var b = repository.salvar(novoCliente("B"));
        repository.salvar(novoCliente("C"));

        var encontrados = repository.buscarPorIds(List.of(a.getId(), b.getId()));

        assertEquals(2, encontrados.size());
        var lojas = encontrados.stream().map(ClienteModel::getLoja).toList();
        assertTrue(lojas.contains("A"));
        assertTrue(lojas.contains("B"));
        assertFalse(lojas.contains("C"));
    }

    @Test
    void buscarPorIdsVazioRetornaListaVazia() throws SQLException {
        assertTrue(repository.buscarPorIds(List.of()).isEmpty());
        assertTrue(repository.buscarPorIds(null).isEmpty());
    }
}
