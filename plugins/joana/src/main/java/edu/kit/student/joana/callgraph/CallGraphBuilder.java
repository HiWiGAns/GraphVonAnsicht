package edu.kit.student.joana.callgraph;

import edu.kit.student.graphmodel.builder.GraphBuilderException;
import edu.kit.student.graphmodel.builder.IEdgeBuilder;
import edu.kit.student.graphmodel.builder.IGraphBuilder;
import edu.kit.student.graphmodel.builder.IVertexBuilder;
import edu.kit.student.joana.CallGraphVertex;
import edu.kit.student.joana.InterproceduralEdge;
import edu.kit.student.joana.JoanaEdge;
import edu.kit.student.joana.JoanaEdge.EdgeKind;
import edu.kit.student.joana.JoanaEdgeBuilder;
import edu.kit.student.joana.JoanaObjectPool;
import edu.kit.student.joana.JoanaVertex;
import edu.kit.student.joana.methodgraph.MethodGraph;
import edu.kit.student.joana.methodgraph.MethodGraphBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The CallGraphBuilder implements an {@link IGraphBuilder} and builds 
 * one {@link CallGraph}.
 */
public class CallGraphBuilder implements IGraphBuilder {

    private final Logger logger = LoggerFactory.getLogger(CallGraphBuilder.class);

    private final Map<String, String> data = new HashMap<>();

    private final Map<String, MethodGraphBuilder> vertexIDToMGBuilder = new HashMap<>();
    private final Set<MethodGraphBuilder> methodGraphBuilders = new HashSet<>();
    // contains all MethodGraphBuilder whose vertices have not been added to this vertexIDToMGBuilder mapping.
    private final Set<MethodGraphBuilder> newMGBuilders = new HashSet<>();

    private final JoanaObjectPool joanaObjectPool;
    private final Set<MethodGraph> methodGraphs = new HashSet<>();
    private final Set<JoanaEdgeBuilder> callEdgeBuilders = new HashSet<>();
    private String name;

    public CallGraphBuilder(String name, JoanaObjectPool pool) {
        this.name = name;
        this.joanaObjectPool = pool;
    }

    @Override
    public IEdgeBuilder getEdgeBuilder(String sourceId, String targetId) {
        collectMethodGraphBuilders();
        MethodGraphBuilder sourceMG = vertexIDToMGBuilder.get(sourceId);
        MethodGraphBuilder targetMG = vertexIDToMGBuilder.get(targetId);
        if (sourceMG != null && sourceMG == targetMG) {
            // Return edge between to vertices in one method graph
            return sourceMG.getEdgeBuilder(sourceId, targetId);
        }
        // Found edge between two method graphs
        JoanaEdgeBuilder eBuilder = new JoanaEdgeBuilder(sourceId, targetId);
        callEdgeBuilders.add(eBuilder);
        return eBuilder;
    }

    @Override
    public IVertexBuilder getVertexBuilder(String vertexId) {
        //TODO: throw exception
        return null;
    }

    @Override
    public IGraphBuilder getGraphBuilder(String graphId) {
        MethodGraphBuilder builder = new MethodGraphBuilder(graphId, joanaObjectPool);
        methodGraphBuilders.add(builder);
        newMGBuilders.add(builder);
        return builder;
    }

    /**
     * Collects all MethodGraphBuilder who have been added since
     * the last call of this function or the construction of this CallGraphBuilder.
     * Collecting means currently adding them to the mapping from vertexIds to MethodGraphBuilder.
     */
    private void collectMethodGraphBuilders() {
        for (MethodGraphBuilder builder : newMGBuilders) {
            for (String vId : builder.getVertexIds()) {
                vertexIDToMGBuilder.put(vId, builder);
            }
        }
        newMGBuilders.clear();

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
     * Builds a new {@link CallGraph} with the given information, added before this call.
     * @return the {@link CallGraph}
     * @throws GraphBuilderException if the {@link CallGraph} could not be build.
     */
    public CallGraph build() throws GraphBuilderException {
        HashMap<String, JoanaVertex> vertexPool = new HashMap<>();

        long startTime = System.currentTimeMillis();

        final HashMap<String, MethodGraph> vertexIDToMG = new HashMap<>();
        logger.info(methodGraphBuilders.size() + " MethodGraphs to build.");
        for (MethodGraphBuilder b : methodGraphBuilders) {
            MethodGraph mg = b.build();
            methodGraphs.add(mg);
            vertexPool.putAll(b.getVertexPool());
            for (String vId : b.getVertexPool().keySet()) {
                vertexIDToMG.put(vId, mg);
            }
        }
        methodGraphBuilders.clear();
        vertexIDToMGBuilder.clear();

        long stopTime = System.currentTimeMillis();
        logger.info("Building MethodGraphs took " + (stopTime - startTime));
        startTime = stopTime;

        HashMap<Integer, CallGraphVertex> vertices = new HashMap<>();
        HashMap<CallGraphVertex, Set<CallGraphVertex>> connections = new HashMap<>();
        Set<JoanaEdge> edges = new HashSet<>();

        // Generate Callgraph
        // Generate CallGraphVertices for every method.
        for (MethodGraph methodGraph : methodGraphs) {
            vertices.put(methodGraph.getID(), new CallGraphVertex(methodGraph.getName(), methodGraph.getName(), methodGraph));
            connections.put(vertices.get(methodGraph.getID()), new HashSet<>());
        }
        stopTime = System.currentTimeMillis();
        logger.info("Building CallGraphVertices took " + (stopTime - startTime));
        startTime = stopTime;

        // Build the calledges in the method graphs.
        // This should be temporary. Better would be if they would really get built in the method graph builder.
        Set<JoanaEdge> callEdges = new HashSet<>();
        for (JoanaEdgeBuilder builder : callEdgeBuilders) {
            callEdges.add(builder.build(vertexPool));
        }

        stopTime = System.currentTimeMillis();
        logger.info("Building CallEdges in MethodGraphs took " + (stopTime - startTime));
        startTime = stopTime;

        //edges between two MethodGraphs. All InterproceduralEdges can be found in callEdges, these are also edges not from the same MethodGraph.
        //search in callEdges for interproceduralEdges, create a new InterproceduralEdge for both MethodGraphs the edge connects
        HashMap<MethodGraph, Set<InterproceduralEdge>> mgToIESet = new HashMap<>();
        //add a mapping of MethodGraph id to set of interprocedural edges. (its done for all MethodGraphs)
        for(MethodGraph mg : vertexIDToMG.values()){
        	mgToIESet.put(mg, new HashSet<>());
        }
        logger.info(callEdges.size() + " callEdges to process.");
        for(JoanaEdge callEdge : callEdges){
        	JoanaVertex source = callEdge.getSource();
        	JoanaVertex target = callEdge.getTarget();
        	MethodGraph sourceMG = vertexIDToMG.get(source.getName());
        	MethodGraph targetMG = vertexIDToMG.get(target.getName());
        	//adding two interproc edges to MG of source vertex, and target respectively
            mgToIESet.get(sourceMG).add(new InterproceduralEdge(callEdge, sourceMG, targetMG, InterproceduralEdge.DummyLocation.TARGET));
            mgToIESet.get(targetMG).add(new InterproceduralEdge(callEdge, targetMG, sourceMG, InterproceduralEdge.DummyLocation.SOURCE));
        }
        stopTime = System.currentTimeMillis();
        logger.info("Building interprocedural edges took " + (stopTime - startTime));
        startTime = stopTime;
        //set the interprocedural edges of a method graph
        for(MethodGraph mg : methodGraphs){
        	if(mgToIESet.containsKey(mg)){
        		mg.setInterproceduralEdges(mgToIESet.get(mg));
        	}
        }
        stopTime = System.currentTimeMillis();
        logger.info("Setting interprocedural vertices took " + (stopTime - startTime));
        startTime = stopTime;

        
        //search for call loops
        //maybe there is a better/faster solution without searching through all edges
        for (MethodGraph methodGraph : methodGraphs) {
            for (JoanaEdge edge : methodGraph.getEdgeSet()) {
                if (edge.getEdgeKind() == JoanaEdge.EdgeKind.CL) {
                    callEdges.add(edge);
                }
            }
        }
        stopTime = System.currentTimeMillis();
        logger.info("Searching calloops vertices took " + (stopTime - startTime));
        startTime = stopTime;

        int joanaEdgeCtr = 0;
        for (JoanaEdge callEdge : callEdges) {
            if (callEdge.getEdgeKind() != EdgeKind.CL)
                continue;
            joanaEdgeCtr++;
            // Find which methodgraph contains the target and the source vertex for the callEdge
            int sourceID = vertexIDToMG.get(callEdge.getSource().getName()).getID();
            int targetID = vertexIDToMG.get(callEdge.getTarget().getName()).getID();
            if (connections.get(vertices.get(sourceID)).contains(vertices.get(targetID))) {
                // Second call from this function. Skip.
                continue;
            }
            edges.add(new JoanaEdge(callEdge.getName(), callEdge.getLabel(), vertices.get(sourceID), vertices.get(targetID), EdgeKind.CL));
            connections.get(vertices.get(sourceID)).add(vertices.get(targetID));
        }
        stopTime = System.currentTimeMillis();
        logger.info("Building " + joanaEdgeCtr + " joanaedges for calledges took " + (stopTime - startTime));

        return new CallGraph(this.name, new HashSet<>(vertices.values()), edges);

    }
}
