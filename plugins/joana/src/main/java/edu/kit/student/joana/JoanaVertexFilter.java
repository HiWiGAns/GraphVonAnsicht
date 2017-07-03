package edu.kit.student.joana;

import edu.kit.student.graphmodel.Vertex;
import edu.kit.student.joana.JoanaVertex.VertexKind;
import edu.kit.student.objectproperty.GAnsProperty;
import edu.kit.student.plugin.VertexFilter;

import java.util.function.Predicate;

public class JoanaVertexFilter extends VertexFilter {

	private VertexKind kind;
	
	public JoanaVertexFilter(VertexKind kind) {
		super(kind.name());
		
		this.kind = kind;
	}

    @Override
    public String getGroup() {
	    return kind.backgroundColor.toString();
    }

    @Override
	public Predicate<Vertex> getPredicate() {
		return t -> {
            for (GAnsProperty<?> property : t.getProperties()) {
                if (property.getName().equals("nodeKind")) {
                    return  property.getValue().equals(kind);
                }
            }
            return false;
        };
	}

    @Override
    public String toString() {
        return "[JoanaVertexFilter: " + kind + "]";
    }
}
