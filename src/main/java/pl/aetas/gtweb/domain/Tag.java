package pl.aetas.gtweb.domain;

public class Tag {

    private final String ownerId;
    private final String name;
    private final String color;
    private final boolean visibleInWorkView;

    public Tag(String ownerId, final String name, final String color, final boolean visibleInWorkView) {
        this.ownerId = ownerId;
        this.name = name;
        this.color = color;
        this.visibleInWorkView = visibleInWorkView;
    }

    public String getOwnerId() {
        return ownerId;
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

        private String ownerId;
        private String name;
        private String color;
        private Boolean visibleInWorkView;

        public TagBuilder ownerId(String ownerId) {
            this.ownerId = ownerId;
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
            return new Tag(ownerId, name, color, visibleInWorkView);
        }

    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (name == null ? 0 : name.hashCode());
        result = prime * result + (ownerId == null ? 0 : ownerId.hashCode());
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
        if (ownerId == null) {
            if (other.ownerId != null) {
                return false;
            }
        } else if (!ownerId.equals(other.ownerId)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return getName();
    }

}
