package my_app.db.repositories;

import net.sf.persism.Session;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;

abstract class BaseRepositoryTest {

    private static final Logger log =
            LoggerFactory.getLogger(BaseRepositoryTest.class);

    protected Session session;

    protected abstract void initRepository();

    // Banco em memória por classe de teste — cada classe com nome próprio, sem
    // compartilhar o mesmo "testdb" (evita vazamento de dados/estado entre classes).
    protected String testUrl() {
        return "jdbc:sqlite:file:testdb-" + getClass().getSimpleName() + "?mode=memory&cache=shared";
    }

    @BeforeEach
    void setUp() throws Exception {
        String testUrl = testUrl();
        Connection connection = DriverManager.getConnection(testUrl);

        Flyway.configure()
                .dataSource(testUrl, "", "")
                .locations("classpath:flyway_migrations")
                .load()
                .migrate();

        session = new Session(connection);
        initRepository();
        log.info("Banco de teste inicializado");
    }

    @AfterEach
    void tearDown() throws Exception {
        session.close();
    }
}
