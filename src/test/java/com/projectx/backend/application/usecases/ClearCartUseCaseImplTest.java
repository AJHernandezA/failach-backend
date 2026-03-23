package com.projectx.backend.application.usecases;

import com.projectx.backend.domain.ports.out.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ClearCartUseCaseImplTest {

    @Mock private CartRepository cartRepository;
    private ClearCartUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new ClearCartUseCaseImpl(cartRepository);
    }

    @Test
    void debeEliminarCarritoDelRepositorio() {
        useCase.execute("idoneo", "session-1");
        verify(cartRepository).delete("idoneo", "session-1");
    }
}
