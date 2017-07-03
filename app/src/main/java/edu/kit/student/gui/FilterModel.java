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

    FilterModel() {
        this(new LinkedList<>(), new LinkedList<>());
    }

    /**
     * Constructs a new {@link FilterModel} with the specified vertex and edge filters as
     * the currently active filters. Also initializes a first backup with the same filters.
     * @param vertexFilters the active vertex filters
     * @param edgeFilters the active edge filters
     */
    FilterModel(List<VertexFilter> vertexFilters, List<EdgeFilter> edgeFilters) {
        vFilter.addAll(vertexFilters);
        eFilter.addAll(edgeFilters);
    }

    final BooleanProperty layoutCanOptimizeProperty = new SimpleBooleanProperty(true);
    final BooleanProperty needLayoutProperty = new SimpleBooleanProperty(true);

    /**
     * Returns true if the at this state the filter could be applied, while fixing the vertex positions.
     * I.e. the layout is able to handle a graph with fixed vertices and no relayout is needed due to not layouted
     * vertices.
     * @return if the filter could be applied, while fixing the vertex positions.
     */
    boolean canOptimize() {
        return layoutCanOptimizeProperty.get() && !needLayoutProperty.get();
    }

    /**
     * Indicates if the filters should be applied without relayout, the edges should be optimized.
     */
    private boolean fixVertices;

    /**
     * Indicates if the filters should be applied without relayout, the edges should be optimized.
     * @return true if the edges should be optimized, false otherwise
     */
    boolean isFixVertices() {
        return fixVertices;
    }

    /**
     * Sets whether the edges should be optimized when applying the filters without relayout
     * @param fixVertices the indicator if edges should be optimized
     */
    void setFixVertices(boolean fixVertices) {
        this.fixVertices = fixVertices;
    }

    ObservableList<VertexFilter> observableVertexFilters() {
        return vFilter;
    }

    ObservableList<EdgeFilter> observableEdgeFilters() {
        return eFilter;
    }

    List<VertexFilter> getVertexFilters() {
        return vFilter;
    }

    List<EdgeFilter> getEdgeFilters() {
        return eFilter;
    }

    void backup() {
        this.lastBackupVFilter.clear();
        this.lastBackupVFilter.addAll(getVertexFilters());
        this.lastBackupEFilter.clear();
        this.lastBackupEFilter.addAll(getEdgeFilters());
    }

    boolean changedSinceBackup() {
        HashSet<VertexFilter> vertexFilterHashSet = new HashSet<>(getVertexFilters());
        HashSet<EdgeFilter> edgeFilterHashSet = new HashSet<>(getEdgeFilters());
        return !(vertexFilterHashSet.equals(getLastBackupVFilter()) && edgeFilterHashSet.equals(getLastBackupEFilter()));
    }

    Set<VertexFilter> getLastBackupVFilter() {
        return lastBackupVFilter;
    }

    Set<EdgeFilter> getLastBackupEFilter() {
        return lastBackupEFilter;
    }
}
