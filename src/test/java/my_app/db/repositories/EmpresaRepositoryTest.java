package my_app.db.repositories;

import my_app.db.models.EmpresaModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EmpresaRepositoryTest extends BaseRepositoryTest {

    private static final Logger log =
            LoggerFactory.getLogger(EmpresaRepositoryTest.class);

    EmpresaRepository repository;

    @Override
    protected void initRepository() {
        repository = new EmpresaRepository(session);
    }

    @BeforeEach
    void cleanEmpresas() throws Exception {
        try (var conn = DriverManager.getConnection(testUrl());
             var stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM empresas");
        }
    }

    private EmpresaModel novaEmpresa(String nome) {
        var model = new EmpresaModel();
        model.setNome(nome);
        model.setCpfCnpj("11.222.333/0001-44");
        model.setTelefone("(61) 3612-3876");
        model.setEmail("contato@balancasgobitech.com.br");
        model.setCep("73805-100");
        model.setCidade("Formosa");
        model.setEstado("GO");
        model.setBairro("Centro");
        model.setRua("R. Dez");
        model.setNumero("36");
        model.setLogomarca("/logo.png");
        model.setDataCriacao(LocalDateTime.now());
        return model;
    }

    @Test
    void salvar() throws SQLException {
        EmpresaModel salvo = repository.salvar(novaEmpresa("Balanças Gobitech"));

        log.info("Empresa salva com id={}", salvo.getId());

        assertNotNull(salvo);
        assertNotNull(salvo.getId());
        assertEquals("Balanças Gobitech", salvo.getNome());
        assertEquals("11.222.333/0001-44", salvo.getCpfCnpj());
    }

    @Test
    void buscarUnico() throws SQLException {
        EmpresaModel salvo = repository.salvar(novaEmpresa("Empresa Única"));

        EmpresaModel encontrado = repository.buscarUnico();

        assertNotNull(encontrado);
        assertEquals(salvo.getId(), encontrado.getId());
        assertEquals("Empresa Única", encontrado.getNome());
    }

    @Test
    void atualizar() throws SQLException {
        EmpresaModel salvo = repository.salvar(novaEmpresa("Original"));

        salvo.setNome("Atualizada");
        salvo.setTelefone("(61) 98888-0000");

        repository.atualizar(salvo);

        EmpresaModel atualizado = repository.buscarById(salvo.getId());

        assertNotNull(atualizado);
        assertEquals("Atualizada", atualizado.getNome());
        assertEquals("(61) 98888-0000", atualizado.getTelefone());
    }

    @Test
    void excluirById() throws SQLException {
        EmpresaModel salvo = repository.salvar(novaEmpresa("Excluir"));

        repository.excluirById(salvo.getId());

        assertNull(repository.buscarById(salvo.getId()));
    }

    @Test
    void buscarById() throws SQLException {
        EmpresaModel salvo = repository.salvar(novaEmpresa("Busca"));

        EmpresaModel encontrado = repository.buscarById(salvo.getId());

        assertNotNull(encontrado);
        assertEquals(salvo.getId(), encontrado.getId());
        assertEquals("Busca", encontrado.getNome());
    }
}
