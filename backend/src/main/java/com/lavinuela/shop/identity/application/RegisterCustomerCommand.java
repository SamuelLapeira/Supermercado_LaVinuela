package com.lavinuela.shop.identity.application;

/** Datos del formulario de registro. */
public record RegisterCustomerCommand(String username, String email, String password, boolean termsAccepted) {

    /** La contraseña nunca se muestra en los registros. */
    @Override
    public String toString() {
        return "RegisterCustomerCommand[username=" + username + ", email=***, password=***]";
    }
}
