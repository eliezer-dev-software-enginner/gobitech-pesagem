package my_app.utils;

import javafx.collections.ObservableList;
import javafx.scene.control.TableView;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Random;
import java.util.function.Consumer;

public class Utils {
    public static String toBRLCurrency(BigDecimal value){
        final NumberFormat BRL =
                NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        return BRL.format(value).replace('\u00a0', ' ');
    }

    public static String toBRLCurrency(String value){
        return toBRLCurrency(new BigDecimal(value));
    }


    /**
     * Esse método é usado para transformar os centavos visuais para valor em Real que será persistido no banco de dados.
     * 1000 centavos equivalem a 10 reais.
     * A conversão entre centavos e reais é baseada na relação de que 1 real = 100 centavos.  Para converter centavos em reais, basta dividir o número de centavos por 100:
     *
     * 1000 centavos ÷ 100 = 10 reais
     * @param centavos
     * @return
     */
public static BigDecimal deCentavosParaReal(String centavos){
        if (centavos == null || centavos.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(centavos).movePointLeft(2);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    // Validação simples de E-mail
    public static boolean isNotValidEmail(String email) {
        return !email.matches("^[\\w!#$%&'*+/=?`{|}~^-]+(?:\\.[\\w!#$%&'*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}$");
    }

    public static boolean isValidCep(String cep) {
        String clean = cep == null ? "" : cep.replaceAll("[^0-9]", "");
        return clean.length() == 8;
    }

    public static boolean isValidCnpj(String cnpj) {
        String clean = cnpj == null ? "" : cnpj.toUpperCase().replaceAll("[^0-9A-Z]", "");
        if (clean.length() != 14) return false;
        return clean.substring(12).matches("\\d{2}");
    }

    public static boolean isValidCpfOrCnpj(String value) {
        String clean = value == null ? "" : value.toUpperCase().replaceAll("[^0-9A-Z]", "");
        if (clean.length() == 11) return isValidCpf(clean);
        if (clean.length() == 14) return isValidCnpj(clean);
        return false;
    }

    public static boolean isValidCpf(String cpf) {
        String cleanCpf = cpf.replaceAll("[^0-9]", "");
        return cleanCpf.length() == 11;
    }

    /**
     * Valida um documento de motorista que aceita RG (8-9 dígitos) ou CPF (11 dígitos), no
     * mesmo espírito do campo combinado {@code InputRgCpf}. {@code true} para vazio/nulo
     * (o documento é opcional — a validação só se aplica a um valor informado).
     */
    public static boolean isValidDocumento(String documento) {
        String clean = documento == null ? "" : documento.replaceAll("[^0-9]", "");
        if (clean.isEmpty()) return true;
        if (clean.length() <= 9) return clean.length() >= 8;
        return clean.length() == 11;
    }

    // Valida se o telefone tem 10 ou 11 dígitos numéricos
    public static boolean isValidPhone(String phone) {
        String cleanPhone = phone.replaceAll("[^0-9]", "");
        return cleanPhone.length() >= 10 && cleanPhone.length() <= 11;
    }
    /**
     * Aplica a máscara (XX) XXXXX-XXXX ou (XX) XXXX-XXXX dinamicamente
     */
    public static String formatPhone(String numeric) {
        if (numeric == null || numeric.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        int len = numeric.length();

        sb.append("(");
        if (len <= 2) {
            sb.append(numeric);
        } else {
            sb.append(numeric, 0, 2).append(") ");
            String rest = numeric.substring(2);

            if (rest.length() <= 4) {
                sb.append(rest);
            } else if (rest.length() == 5) {
                // Formato celular (5 dígitos no primeiro bloco)
                sb.append(rest);
            } else if (rest.length() <= 8) {
                // Formato Fixo: (XX) XXXX-XXXX
                sb.append(rest, 0, 4).append("-").append(rest.substring(4));
            } else {
                // Formato Celular: (XX) XXXXX-XXXX
                sb.append(rest, 0, 5).append("-").append(rest.substring(5));
            }
        }
        return sb.toString();
    }

    public static String formatCep(String numeric) {
        if (numeric == null || numeric.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        int len = numeric.length();

        if (len <= 5) {
            sb.append(numeric);
        } else {
            sb.append(numeric, 0, 5).append("-")
                    .append(numeric.substring(5));
        }

        return sb.toString();
    }

    public static String formatCpf(String numeric) {
        if (numeric == null || numeric.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        int len = numeric.length();

        if (len <= 3) {
            sb.append(numeric);
        } else if (len <= 6) {
            sb.append(numeric, 0, 3).append(".").append(numeric.substring(3));
        } else if (len <= 9) {
            sb.append(numeric, 0, 3).append(".").append(numeric, 3, 6).append(".").append(numeric.substring(6));
        } else {
            sb.append(numeric, 0, 3).append(".")
                    .append(numeric, 3, 6).append(".")
                    .append(numeric, 6, 9).append("-")
                    .append(numeric.substring(9));
        }

        return sb.toString();
    }

    public static String formatCnpj(String numeric) {
        if (numeric == null || numeric.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        int len = numeric.length();

        if (len <= 2) {
            sb.append(numeric);
        } else if (len <= 5) {
            sb.append(numeric, 0, 2).append(".").append(numeric.substring(2));
        } else if (len <= 8) {
            sb.append(numeric, 0, 2).append(".").append(numeric, 2, 5).append(".").append(numeric.substring(5));
        } else if (len <= 12) {
            sb.append(numeric, 0, 2).append(".").append(numeric, 2, 5).append(".")
                    .append(numeric, 5, 8).append("/").append(numeric.substring(8));
        } else {
            sb.append(numeric, 0, 2).append(".").append(numeric, 2, 5).append(".")
                    .append(numeric, 5, 8).append("/").append(numeric, 8, 12).append("-")
                    .append(numeric.substring(12));
        }

        return sb.toString();
    }

    /**
     * Formata um campo combinado CPF-ou-CNPJ: enquanto o usuário digita 11 caracteres ou menos,
     * assume CPF (puramente numérico); a partir do 12º caractere, assume CNPJ (aceita letras,
     * formato alfanumérico mais recente). O reagrupamento dos separadores ao cruzar esse limiar
     * é esperado — sem perguntar de antemão qual documento é, não tem como saber os grupos certos
     * antes de ver o tamanho final.
     */
    public static String formatCpfCnpj(String cleaned) {
        if (cleaned == null || cleaned.isEmpty()) return "";
        if (cleaned.length() <= 11) {
            return formatCpf(cleaned.replaceAll("[^0-9]", ""));
        }
        return formatCnpj(cleaned);
    }

    /**
     * Formata um campo combinado RG-ou-CPF: até 9 caracteres assume RG (máscara
     * {@code ##.###.###-#}), a partir do 10º assume CPF ({@code ###.###.###-##}). O
     * reagrupamento dos separadores ao cruzar esse limiar é esperado — sem perguntar de antemão
     * qual documento é, não tem como saber os grupos certos antes de ver o tamanho final.
     */
    public static String formatRgCpf(String cleaned) {
        if (cleaned == null || cleaned.isEmpty()) return "";
        String numeric = cleaned.replaceAll("[^0-9]", "");
        if (numeric.length() <= 9) {
            return formatRg(numeric);
        }
        return formatCpf(numeric);
    }

    private static String formatRg(String numeric) {
        if (numeric == null || numeric.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        int len = numeric.length();

        if (len <= 2) {
            sb.append(numeric);
        } else if (len <= 5) {
            sb.append(numeric, 0, 2).append(".").append(numeric.substring(2));
        } else if (len <= 8) {
            sb.append(numeric, 0, 2).append(".").append(numeric, 2, 5).append(".").append(numeric.substring(5));
        } else {
            sb.append(numeric, 0, 2).append(".")
                    .append(numeric, 2, 5).append(".")
                    .append(numeric, 5, 8).append("-")
                    .append(numeric.substring(8));
        }

        return sb.toString();
    }

    @Deprecated(forRemoval = true)
    public static <T> void updateItemOnObservableList(
            ObservableList<T> observableList, T modelSelected, T modelAtualizada
    ){
        int index = observableList.indexOf(modelSelected);
        if (index != -1) {
            observableList.set(index, modelAtualizada);
        }
    }
}
