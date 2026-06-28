package es.upm.api.resources.dtos.validations;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PositiveBigDecimalValidatorTest {

    private final PositiveBigDecimalValidator validator = new PositiveBigDecimalValidator();

    @Mock
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        validator.initialize(null);
    }

    @Test
    void acceptsPositiveValues() {
        assertThat(validator.isValid(BigDecimal.TEN, context)).isTrue();
        assertThat(validator.isValid(BigDecimal.ZERO, context)).isTrue();
    }

    @Test
    void rejectsNullAndNegativeValues() {
        assertThat(validator.isValid(null, context)).isFalse();
        assertThat(validator.isValid(new BigDecimal("-1"), context)).isFalse();
    }
}

@ExtendWith(MockitoExtension.class)
class ListNotEmptyValidatorTest {

    private final ListNotEmptyValidator validator = new ListNotEmptyValidator();

    @Mock
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        validator.initialize(null);
    }

    @Test
    void acceptsNonEmptyList() {
        assertThat(validator.isValid(List.of("item"), context)).isTrue();
    }

    @Test
    void rejectsNullAndEmptyList() {
        assertThat(validator.isValid(null, context)).isFalse();
        assertThat(validator.isValid(Collections.emptyList(), context)).isFalse();
    }
}
