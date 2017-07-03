package edu.kit.student.joana;

import edu.kit.student.graphmodel.Edge;
import edu.kit.student.joana.JoanaEdge.EdgeKind;
import edu.kit.student.objectproperty.GAnsProperty;
import edu.kit.student.plugin.EdgeFilter;

import java.util.function.Predicate;

public class JoanaEdgeFilter extends EdgeFilter {

	private EdgeKind kind;

	public JoanaEdgeFilter(EdgeKind kind) {
	    super(kind.name());
		this.kind = kind;
	}

    @Override
    public String getGroup() {
        return kind.color().toString();
    }

    @Override
	public Predicate<Edge> getPredicate() {
		return t -> {
            for (GAnsProperty<?> property : t.getProperties()) {
                if (property.getName().equals("edgeKind")) {
                    return property.getValue().equals(kind);
                }
            }
            return false;
        };
	}

    @Override
    public String toString() {
	    return "[JoanaEdgefilter: " + kind + "]";
    }
}
