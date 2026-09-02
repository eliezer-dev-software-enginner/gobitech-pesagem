package my_app.domain;

import pack.utilities.FormatterPack;

import java.util.List;

public class Data {
    public static final List<String> simNaoList = List.of("Sim", "Não");
    public static final List<String> ufList = List.of(
            "AC-Acre", "AL-Alagoas", "AP-Amapá", "AM-Amazonas", "BA-Bahia", "CE-Ceará", "DF-Distrito Federal", "ES-Espírito Santo",
            "GO-Goiás", "MA-Maranhão", "MT-Mato Grosso", "MS-Mato Grosso do Sul", "MG-Minas Gerais", "PA-Pará", "PB-Paraíba", "PR-Paraná",
            "PE-Pernambuco", "PI-Piauí", "RJ-Rio de Janeiro", "RN-Rio Grande do Norte", "RS-Rio Grande do Sul", "RO-Rondônia", "RR-Roraima",
            "SC-Santa Catarina", "SP-São Paulo", "SE-Sergipe", "TO-Tocantins"
    );

    public static final List<String> tiposPagamentoList = List.of("A VISTA", "CRÉDITO", "DÉBITO", "PIX", "A PRAZO");

    // Mesmas 3 opções do combobox "Unidade*" do app original (UI.Product.ProductForm) —
    // faz sentido pro domínio de pesagem de caminhão (produtos a granel), diferente da
    // lista genérica de varejo (UN/CX/PCT/DZ/...) que veio do plics-sw.
    public static final List<String> unidadesDeMedidaList = List.of(
            "Quilos", "Toneladas", "Gramas"
    );
    public static final List<String> tiposPessoaList = List.of("Física", "Jurídica");

    private static final String numberWhatsappSupport = "5532985066537";
    public static final String linkWhatsappSupport = "https://wa.me/"+numberWhatsappSupport;

    public static final String linkWebsiteOfficial = "https://gobitech-webpage.vercel.app/";

    public static String getNumberWhatsappSupportFormatted() {
        return FormatterPack.formatPhone(numberWhatsappSupport.replace("55",""));
    }

    static void main() {
        IO.println(getNumberWhatsappSupportFormatted());
    }
}
