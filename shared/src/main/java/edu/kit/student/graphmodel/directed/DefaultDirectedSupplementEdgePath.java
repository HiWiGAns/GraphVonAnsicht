package edu.kit.student.graphmodel.directed;

import java.util.List;

import edu.kit.student.graphmodel.DirectedSupplementEdgePath;
import edu.kit.student.graphmodel.Vertex;

public class DefaultDirectedSupplementEdgePath implements DirectedSupplementEdgePath{

	private final DirectedEdge replacedEdge;
	private final List<Vertex> dummies;
	private final List<DirectedEdge> supplementEdges;
	
	public DefaultDirectedSupplementEdgePath(DirectedEdge replacedEdge, List<Vertex> dummies, List<DirectedEdge> supplementEdges){
		this.replacedEdge = replacedEdge;
		this.dummies = dummies;
		this.supplementEdges = supplementEdges;
		testSupplementPath();
	}
	
	@Override
	public int getLength() {
		return this.dummies.size();
	}

	@Override
	public List<Vertex> getDummyVertices() {
		return this.dummies;
	}

	@Override
	public List<DirectedEdge> getSupplementEdges() {
		return this.supplementEdges;
	}

	@Override
	public DirectedEdge getReplacedEdge() {
		return this.replacedEdge;
	}
	
	//just for testing the path 
	private void testSupplementPath(){
		assert(!dummies.isEmpty());
		assert(!supplementEdges.isEmpty());
		assert(dummies.size() == supplementEdges.size() - 1);
		assert(this.supplementEdges.get(0).getSource().getID().equals(this.replacedEdge.getSource().getID()));
		assert(this.supplementEdges.get(this.supplementEdges.size() - 1).getTarget().getID().equals(this.replacedEdge.getTarget().getID()));
		for(int i = 0; i < this.supplementEdges.size() - 1; i++){
			assert(this.supplementEdges.get(i).getTarget().getID().equals(this.supplementEdges.get(i+1).getSource().getID()));
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		DefaultDirectedSupplementEdgePath that = (DefaultDirectedSupplementEdgePath) o;

		if (replacedEdge != null ? !replacedEdge.equals(that.replacedEdge) : that.replacedEdge != null) return false;
		if (dummies != null ? !dummies.equals(that.dummies) : that.dummies != null) return false;
		return supplementEdges != null ? supplementEdges.equals(that.supplementEdges) : that.supplementEdges == null;
	}

	@Override
	public int hashCode() {
		int result = replacedEdge != null ? replacedEdge.hashCode() : 0;
		result = 31 * result + (dummies != null ? dummies.hashCode() : 0);
		result = 31 * result + (supplementEdges != null ? supplementEdges.hashCode() : 0);
		return result;
	}
}
