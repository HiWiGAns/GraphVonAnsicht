package edu.kit.student.sugiyama.steps;

import edu.kit.student.graphmodel.Vertex;
import edu.kit.student.sugiyama.graph.ISugiyamaEdge;
import edu.kit.student.sugiyama.graph.ISugiyamaVertex;
import edu.kit.student.sugiyama.graph.IVertexPositionerGraph;
import edu.kit.student.sugiyama.graph.SugiyamaGraph;
import edu.kit.student.util.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * This class takes a directed graph and position its vertices in order to look more clearly. (e.g. position vertices in a row or column)
 */
public class VertexPositioner implements IVertexPositioner {

    private final Logger logger = LoggerFactory.getLogger(VertexPositioner.class);

	@Override
	public void positionVertices(IVertexPositionerGraph graph) {
		logger.info("VertexPositioner.positionVertices():");

		if (graph.getVertexSet().size() == 0) {
			return;
		}

		int maxwidth = graph.getLayers().stream().mapToInt(List::size).max().getAsInt();
		List<Segment> allsegments = new LinkedList<>();
		Set<ISugiyamaVertex> addedVertices = new HashSet<>();
		Map<Integer, List<ISugiyamaVertex>> segmentStarts = new HashMap<>();
		Map<Vertex, Segment> vertexToSegment = new HashMap<>();

		int[] horizontalWidth = new int[maxwidth*graph.getLayerCount()*10];
		int[] horizontalOffset = new int[maxwidth*graph.getLayerCount()*10];
		int horizontalSpacing = 2;
		int[] verticalHeight = new int[graph.getLayerCount()];
		int[] verticalOffset = new int[graph.getLayerCount()];
		int verticalSpacing = 70;
		
		for (int i = 0; i < maxwidth*4; i++) {
			segmentStarts.put(i, new LinkedList<>());
		}

		//logger.debug("setting initial positions for each vertex on each layer");
		for (int i = 0; i < graph.getLayerCount(); i++) {
			int j = 0;

			for (ISugiyamaVertex vertex : graph.getLayer(i)) {
				vertex.setX(j);
				vertex.setY(i);
				++j;
			}
		}

		//add all paths to segments
		List<SugiyamaGraph.SupplementPath> paths = new LinkedList<>();
		paths.addAll(graph.getSupplementPaths());
		paths.sort(Comparator.comparingInt(SugiyamaGraph.SupplementPath::getLength));
		//int counter = 0;

        //logger.debug("processing supplement paths");
		for (SugiyamaGraph.SupplementPath path : paths) {
			List<ISugiyamaVertex> vertices = path.getDummyVertices();
			ISugiyamaVertex source = path.getReplacedEdge().getSource();
			ISugiyamaVertex target = path.getReplacedEdge().getTarget();

			if (!addedVertices.contains(source)) {
				vertices.add(source);
				addedVertices.add(source);
			}
			if (!addedVertices.contains(target)) {
				vertices.add(target);
				addedVertices.add(target);
			}

			Segment probableNewSegment = new Segment(vertices);
			List<Segment> newSegments = probableNewSegment.cutWithSegments(allsegments);

			for (Segment segment : newSegments) {
				for (ISugiyamaVertex vertex : segment.getVertices()) {
					vertexToSegment.put(vertex, segment);
				}
			}

			//System.out.println("run " + counter++ + " adds " + newSegments.size() + " segments");

			allsegments.addAll(newSegments);
		}

		//logger.debug("adding edges to segments");
		//add all possible edges as segments
		for (ISugiyamaEdge edge : graph.getEdgeSet()) {
			if (edge.getSource().isDummy() || edge.getTarget().isDummy() || addedVertices.contains(edge.getSource()) || addedVertices.contains(edge.getTarget())) {
				continue;
			}
			if (edge.getTarget().getLayer() - edge.getSource().getLayer() == 0) {
				continue;
			}

			List<ISugiyamaVertex> vertices = new LinkedList<>();
			vertices.add(edge.getSource());
			vertices.add(edge.getTarget());
			addedVertices.add(edge.getSource());
			addedVertices.add(edge.getTarget());
			Segment probableNewSegment = new Segment(vertices);
			List<Segment> newSegments = probableNewSegment.cutWithSegments(allsegments);

			for (Segment segment : newSegments) {
				for (ISugiyamaVertex vertex : segment.getVertices()) {
					vertexToSegment.put(vertex, segment);
				}
			}

			allsegments.addAll(newSegments);
		}

		//logger.debug("adding all other vertices as segments");
		//add all other vertices as segments
		for (ISugiyamaVertex vertex : graph.getVertexSet()) {
			if (!vertex.isDummy() && !addedVertices.contains(vertex)) {
				List<ISugiyamaVertex> list = new LinkedList<>();
				list.add(vertex);
				Segment newSegment = new Segment(list);
				allsegments.add(newSegment);
				vertexToSegment.put(vertex, newSegment);
				addedVertices.add(vertex);
				//System.out.println("rest run " + counter++ + " adds 1 segments");
			}
		}
		//logger.debug("created " + allsegments.size() + " segments");

		//logger.debug("aligning segments");
		//make all segments a line by making all vertices align (pun may or may not be intended)
		for (Segment segment : allsegments) {
			segment.align(graph);
		}

		//logger.debug("sorting segments");
		allsegments.sort((o1, o2) -> (int)((o1.getBoundingBox().left - o2.getBoundingBox().left) * graph.getLayerCount() + (o1.getBoundingBox().top - o2.getBoundingBox().top)));
        //TODO: maybe avoid casting for more precise results


		//alternate and faster method for searching for intersections and move the intersecting segments.
		//maybe the moving of the single segments has to be improved because the graph gets very wide through moving the whole bigger segments

		//List<List<Segment>> segments = new LinkedList<>();
		//graph.getLayers().forEach(l->segments.add(l.stream().map(v->vertexToSegment.get(v)).collect(Collectors.toList())));//map vertices to their segment
        //Segment root = buildSegmentHierarchy(segments);
        //boolean intersection = root.intersectAndMoveChildren(graph);
        //boolean changes = true;
        //int runs = 0;
        //while(changes && runs < 0){
        //	changes = root.intersectAndMoveChildren(graph);
        //	runs++;
		//}
		//logger.debug("did " + runs +" runs in recursive moving of segments");
        //logger.debug("last was overlapping: " + changes);

		//logger.debug("looking for segment intersections");
		boolean changes = true;
		int runs = 0; //safeguard
		while (changes && runs < 1000) {
			changes = false;
			double overlapping = 0;
			int ctr = 0;

			for (Segment segment : allsegments) {
			    ctr++;
				for (Segment other : allsegments) {
					if (segment.getId() == other.getId()) {
						continue;
					}
					overlapping = segment.overlapping(other);
					//if(segment.intersects(other)){
					//	changes = true;
					//	other.move(1,graph);
					//}
					if (overlapping != 0) {
						changes = true;
						other.move(overlapping, graph);
					}
				}
			}

			runs++;
		}

		logger.debug("did " + runs + " runs, last was overlapping: " + changes);

		for (Vertex vertex : graph.getVertexSet()) {
			//logger.debug(vertex.getSize().toString());
			horizontalWidth[(int)vertex.getX()] = Math.max(horizontalWidth[(int)vertex.getX()], (int)Math.ceil(vertex.getSize().x));
			verticalHeight[(int)vertex.getY()] = Math.max(verticalHeight[(int)vertex.getY()], (int)Math.ceil(vertex.getSize().y));
			//TODO: as getX/Y returns a double this might not be the best solution
		}


		Set<ISugiyamaVertex> firstVertexOfLayer = graph.getLayers().stream().map(l->l.get(0)).collect(Collectors.toSet());
		int maxLeftMargin = firstVertexOfLayer.stream().mapToInt(v->v.getLeftRightMargin().x).max().getAsInt();
		horizontalOffset[0] = maxLeftMargin;

		verticalOffset[0] = 0;

		for (int i = 1; i < horizontalWidth.length; i++) {
			horizontalOffset[i] = horizontalOffset[i - 1] + horizontalWidth[i - 1] + horizontalSpacing;
		}

		for (int i = 1; i < verticalHeight.length; i++) {
			verticalOffset[i] = verticalOffset[i - 1] + verticalHeight[i - 1] + verticalSpacing;
		}

		for (Vertex vertex : graph.getVertexSet()) {
			vertex.setX(horizontalOffset[(int)vertex.getX()]);
			vertex.setY(verticalOffset[(int)vertex.getY()]);
		}
		//printPositions(graph.getLayers());
		//adjust left and right margins of all vertices in the graph
		this.adjustLeftAndRightMargin(graph.getLayers());
	}

	/*private void printPositions(List<List<ISugiyamaVertex>> layers){
		for(int i = 0; i < layers.size(); i++){
			List<ISugiyamaVertex> layer = layers.get(i);
			StringBuilder out = new StringBuilder("Layer " + i + ": ");
			for(ISugiyamaVertex v : layer){
				out.append("(").append(v.getX()).append("|").append(v.getY()).append("); ");
			}
			System.out.println(out);
		}
	}*/


	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		VertexPositioner that = (VertexPositioner) o;

		return logger != null ? logger.equals(that.logger) : that.logger == null;
	}

	@Override
	public int hashCode() {
		return logger != null ? logger.hashCode() : 0;
	}

	//builds a segment hierarchy for a given List of segments, and gives back one segments containing up to 4 children segments, which contain up to 4 child segments...
	//lists in the list describe different layer containing segment.
	//this method looks for duplicates in the given segment list. Its therefore possible to take all vertices on a layer and map them to the segment which contains them and give in to this method.
	private Segment buildSegmentHierarchy(List<List<Segment>> layers){
		assert(layers.size() > 0);
	    List<List<Segment>> newSegmentLayers = new ArrayList<>();
	    Set<Segment> processedSegments = new HashSet<>(); //Segments that have already been added to other segments.

        //assert that every vertex is in just one segment
        int remainingLayers = layers.size();
        int layerIdx = 0;
		List<List<Segment>> probablyNewChilds;
        while(remainingLayers > 0){
        	if(remainingLayers == 1){
        		List<Segment> layer = layers.get(layerIdx);
				probablyNewChilds = this.getChildSegmentsFromOneLayer(layer, 4);
				remainingLayers--;
				layerIdx++;
			}else{
				List<Segment> layer1 = layers.get(layerIdx);
				List<Segment> layer2 = layers.get(layerIdx+1);
				probablyNewChilds = this.getChildSegmentsFromTwoLayers(layer1,layer2,2,4);
				remainingLayers -= 2;
				layerIdx += 2;
			}
			List<Segment> newSegmentLayer = new LinkedList<>();
			for(List<Segment> l : probablyNewChilds){
				Set<Segment> newChilds = l.stream().filter(s->!processedSegments.contains(s)).collect(Collectors.toSet());
				if(!newChilds.isEmpty()){
					processedSegments.addAll(newChilds);
					newSegmentLayer.add(new Segment(newChilds));
				}
			}
			newSegmentLayers.add(newSegmentLayer);
        }
        if(layers.size() == 1){//end of recursion, now build one big segment from all segments in this single layer
        	Set<Segment> children = new HashSet<>(newSegmentLayers.get(0));
        	if(children.size() == 1){
        		return children.iterator().next();
			}else{
        		return new Segment(children);
			}

		}
		return buildSegmentHierarchy(newSegmentLayers);
    }

    /**
     * splits given list of Segments into new Lists of Segments.
	 * Given integer parameters may be greater than the actual amount of vertices to be added. In that case all remaining vertices will be added
     *
     * @param first first layer
     * @param second second layer
     * @param amountFromBoth amount of Segments that should be taken into one new List from both layer
     * @param singleLayerSize amount of Segments that should be taken into one new List from the remaining Segments of the bigger layer(for equal sized Lists take 2*amountFromBoth)
     * @return List of Lists of Segments
     */
    private List<List<Segment>> getChildSegmentsFromTwoLayers(List<Segment> first, List<Segment> second, int amountFromBoth, int singleLayerSize){
        if(first.isEmpty() && second.isEmpty()){ //both layers do not contain any elements
        	return new LinkedList<>();
		}
        int minVertexCount = Math.min(first.size(), second.size());
        int maxVertexCount = Math.max(first.size(), second.size());
        assert(amountFromBoth > 0);
        assert(singleLayerSize > 0);
        List<List<Segment>> segmentLists = new LinkedList<>();
        List<Segment> biggerLayer = first.size() > second.size() ? first : second;
        int remainingVertices = minVertexCount;
        int idx = 0; //idx for access in both layer
		while(remainingVertices > 0){
            List<Segment> childSegments = new LinkedList<>(); //segments to create a parent segment containing them
			if(remainingVertices < amountFromBoth){ //fill the remaining segments into a list
				for(int i = idx; i < idx + remainingVertices; i++){
					childSegments.add(first.get(i));
					childSegments.add(second.get(i));
				}
				segmentLists.add(childSegments);
				idx += remainingVertices;
				remainingVertices = 0;
			}else{ //fill amountFromBoth vertices from both layers into a list
				for(int i = idx; i < idx + amountFromBoth; i++){
					childSegments.add(first.get(i));
					childSegments.add(second.get(i));
				}
				segmentLists.add(childSegments);
				idx += amountFromBoth;
				remainingVertices -= amountFromBoth;
			}
        }
        //now handle the bigger layer
		segmentLists.addAll(this.getChildSegmentsFromOneLayer(biggerLayer.subList(idx,maxVertexCount),singleLayerSize));
		return segmentLists;
	}

    /**
     * splits given list of Segments into new Lists of Segments containing #size Segments
     */
    private List<List<Segment>> getChildSegmentsFromOneLayer(List<Segment> layer, int size){
        List<List<Segment>> segmentLists = new LinkedList<>();
        if(layer.isEmpty()){ return segmentLists;}
        int idx = 0;
        List<Segment> subList = new LinkedList<>();
        for(Segment s : layer){
            idx++;
            subList.add(s);
            if(idx == size){
                segmentLists.add(subList);
                idx = 0;
                subList = new LinkedList<>();

            }
        }
        if(idx != 0){ //adds the remaining elements to the final list, (layer.size % size != 0)
			segmentLists.add(subList);
		}
        return segmentLists;
    }

    //TODO: necessary anymore ?
    private void intersectSegmentsRecursively(Segment root){
	    for(Segment s : root.children){
	        for(Segment o : root.children){
	            //TODO: adjust here the segments positions
            }
        }
        if(root.children.isEmpty()){
	        //TODO: here adjust possibly intersection of the last segment, that only holds vertices
        }
        //call recursicely for each child, so there aren't any intersections later
        root.children.forEach(this::intersectSegmentsRecursively);
    }
	
	private void adjustLeftAndRightMargin(List<List <ISugiyamaVertex>> layers){
	    //logger.debug("adjusting left and right margins");
		for(List<ISugiyamaVertex> layer : layers){
			assert(!layer.isEmpty());
//			int offsetRight = 0; //optional. every distance a single vertex has been moved is added here so all following vertices are also moved this much, even if the distance between both vertices is enough
			//first move the first vertex more right if he is very left (x-coord 0)
			ISugiyamaVertex fst = layer.get(0);
			if(fst.getX() < fst.getLeftRightMargin().x){
				fst.setX(fst.getLeftRightMargin().x);
			}
			for(int i = 1; i < layer.size(); i++){
				//adjust here left and right margins
				//is it necessary to move all vertices in a layer if only one has been moved, or just look whether the dist betw. vetrtices is enough ?
				ISugiyamaVertex left = layer.get(i-1);
				ISugiyamaVertex right = layer.get(i);
				int distBetweenVertices = left.getLeftRightMargin().y + right.getLeftRightMargin().x;//necessary space betw. vertices i-1,i
				if(right.getX() < left.getX() + left.getSize().x + distBetweenVertices){
					double newX = (left.getX() + left.getSize().x + distBetweenVertices);
					right.setX(newX);
				}
			}
			//TODO: maybe adjust graph size so that the last vertex has the space rightmost that he wants
		}
	}

	private class Segment {

		private List<ISugiyamaVertex> vertices;
		private boolean corrected;
		private boolean changed;
		private BoundingBox boundingBox;
		private int id;
		private int parentId;
		private Set<Segment> children;



        /**
         * Basic case. A segment which Bounding Box is described by position of the given vertices.
         * The given vertices mustn't intersect with others (rather not having the same coordinates than any other)
         */
		Segment(List<ISugiyamaVertex> vertices) {
		    assert(!vertices.isEmpty());
			this.vertices = vertices;
			this.vertices.sort(Comparator.comparingInt(ISugiyamaVertex::getLayer));
			this.corrected = false;
			this.changed = true;
			this.id = IdGenerator.getInstance().createId();
			this.parentId = -1;
			this.children = new HashSet<>();
		}

        /**
         * A segment with child segments.
         * Its BoundingBox is calculated from its child ones.
         *
         */
		Segment(Set<Segment> segments) { //
            assert(!segments.isEmpty());
            this.vertices = new LinkedList<>();
            segments.forEach(s -> this.vertices.addAll(s.getVertices()));
            this.vertices.sort(Comparator.comparingInt(ISugiyamaVertex::getLayer));
            this.changed = true;
            this.id = IdGenerator.getInstance().createId();
            segments.forEach(s -> s.parentId = this.id);
            this.parentId = -1;
            this.children = segments;
        }

		public List<ISugiyamaVertex> getVertices() {
			return vertices;
		}

        public Set<Segment> getChildren() {
            return children;
        }

        public int getParentId() {
            return parentId;
        }

		/**
		 * looks for the segments children, if any of these are intersecting. If so, the children are moved adequately.
		 * Calls this method recursively for every child until the children only contain vertices and no other segments.
		 */
		public boolean intersectAndMoveChildren(IVertexPositionerGraph graph){
			boolean intersection = false;
			if(!this.children.isEmpty()){
				double overlapping = 0;
				for(Segment s1 : this.children){
					for(Segment s2: this.children){
						overlapping = s1.overlapping(s2);
						if(overlapping != 0){
							intersection = true;
							s2.move(overlapping,graph);
						}
					}
				}
				//now call recursively for every childs children
				for(Segment c : this.children){
					intersection = intersection || c.intersectAndMoveChildren(graph);
				}
			}else{ //the child has no children, now look for its vertices to intersect
				//normally this should not happen!
			}
			return intersection;
		}

        //move this segment in x direction with given amount
		void move(double amount, IVertexPositionerGraph graph) {
			for (ISugiyamaVertex vertex : this.vertices) {
				graph.setX(vertex, vertex.getX() + amount);
			}
			this.getBoundingBox().moveX(amount); //also move box and don't set change flag, so we don't have to recalculate the bounding box
			//this.changed = true;
		}

		void align(IVertexPositionerGraph graph) {
			double x = this.vertices.get(0).getX();

			for (ISugiyamaVertex vertex : this.vertices) {
				graph.setX(vertex, x);
			}
			//TODO: maybe improve this method that it calculates the new size of the bounding box instead of setting the changed flag. Could be a bit faster this way
			this.changed = true;
		}

		List<Segment> cutWithSegments(List<Segment> others) {
			List<Segment> result = new LinkedList<>();

			boolean stop = false;
			for (Segment segment : others)  {
				if (!intersects(segment)) {
					continue;
				}

				List<ISugiyamaVertex> otherVertices = segment.vertices;
				int vOffset = this.vertices.get(0).getLayer();
				int oOffset = otherVertices.get(0).getLayer();

				int startLayer = Math.max(vOffset, oOffset);
				int endLayer = Math.min(this.vertices.get(this.vertices.size() - 1).getLayer(), otherVertices.get(otherVertices.size() - 1).getLayer());


				if (endLayer - startLayer == 0) {
					continue;
				}

				boolean firstIsLeft;
				firstIsLeft = (this.vertices.get(startLayer - vOffset).getX() - otherVertices.get(startLayer - oOffset).getX()) < 0;

				for (int i = startLayer + 1; i < endLayer; i++) {

					boolean isStillLeft = (this.vertices.get(i - vOffset).getX() - otherVertices.get(i - oOffset).getX()) < 0;

					if (firstIsLeft != isStillLeft) {
						result.addAll((new Segment(new LinkedList<>(this.vertices.subList(0, i - vOffset))).cutWithSegments(others)));
						result.addAll((new Segment(new LinkedList<>(this.vertices.subList(i - vOffset, this.vertices.size()))).cutWithSegments(others)));
						stop = true;
						break;
					}

					firstIsLeft = isStillLeft;
				}

				if (stop) {
					break;
				}
			}

			if (result.size() == 0) {
				result.add(this);
			}

			return result;
		}

		@Override
		public String toString() {
			return "Segment{" +
					"vertices=" + vertices +
					", changed=" + changed +
					", boundingBox=" + boundingBox +
					", id=" + id +
					'}';
		}

		private boolean intersects(Segment other) {
			return getBoundingBox().intersects(other.getBoundingBox());
		}

		private double overlapping(Segment other){ return getBoundingBox().overlapping(other.getBoundingBox());}

		public int getId() {
			return id;
		}

		BoundingBox getBoundingBox() {
			if (changed) {
				double startX = Integer.MAX_VALUE;
				double startY = Integer.MAX_VALUE;
				double endX = 0;
				double endY = 0;

				if(this.children.isEmpty()){
                    for (ISugiyamaVertex vertex : vertices) {
                        if (vertex.getX() < startX)
                            startX = vertex.getX();
                        if (vertex.getY() < startY)
                            startY = vertex.getY();
                        if (vertex.getX() > endX)
                            endX = vertex.getX();
                        if (vertex.getY() > endY)
                            endY = vertex.getY();
                    }
                }else{
				    for(Segment s : this.children){
				        BoundingBox bb = s.getBoundingBox();
				        if(bb.left < startX)
				            startX = bb.left;
				        if(bb.top < startY)
				            startY = bb.top;
				        if(bb.right > endX)
				            endX = bb.right;
				        if(bb.bottom > endY)
				            endY = bb.bottom;
                    }
                }

				this.boundingBox = new BoundingBox(startX, startY, endX, endY);
				changed = false;
			}

			return boundingBox;
		}

		private class BoundingBox {
			@Override
			public boolean equals(Object o) {
				if (this == o) return true;
				if (o == null || getClass() != o.getClass()) return false;

				BoundingBox that = (BoundingBox) o;

				if (Double.compare(that.left, left) != 0) return false;
				if (Double.compare(that.top, top) != 0) return false;
				if (Double.compare(that.right, right) != 0) return false;
				return Double.compare(that.bottom, bottom) == 0;
			}

			@Override
			public int hashCode() {
				int result;
				long temp;
				temp = Double.doubleToLongBits(left);
				result = (int) (temp ^ (temp >>> 32));
				temp = Double.doubleToLongBits(top);
				result = 31 * result + (int) (temp ^ (temp >>> 32));
				temp = Double.doubleToLongBits(right);
				result = 31 * result + (int) (temp ^ (temp >>> 32));
				temp = Double.doubleToLongBits(bottom);
				result = 31 * result + (int) (temp ^ (temp >>> 32));
				return result;
			}

			private double left;
			private double top;
			private double right;
			private double bottom;

			BoundingBox(double left, double top, double right, double bottom) {
				this.left = left;
				this.top = top;
				this.right = right;
				this.bottom = bottom;
			}

            /**
             * moves this in x direction with the given amount
             *
             * @param amount amount to move this in x-direction
             */
			void moveX(double amount){
			    this.left += amount;
			    this.right += amount;
            }

			public void setLeft(int left){
			    this.left = left;
            }

            public void setTop(int top) {
                this.top = top;
            }

            public void setRight(int right) {
                this.right = right;
            }

            public void setBottom(int bottom) {
                this.bottom = bottom;
            }

            /**
             * returns amount of values both bounding boxes are overlapping
             */
            double overlapping(BoundingBox other){
                double dist = 0;
			    if(!intersects(other))  return dist;
                dist = 1; //possibly both have same left and right values, then a movement of 1 is enough
                if(other.left < this.left)  dist = other.right - this.left; //should not occur, just other will be moved and just in right direction
                else if(other.right > this.right)   dist = this.right - other.left;
                return dist;
            }

            boolean intersects(BoundingBox other) {
				return !(other.left > this.right
						|| other.right < this.left
						|| other.top > this.bottom
						|| other.bottom < this.top);
			}

			@Override
			public String toString() {
				return "BoundingBox{" +
						"left=" + left +
						", top=" + top +
						", right=" + right +
						", bottom=" + bottom +
						'}';
			}
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			Segment segment = (Segment) o;

			if (corrected != segment.corrected) return false;
			if (changed != segment.changed) return false;
			if (id != segment.id) return false;
			if (parentId != segment.parentId) return false;
			if (vertices != null ? !vertices.equals(segment.vertices) : segment.vertices != null) return false;
			if (boundingBox != null ? !boundingBox.equals(segment.boundingBox) : segment.boundingBox != null)
				return false;
			return children != null ? children.equals(segment.children) : segment.children == null;
		}

		@Override
		public int hashCode() {
			int result = vertices != null ? vertices.hashCode() : 0;
			result = 31 * result + (corrected ? 1 : 0);
			result = 31 * result + (changed ? 1 : 0);
			result = 31 * result + (boundingBox != null ? boundingBox.hashCode() : 0);
			result = 31 * result + id;
			result = 31 * result + parentId;
			result = 31 * result + (children != null ? children.hashCode() : 0);
			return result;
		}
	}
}
