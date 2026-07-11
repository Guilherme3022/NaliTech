package com.nalitech.modules.user.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    void usuarioNovoEhAtivoPorPadrao() {
        assertThat(new User().isActive()).isTrue();
    }

    @Test
    void usuarioInativoNaoEstaAtivo() {
        User user = new User();
        user.setStatus(UserStatus.INATIVO);

        assertThat(user.isActive()).isFalse();
    }

    @Test
    void doisFatoresDesligadoPorPadrao() {
        assertThat(new User().isTwoFactorEnabled()).isFalse();
    }
}
