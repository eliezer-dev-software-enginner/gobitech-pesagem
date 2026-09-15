package my_app.domain.pesagem;

public enum TipoImpressao {
    TERMICA("termica", "Impressora térmica (80 mm)"),
    LASER("laser", "Impressora a laser");

    private final String valor;
    private final String descricao;

    TipoImpressao(String valor, String descricao) {
        this.valor = valor;
        this.descricao = descricao;
    }

    public String valor() {
        return valor;
    }

    public String descricao() {
        return descricao;
    }

    public static TipoImpressao doValor(String valor) {
        for (var tipo : values()) {
            if (tipo.valor.equals(valor)) return tipo;
        }
        throw new IllegalArgumentException("Tipo de impressão inválido. Salve novamente em Configurações.");
    }
}
