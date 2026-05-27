package com.r4bb1t.blockerspam.helper

import android.content.Context
import android.telephony.PhoneNumberUtils

object NumberInfoHelper {

    private val dddMap = mapOf(
        "11" to "São Paulo - SP", "12" to "São José dos Campos - SP",
        "13" to "Baixada Santista - SP", "14" to "Bauru - SP",
        "15" to "Sorocaba - SP", "16" to "Ribeirão Preto - SP",
        "17" to "São José do Rio Preto - SP", "18" to "Presidente Prudente - SP",
        "19" to "Campinas - SP", "21" to "Rio de Janeiro - RJ",
        "22" to "Campos dos Goytacazes - RJ", "24" to "Volta Redonda - RJ",
        "27" to "Vitória - ES", "28" to "Sul do Espírito Santo - ES",
        "31" to "Belo Horizonte - MG", "32" to "Juiz de Fora - MG",
        "33" to "Governador Valadares - MG", "34" to "Uberlândia - MG",
        "35" to "Poços de Caldas - MG", "37" to "Divinópolis - MG",
        "38" to "Montes Claros - MG", "41" to "Curitiba - PR",
        "42" to "Ponta Grossa - PR", "43" to "Londrina - PR",
        "44" to "Maringá - PR", "45" to "Cascavel - PR",
        "46" to "Francisco Beltrão - PR", "47" to "Joinville - SC",
        "48" to "Florianópolis - SC", "49" to "Chapecó - SC",
        "51" to "Porto Alegre - RS", "53" to "Pelotas - RS",
        "54" to "Caxias do Sul - RS", "55" to "Santa Maria - RS",
        "61" to "Brasília - DF", "62" to "Goiânia - GO",
        "63" to "Palmas - TO", "64" to "Rio Verde - GO",
        "65" to "Cuiabá - MT", "66" to "Rondonópolis - MT",
        "67" to "Campo Grande - MS", "68" to "Rio Branco - AC",
        "69" to "Porto Velho - RO", "71" to "Salvador - BA",
        "73" to "Ilhéus - BA", "74" to "Juazeiro - BA",
        "75" to "Feira de Santana - BA", "77" to "Vitória da Conquista - BA",
        "79" to "Aracaju - SE", "81" to "Recife - PE",
        "82" to "Maceió - AL", "83" to "João Pessoa - PB",
        "84" to "Natal - RN", "85" to "Fortaleza - CE",
        "86" to "Teresina - PI", "87" to "Caruaru - PE",
        "88" to "Juazeiro do Norte - CE", "89" to "Picos - PI",
        "91" to "Belém - PA", "92" to "Manaus - AM",
        "93" to "Santarém - PA", "94" to "Marabá - PA",
        "95" to "Boa Vista - RR", "96" to "Macapá - AP",
        "97" to "Coari - AM", "98" to "São Luís - MA",
        "99" to "Imperatriz - MA"
    )

    data class NumberInfo(
        val formatted: String,
        val ddd: String?,
        val region: String?,
        val lineType: String,
        val isInternational: Boolean,
        val country: String?
    )

    fun getInfo(context: Context, rawNumber: String): NumberInfo {
        val clean = rawNumber.replace(Regex("[^+\\d]"), "")

        val isIntl = clean.startsWith("+") && !clean.startsWith("+55")
        val isBrazil = clean.startsWith("+55") || (!clean.startsWith("+") && clean.length in 8..15)

        var ddd: String? = null
        var region: String? = null
        var lineType = "Desconhecido"

        if (isBrazil) {
            var local = when {
                clean.startsWith("+55") -> clean.removePrefix("+55")
                clean.startsWith("55") && clean.length > 11 -> clean.removePrefix("55")
                else -> clean
            }
            
            if (local.startsWith("0")) {
                local = local.removePrefix("0")
                if (local.length >= 12) {
                    local = local.drop(2) // Remove o código da operadora (ex: 15)
                }
            }

            if (local.length >= 2) {
                ddd = local.take(2)
                region = dddMap[ddd] ?: "Brasil"
            }
            val digits = local.drop(2)
            lineType = when {
                digits.length == 9 -> "Celular"
                digits.length == 8 -> "Fixo"
                else -> "Desconhecido"
            }
        } else if (isIntl) {
            lineType = "Internacional"
        }

        val formatted = PhoneNumberUtils.formatNumber(rawNumber, "BR") ?: rawNumber

        return NumberInfo(
            formatted = formatted,
            ddd = ddd,
            region = region,
            lineType = lineType,
            isInternational = isIntl,
            country = if (isIntl) "Internacional" else if (isBrazil) "Brasil" else null
        )
    }
}
