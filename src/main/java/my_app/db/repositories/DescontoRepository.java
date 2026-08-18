package my_app.db.repositories;

import my_app.db.models.DescontoModel;
import net.sf.persism.Session;

public class DescontoRepository extends BaseRepository<DescontoModel> {

    public DescontoRepository(Session session) {
        super(session);
    }

    @Override
    protected Class<DescontoModel> modelClass() {
        return DescontoModel.class;
    }
}
