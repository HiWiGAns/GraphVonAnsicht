package edu.kit.student.joana.methodgraph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.kit.student.graphmodel.builder.GraphBuilderException;
import edu.kit.student.graphmodel.builder.IEdgeBuilder;
import edu.kit.student.graphmodel.builder.IGraphBuilder;
import edu.kit.student.graphmodel.builder.IVertexBuilder;
import edu.kit.student.joana.*;
import edu.kit.student.joana.JoanaVertex.VertexKind;

/**
 * The MethodGraphBuilder is a {@link IGraphBuilder}, specifically for building
 * {@link MethodGraph}.
 */
public class MethodGraphBuilder implements IGraphBuilder {

    private String name;
    private Map<String, String> data = new HashMap<>();

    private final JoanaObjectPool joanaObjectPool;

    private Set<JoanaVertexBuilder> vertexBuilders = new HashSet<>();
    private Set<String> vertexIds = new HashSet<>();
    private HashMap<String, JoanaVertex> vertices = new HashMap<>();

    private Set<JoanaEdge> edges = new HashSet<>();
    private Set<JoanaEdgeBuilder> edgeBuilders = new HashSet<>();


    /**
     * Constructor for {@link MethodGraphBuilder} which is created by a {@link edu.kit.student.joana.callgraph.CallGraphBuilder}.
     * 
     * @param name of the {@link MethodGraph}
     */
    public MethodGraphBuilder(String name, JoanaObjectPool pool) {
        this.name = name;
        this.joanaObjectPool = pool;
    }

    @Override
    public IEdgeBuilder getEdgeBuilder(String sourceId, String targetId) {
        JoanaEdgeBuilder builder = new JoanaEdgeBuilder(sourceId, targetId);
        edgeBuilders.add(builder);
        return builder;
    }

    @Override
    public IVertexBuilder getVertexBuilder(String vertexId) {
        JoanaVertexBuilder builder = new JoanaVertexBuilder(vertexId, joanaObjectPool);
        vertexIds.add(vertexId);
        vertexBuilders.add(builder);
        return builder;
    }

    @Override
    public IGraphBuilder getGraphBuilder(String graphId) {
        //is not allowed to happen
        return null;
    }

    @Override
    public String getId() {
        return this.name;
    }

    @Override
    public void addData(String keyname, String value) throws IllegalArgumentException {
        this.data.put(keyname, value);
    }

    /**
     * Returns true if a vertex with the specified ID is present in this builder.
     *
     * @param vertexId the ID of the vertex
     * @return true if the vertex is present, false otherwise.
     */
    public boolean containsVertexWithId(String vertexId) {
        return vertexIds.contains(vertexId);
    }

    /**
     * Returns a mapping of vertex names to vertices, after the graph was build.
     *
     * @return the vertices.
     */
    public HashMap<String, JoanaVertex> getVertexPool() {
        return this.vertices;
    }

    public Set<String> getVertexIds() {
        return this.vertexIds;
    }

    /**
     * Builds the {@link MethodGraph}, which has been described before this method is called.
     * @return the built {@link MethodGraph}
     * @throws GraphBuilderException if the {@link MethodGraph} could not be build.
     */
    public MethodGraph build() throws GraphBuilderException {
        for (JoanaVertexBuilder builder : vertexBuilders) {	
            JoanaVertex v = builder.build();
            vertices.put(v.getName(), v);
        }
        
        for (JoanaEdgeBuilder builder : edgeBuilders) {
            edges.add(builder.build(vertices));
        }

        String name = "";
        for (JoanaVertex v : vertices.values()) {
            if (v.getNodeKind() == VertexKind.ENTR)
            {
                //TODO: maybe some editing to transform the raw classnames 
                // into something more readable
                name = v.getNodeBcName();
            }
        }
        if (name.equals("")) {
            name = "MethodGraph without ENTR Vertex.";
        }

        return new MethodGraph(new HashSet<>(vertices.values()), edges, name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MethodGraphBuilder that = (MethodGraphBuilder) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (data != null ? !data.equals(that.data) : that.data != null) return false;
        if (joanaObjectPool != null ? !joanaObjectPool.equals(that.joanaObjectPool) : that.joanaObjectPool != null)
            return false;
        if (vertexBuilders != null ? !vertexBuilders.equals(that.vertexBuilders) : that.vertexBuilders != null)
            return false;
        if (vertexIds != null ? !vertexIds.equals(that.vertexIds) : that.vertexIds != null) return false;
        if (vertices != null ? !vertices.equals(that.vertices) : that.vertices != null) return false;
        if (edges != null ? !edges.equals(that.edges) : that.edges != null) return false;
        return edgeBuilders != null ? edgeBuilders.equals(that.edgeBuilders) : that.edgeBuilders == null;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (data != null ? data.hashCode() : 0);
        result = 31 * result + (joanaObjectPool != null ? joanaObjectPool.hashCode() : 0);
        result = 31 * result + (vertexBuilders != null ? vertexBuilders.hashCode() : 0);
        result = 31 * result + (vertexIds != null ? vertexIds.hashCode() : 0);
        result = 31 * result + (vertices != null ? vertices.hashCode() : 0);
        result = 31 * result + (edges != null ? edges.hashCode() : 0);
        result = 31 * result + (edgeBuilders != null ? edgeBuilders.hashCode() : 0);
        return result;
    }
}
