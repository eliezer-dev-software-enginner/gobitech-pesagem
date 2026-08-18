package my_app.db.repositories;

import my_app.db.models.DescontoModel;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class DescontoRepositoryTest extends BaseRepositoryTest {

    private static final Logger log =
            LoggerFactory.getLogger(DescontoRepositoryTest.class);

    DescontoRepository repository;

    @Override
    protected void initRepository() {
        repository = new DescontoRepository(session);
    }

    private DescontoModel novoDesconto() {
        var model = new DescontoModel();
        model.setAvariados(BigDecimal.valueOf(2));
        model.setArdidos(BigDecimal.valueOf(1));
        model.setQuebraArdidos(BigDecimal.ZERO);
        model.setImpurezas(BigDecimal.valueOf(3));
        model.setQuebraImpurezas(BigDecimal.ZERO);
        model.setUmidade(BigDecimal.valueOf(14));
        model.setQuebraUmidade(BigDecimal.ZERO);
        model.setOutros(BigDecimal.ZERO);
        model.setDataCriacao(LocalDateTime.now());
        return model;
    }

    @Test
    void salvar() throws SQLException {
        DescontoModel salvo = repository.salvar(novoDesconto());

        log.info("Desconto salvo com id={}", salvo.getId());

        assertNotNull(salvo);
        assertNotNull(salvo.getId());
        assertEquals(0, BigDecimal.valueOf(2).compareTo(salvo.getAvariados()));
    }

    @Test
    void atualizar() throws SQLException {
        DescontoModel salvo = repository.salvar(novoDesconto());

        salvo.setUmidade(BigDecimal.valueOf(20));
        repository.atualizar(salvo);

        DescontoModel atualizado = repository.buscarById(salvo.getId());

        assertEquals(0, BigDecimal.valueOf(20).compareTo(atualizado.getUmidade()));
    }

    @Test
    void buscarById() throws SQLException {
        DescontoModel salvo = repository.salvar(novoDesconto());

        DescontoModel encontrado = repository.buscarById(salvo.getId());

        assertNotNull(encontrado);
        assertEquals(salvo.getId(), encontrado.getId());
    }

    @Test
    void excluirById() throws SQLException {
        DescontoModel salvo = repository.salvar(novoDesconto());

        repository.excluirById(salvo.getId());

        assertNull(repository.buscarById(salvo.getId()));
    }
}
