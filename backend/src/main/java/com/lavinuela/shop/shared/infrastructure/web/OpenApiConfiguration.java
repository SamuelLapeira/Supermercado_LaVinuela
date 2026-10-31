package com.lavinuela.shop.shared.infrastructure.web;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Datos generales del contrato de la API.
 *
 * <p>La dirección del servidor se fija en «/» (relativa) a propósito: si no, el documento llevaría la dirección y el
 * puerto con los que se generó, y cambiaría en cada ejecución de las pruebas.
 */
@Configuration(proxyBeanMethods = false)
class OpenApiConfiguration {

    @Bean
    OpenAPI lavinuelaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("API de Supermercado La Viñuela")
                        .version("0.1.0")
                        .description("Contrato de la API de la tienda. Se genera solo a partir del código del servidor: "
                                + "no se edita a mano. El cliente de Angular se genera a partir de este documento."))
                .servers(List.of(new Server().url("/")));
    }
}
