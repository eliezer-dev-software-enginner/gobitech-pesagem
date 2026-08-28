package my_app.core.events;

/**
 * Evento de domínio emitido via {@link EventBus} quando uma entidade é cadastrada, alterada ou
 * excluída. É uma classe abstrata genérica: cada entidade tem o seu próprio evento concreto
 * (ex.: {@link ClienteEvent}, {@link ProdutoEvent}, {@link PesagemEvent}, {@link UsuarioEvent})
 * que carrega o tipo certo da entidade — assim os listeners desacoplam no tipo específico em vez
 * de fazer um pattern-match verboso sobre uma única classe genérica.
 */
public abstract class EntityEvent<T> {

    public enum EventType { CRIADO, EDITADO, EXCLUIDO }

    private final T entity;
    private final EventType type;
    private final long entityId;

    protected EntityEvent(T entity, EventType type, long entityId) {
        this.entity = entity;
        this.type = type;
        this.entityId = entityId;
    }

    /** A entidade envolvida no evento — {@code null} quando o tipo é {@link EventType#EXCLUIDO}. */
    public T entity() {
        return entity;
    }

    public EventType type() {
        return type;
    }

    /** Id da entidade — usado no {@link EventType#EXCLUIDO}, quando não há mais o objeto. */
    public long entityId() {
        return entityId;
    }

    public boolean is(EventType eventType) {
        return this.type == eventType;
    }
}
