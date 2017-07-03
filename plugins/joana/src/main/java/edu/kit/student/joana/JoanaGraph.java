package edu.kit.student.joana;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


import edu.kit.student.graphmodel.FastGraphAccessor;
import edu.kit.student.graphmodel.ViewableGraph;
import edu.kit.student.graphmodel.directed.DirectedGraph;
import edu.kit.student.joana.JoanaEdge.EdgeKind;
import edu.kit.student.objectproperty.GAnsProperty;
import edu.kit.student.plugin.EdgeFilter;
import edu.kit.student.plugin.LayoutOption;
import edu.kit.student.plugin.VertexFilter;
import edu.kit.student.util.IdGenerator;
import edu.kit.student.util.LanguageManager;

/**
 * An abstract superclass for all JOANA specific graphs.
 */
public abstract class JoanaGraph
    implements DirectedGraph, ViewableGraph {
    
    private Integer id;
    private GAnsProperty<String> name;
    
    private GAnsProperty<Integer> edgeCount;
    private GAnsProperty<Integer> vertexCount;
    private List<VertexFilter> vertexFilter;
    private List<EdgeFilter> edgeFilter;
    

    public JoanaGraph(String name, Set<JoanaVertex> vertices, Set<JoanaEdge> edges) {
        this.name = new GAnsProperty<>(LanguageManager.getInstance().get("stat_name"), name);
        this.id = IdGenerator.getInstance().createId();    
        this.edgeCount = new GAnsProperty<>(LanguageManager.getInstance().get("stat_edge"), edges.size());
        this.vertexCount = new GAnsProperty<>(LanguageManager.getInstance().get("stat_vertex"), vertices.size());
        this.vertexFilter = new LinkedList<>();
        this.edgeFilter = new LinkedList<>();
    }

    @Override
    public String getName() {
        return name.getValue();
    }

    @Override
    public Integer getID() {
        return this.id;
    }
    
    @Override
    public List<GAnsProperty<?>> getStatistics() {
    	List<GAnsProperty<?>> statistics = new LinkedList<>();
    	statistics.add(name);
    	statistics.add(this.vertexCount);
    	statistics.add(this.edgeCount);
    	return statistics;
    }

    public List<LayoutOption> getRegisteredLayouts() {
        return new LinkedList<>();
    }

    @Override
    public void addVertexFilter(VertexFilter filter) {
        this.vertexFilter.add(filter);
    }
    
    @Override
    public void setVertexFilter(List<VertexFilter> filter) {
    	this.vertexFilter = filter;
    }

    @Override
    public void addEdgeFilter(EdgeFilter filter) {
        this.edgeFilter.add(filter);
    }
    
    @Override
    public void setEdgeFilter(List<EdgeFilter> filter) {
    	this.edgeFilter = filter;
    }
    
    @Override
    public List<VertexFilter> getActiveVertexFilter() {
    	return Collections.unmodifiableList(this.vertexFilter);
    }
    
    @Override
    public List<EdgeFilter> getActiveEdgeFilter() {
    	return Collections.unmodifiableList(this.edgeFilter);
    }
    
    public Set<JoanaEdge> removeFilteredEdges(Set<JoanaEdge> edges) {
        Set<JoanaEdge> edgeFiltered = edges.stream().filter(e -> edgeFilter.stream().allMatch(f -> f.getPredicate().negate().test(e))).collect(Collectors.toSet());
        Set<JoanaEdge> vertexFiltered = new HashSet<>(edgeFiltered);
        for (JoanaEdge edge : edgeFiltered) {
            JoanaVertex source = edge.getSource();
            JoanaVertex target = edge.getTarget();
            if (vertexFilter.stream().anyMatch(f -> f.getPredicate().test(source) || f.getPredicate().test(target))) {
                vertexFiltered.remove(edge);
            }
        }
        return vertexFiltered;
    }

    /**
     * An interprocedural edge will be removed in filtering only if
     * The normal vertex or the dummy vertex have been filtered.
     * Filtering the edge itself has no effect, as the dummy is drawn nevertheless
     */
    public Set<InterproceduralEdge> removeFilteredInterproceduralEdges(Set<InterproceduralEdge> edges){
        Set<InterproceduralEdge> vertexFiltered = new HashSet<>(edges);
        for (InterproceduralEdge edge : edges) {
            JoanaVertex dummy = edge.getDummyVertex();
            JoanaVertex normal = edge.getNormalVertex();
            if (vertexFilter.stream().anyMatch(f -> f.getPredicate().test(dummy) || f.getPredicate().test(normal))) {
                vertexFiltered.remove(edge);
            }
        }
        return vertexFiltered;
    }

    public Set<JoanaVertex> removeFilteredVertices(Set<JoanaVertex> vertices) {
        return vertices.stream().filter(v -> vertexFilter.stream().allMatch(f -> f.getPredicate().negate().test(v))).collect(Collectors.toSet());
    }

    @Override
    public void removeVertexFilter(VertexFilter filter) {
        this.vertexFilter.remove(filter);
    }

    @Override
    public void removeEdgeFilter(EdgeFilter filter) {
        this.edgeFilter.remove(filter);
    }

    @Override
    public FastGraphAccessor getFastGraphAccessor() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addToFastGraphAccessor(FastGraphAccessor fga) {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }

    protected void applyDefaultFilters() {
    	//default filters for joana graphs
        JoanaEdgeFilter cfFilter = new JoanaEdgeFilter(EdgeKind.CF);
        JoanaEdgeFilter heFilter = new JoanaEdgeFilter(EdgeKind.HE);
        JoanaEdgeFilter psFilter = new JoanaEdgeFilter(EdgeKind.PS);
        JoanaEdgeFilter peFilter = new JoanaEdgeFilter(EdgeKind.PE);
        edgeFilter.add(cfFilter);
        edgeFilter.add(heFilter);
        edgeFilter.add(psFilter);
        edgeFilter.add(peFilter);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JoanaGraph that = (JoanaGraph) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (edgeCount != null ? !edgeCount.equals(that.edgeCount) : that.edgeCount != null) return false;
        if (vertexCount != null ? !vertexCount.equals(that.vertexCount) : that.vertexCount != null) return false;
        if (vertexFilter != null ? !vertexFilter.equals(that.vertexFilter) : that.vertexFilter != null) return false;
        return edgeFilter != null ? edgeFilter.equals(that.edgeFilter) : that.edgeFilter == null;
    }
}
