package pl.aetas.gtweb.domain;

import java.io.Serializable;

public abstract class AbstractEntity implements Serializable {

    protected Long id;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (id == null ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        final AbstractEntity other = (AbstractEntity) obj;
        if (getId() == null) {
            return other.getId() == null && this == other;
        } else if (!getId().equals(other.getId())) {
            return false;
        }
        return true;
    }

}