package pl.aetas.gtweb.domain;


import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AbstractEntityTest {

    // SUT
    private AbstractEntity entity;

    @SuppressWarnings("serial")
    @Before
    public void setup() {
        entity = new AbstractEntity() {
            // abstract class for tests
        };
    }

    @Test
    public void equals_shouldReturnTrueWhenTasksHaveTheSameId() throws Exception {
        final AbstractEntity entity2 = mock(AbstractEntity.class);
        when(entity2.getId()).thenReturn(55L);
        entity.setId(55L);

        assertThat(entity.equals(entity2)).isTrue();
    }

    @Test
    public void equals_shouldReturnFalseWhenTasksHaveTheDifferentIds() throws Exception {
        final AbstractEntity entity2 = mock(AbstractEntity.class);
        when(entity2.getId()).thenReturn(100L);
        entity.setId(200L);

        assertThat(entity.equals(entity2)).isFalse();
    }

    @Test
    public void equals_shouldReturnFalseWhenSecondTaskIdIsNull() throws Exception {
        final AbstractEntity entity2 = mock(AbstractEntity.class);
        when(entity2.getId()).thenReturn(null);
        entity.setId(200L);

        assertThat(entity.equals(entity2)).isFalse();
    }

    @Test
    public void equals_shouldReturnFalseWhenFirstTaskIdIsNull() throws Exception {
        final AbstractEntity entity2 = mock(AbstractEntity.class);
        when(entity2.getId()).thenReturn(100L);
        entity.setId(null);

        assertThat(entity.equals(entity2)).isFalse();
    }

    @Test
    public void equals_shouldReturnFalseWhenBothTasksIdsAreNullButTasksAreDifferentObjects() throws Exception {
        final AbstractEntity entity2 = mock(AbstractEntity.class);
        when(entity2.getId()).thenReturn(null);
        entity.setId(null);

        assertThat(entity.equals(entity2)).isFalse();
    }

}
