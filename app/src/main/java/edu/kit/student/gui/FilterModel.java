package edu.kit.student.gui;

import edu.kit.student.plugin.EdgeFilter;
import edu.kit.student.plugin.VertexFilter;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * A {@link FilterModel} holds the state of the currently active filter and additional options
 * around filter for a single graph.
 */
public class FilterModel {


    /**
     * Hold the filters, which have been active the last time {@link FilterModel#backup()} has been called.
     */
    private final Set<VertexFilter> lastBackupVFilter = new HashSet<>();
    private final Set<EdgeFilter> lastBackupEFilter = new HashSet<>();

    /**
     * Hold the currently active filter.
     */
    private final ObservableList<VertexFilter> vFilter = FXCollections.observableArrayList();

    private final ObservableList<EdgeFilter> eFilter = FXCollections.observableArrayList();

    public FilterModel() {
        this(new LinkedList<>(), new LinkedList<>());
    }

    /**
     * Constructs a new {@link FilterModel} with the specified vertex and edge filters as
     * the currently active filters. Also initializes a first backup with the same filters.
     * @param vertexFilters the active vertex filters
     * @param edgeFilters the active edge filters
     */
    public FilterModel(List<VertexFilter> vertexFilters, List<EdgeFilter> edgeFilters) {
        vFilter.addAll(vertexFilters);
        eFilter.addAll(edgeFilters);
    }

    public final BooleanProperty canOptimize = new SimpleBooleanProperty(true);
    public final BooleanProperty needLayout = new SimpleBooleanProperty(true);

    /**
     * Indicates if the filters should be applied without relayout, the edges should be optimized.
     */
    private boolean optimize;

    /**
     * Indicates if the filters should be applied without relayout, the edges should be optimized.
     * @return true if the edges should be optimized, false otherwise
     */
    public boolean optimize() {
        return optimize;
    }

    /**
     * Sets whether the edges should be optimized when applying the filters without relayout
     * @param optimize the indicator if edges should be optimized
     */
    public void setOptimize(boolean optimize) {
        this.optimize = optimize;
    }

    public ObservableList<VertexFilter> observableVertexFilters() {
        return vFilter;
    }

    public ObservableList<EdgeFilter> observableEdgeFilters() {
        return eFilter;
    }

    public List<VertexFilter> getVertexFilters() {
        return vFilter;
    }

    public List<EdgeFilter> getEdgeFilters() {
        return eFilter;
    }

    public void backup() {
        this.lastBackupVFilter.clear();
        this.lastBackupVFilter.addAll(getVertexFilters());
        this.lastBackupEFilter.clear();
        this.lastBackupEFilter.addAll(getEdgeFilters());
    }

    public boolean changedSinceBackup() {
        HashSet<VertexFilter> vertexFilterHashSet = new HashSet<>(getVertexFilters());
        HashSet<EdgeFilter> edgeFilterHashSet = new HashSet<>(getEdgeFilters());
        return !(vertexFilterHashSet.equals(getLastBackupVFilter()) && edgeFilterHashSet.equals(getLastBackupEFilter()));
    }

    public Set<VertexFilter> getLastBackupVFilter() {
        return lastBackupVFilter;
    }

    public Set<EdgeFilter> getLastBackupEFilter() {
        return lastBackupEFilter;
    }
}
