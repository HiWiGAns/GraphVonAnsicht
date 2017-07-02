package edu.kit.student.joana;

import edu.kit.student.graphmodel.VertexReference;
import edu.kit.student.joana.methodgraph.MethodGraph;
import edu.kit.student.objectproperty.GAnsProperty;

import java.util.List;

public class CallGraphVertex extends JoanaVertex {
	
    private MethodGraph methodGraph;

	public CallGraphVertex(String name, String label, MethodGraph graph) {
		super(name, label, graph.getEntryVertex().getNodeKind(), graph.getEntryVertex().getNodeSource(),
				graph.getEntryVertex().getNodeProc(), graph.getEntryVertex().getNodeOperation(),
				graph.getEntryVertex().getNodeBcName(), graph.getEntryVertex().getNodeBcIndex(),
				graph.getEntryVertex().getNodeSr(), graph.getEntryVertex().getNodeSc(),
				graph.getEntryVertex().getNodeEr(), graph.getEntryVertex().getNodeEc(),
				graph.getEntryVertex().getNodelLocalDef(), graph.getEntryVertex().getNodeLocalUse());
		this.methodGraph = graph;
	}

	public MethodGraph getGraph() {
		return this.methodGraph;
	}

	@Override
	public String getLabel() {
		return methodGraph.getEntryVertex().getLabel();
	}
	
	@Override
	public VertexKind getNodeKind() {
		return methodGraph.getEntryVertex().getNodeKind();
	}

	@Override
	public JavaSource getNodeSource() {
		return methodGraph.getEntryVertex().getNodeSource();
	}

	@Override
	public Integer getNodeProc() {
		return methodGraph.getEntryVertex().getNodeProc();
	}

	@Override
	public Operation getNodeOperation() {
		return methodGraph.getEntryVertex().getNodeOperation();
	}

	@Override
	public String getNodeBcName() {
		return methodGraph.getEntryVertex().getNodeBcName();
	}

	@Override
	public Integer getNodeBcIndex() {
		return methodGraph.getEntryVertex().getNodeBcIndex();
	}

	@Override
	public Integer getNodeSr() {
		return methodGraph.getEntryVertex().getNodeSr();
	}

	@Override
	public Integer getNodeSc() {
		return methodGraph.getEntryVertex().getNodeSc();
	}

	@Override
	public Integer getNodeEr() {
		return methodGraph.getEntryVertex().getNodeEr();
	}

	@Override
	public Integer getNodeEc() {
		return methodGraph.getEntryVertex().getNodeEc();
	}

	@Override
    public VertexReference getLink() {
	    return new VertexReference(getGraph(), null);
    }

	@Override
    public List<GAnsProperty<?>> getProperties() {
	    // TODO: Add own properties like e.g. call count
        return super.getProperties();
    }
}

