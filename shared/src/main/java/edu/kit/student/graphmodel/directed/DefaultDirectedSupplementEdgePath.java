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

}
