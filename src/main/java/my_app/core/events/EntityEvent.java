package my_app.core.events;

/**
 * Evento de domínio emitido via {@link EventBus} quando uma entidade é cadastrada, alterada ou
 * excluída. É uma classe abstrata genérica usada apenas como marcador: cada entidade tem o seu
 * próprio evento concreto (ex.: {@link ClienteEvent}, {@link ProdutoEvent}, {@link PesagemEvent},
 * {@link UsuarioEvent}) que carrega o tipo certo da entidade — assim os listeners desacoplam no
 * tipo específico em vez de fazer um pattern-match verboso sobre uma única classe genérica. A
 * intenção (criado/editado/excluido) é comunicada pelo nome da fábrica estática de cada evento.
 */
public abstract class EntityEvent<T> {
}
