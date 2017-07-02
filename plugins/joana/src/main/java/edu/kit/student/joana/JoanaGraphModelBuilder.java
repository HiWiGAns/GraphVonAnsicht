package edu.kit.student.joana;

import edu.kit.student.graphmodel.GraphModel;
import edu.kit.student.graphmodel.builder.GraphBuilderException;
import edu.kit.student.graphmodel.builder.IGraphBuilder;
import edu.kit.student.graphmodel.builder.IGraphModelBuilder;
import edu.kit.student.joana.callgraph.CallGraph;
import edu.kit.student.joana.callgraph.CallGraphBuilder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The JoanaGraphModelBuilder implements the {@link IGraphModelBuilder} and
 * creates a {@link JoanaGraphModel}.
 */
public class JoanaGraphModelBuilder implements IGraphModelBuilder, JoanaObjectPool {

    private JoanaWorkspace workspace;
    private CallGraphBuilder callBuilder;
    private Map<String, JavaSource> fileNamesToJavaSource = new HashMap<>();

    public JoanaGraphModelBuilder(JoanaWorkspace workspace) {
        this.workspace = workspace;
    }

    @Override
    public IGraphBuilder getGraphBuilder(String graphId) {
        //check if callgraphbuilder
        if (graphId.equals("callgraph")) {
            callBuilder = new CallGraphBuilder(graphId, this);
            return callBuilder;
        }
        return null;
    }

    @Override
    public GraphModel build() throws GraphBuilderException {
        CallGraph callGraph = callBuilder.build();
        JoanaGraphModel model = new JoanaGraphModel(callGraph);
        workspace.setGraphModel(model);
        return model;
    }

    @Override
    public Set<JavaSource> getJavaSources() {
        return new HashSet<>(fileNamesToJavaSource.values());
    }

    @Override
    public JavaSource getJavaSource(String sourceName) {
        if (fileNamesToJavaSource.containsKey(sourceName))
            return fileNamesToJavaSource.get(sourceName);

        JavaSource source = new JavaSource(sourceName);
        fileNamesToJavaSource.put(sourceName, source);
        return source;
    }
}
