package com.lavinuela.shop.identity.application;

import java.util.List;

/** Los datos de la persona que se muestran en la tienda: quién es y qué puede hacer. */
public record UserSummary(String id, String username, String email, List<String> roles) {
}
