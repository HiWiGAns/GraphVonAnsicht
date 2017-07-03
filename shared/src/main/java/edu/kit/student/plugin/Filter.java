package edu.kit.student.plugin;

import edu.kit.student.util.LanguageManager;

public class Filter {

    protected String name;

    public Filter(String name) {
        this.name = name;
    }

    /**
     * Returns the name of the filter.
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the filter.
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the name of the group this filter belongs to.
     * Groups of filters are visually represented together
     * and can be activated or deactivated as a group.
     * @return the group name
     */
    public String getGroup() {
        return LanguageManager.getInstance().get("filter_default_group");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Filter)) return false;

        Filter filter = (Filter) o;

        return getName() != null ? getName().equals(filter.getName()) : filter.getName() == null;
    }

    @Override
    public int hashCode() {
        return getName() != null ? getName().hashCode() : 0;
    }
}
