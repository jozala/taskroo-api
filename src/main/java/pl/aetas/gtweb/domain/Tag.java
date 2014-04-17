package pl.aetas.gtweb.domain;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class Tag {

    private final String id;
    private String ownerId;
    private final String name;
    private final String color;
    private final boolean visibleInWorkView;

    @JsonCreator
    public Tag(@JsonProperty("id") String id, @JsonProperty("ownerId") String ownerId, @JsonProperty("name") String name,
               @JsonProperty("color") String color, @JsonProperty("visibleInWorkView") boolean visibleInWorkView) {
        this.id = id;
        this.ownerId = ownerId;
        this.name = name;
        this.color = color;
        this.visibleInWorkView = visibleInWorkView;
    }

    public String getId() {
        return id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
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
        private String id;
        private String ownerId;
        private String name;
        private String color;
        private boolean visibleInWorkView;

        private TagBuilder() {
            // use static "start" method instead
        }

        public static TagBuilder start(String ownerId, String name) {
            TagBuilder tagBuilder = new TagBuilder();
            tagBuilder.ownerId(Objects.requireNonNull(ownerId));
            tagBuilder.name(Objects.requireNonNull(name));
            return tagBuilder;
        }

        public TagBuilder id(String id) {
            this.id = id;
            return this;
        }

        public TagBuilder ownerId(String ownerId) {
            this.ownerId = Objects.requireNonNull(ownerId);
            return this;
        }

        public TagBuilder name(final String name) {
            this.name = Objects.requireNonNull(name);
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
            return new Tag(id, ownerId, name, color, visibleInWorkView);
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
