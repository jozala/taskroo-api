package pl.aetas.gtweb.domain;

public class Tag {

    private static final long serialVersionUID = 1539838410091532347L;

    private final String id;
    private final User owner;
    private final String name;
    private final String color;
    private final boolean visibleInWorkView;

    public Tag(String id, final User owner, final String name, final String color, final boolean visibleInWorkView) {
        this.id = id;
        this.owner = owner;
        this.name = name;
        this.color = color;
        this.visibleInWorkView = visibleInWorkView;
    }

    public String getName() {
        return name;
    }

    public String getColor() {
        return color;
    }

    public Boolean isVisibleInWorkView() {
        return visibleInWorkView;
    }

    public static class TagBuilder {

        private User owner;
        private String name;
        private String color;
        private Boolean visibleInWorkView;

        public TagBuilder owner(final User owner) {
            this.owner = owner;
            return this;
        }

        public TagBuilder name(final String name) {
            this.name = name;
            return this;
        }

        public TagBuilder color(final String color) {
            this.color = color;
            return this;
        }

        public TagBuilder visibleInWorkView(final Boolean visibleInWorkView) {
            this.visibleInWorkView = visibleInWorkView;
            return this;
        }

        public Tag build() {
            return new Tag(null, owner, name, color, visibleInWorkView);
        }

    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (name == null ? 0 : name.hashCode());
        result = prime * result + (owner == null ? 0 : owner.hashCode());
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
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Tag other = (Tag) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (owner == null) {
            if (other.owner != null) {
                return false;
            }
        } else if (!owner.equals(other.owner)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return getName();
    }

}
