package com.logicalastrology.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ThematicPredictionRequest {

    @NotBlank
    private String tema;

    @NotBlank
    @Size(max = 120)
    private String nome;

    @Size(max = 120)
    private String nomeAmor;

    @NotBlank
    private String sentimento;

    /**
     * Token de pagamento ativo informado pelo cliente para aplicar desconto, quando válido.
     */
    private String activePaymentToken;
}
