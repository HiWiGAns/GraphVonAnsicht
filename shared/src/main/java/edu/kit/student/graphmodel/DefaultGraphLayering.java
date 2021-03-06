package edu.kit.student.graphmodel;

import edu.kit.student.util.IntegerPoint;
import edu.kit.student.util.MutableIntegerPoint;

import java.util.*;

public class DefaultGraphLayering<V extends Vertex> implements GraphLayering<V> {
    
    private ArrayList<ArrayList<V>> layers = new ArrayList<>();
    private Map<V, MutableIntegerPoint> vertexToPoint = new HashMap<>();
    
    public DefaultGraphLayering(Set<V> vertices) {
        this.layers.add(new ArrayList<>(vertices));
        for (V vertex : vertices) {
            vertexToPoint.put(vertex, MutableIntegerPoint.zero());
        }
    }

    @Override
    public int getLayerWidth(int layerN) {
        return layers.get(layerN).size();
    }

    @Override
    public int getMaxWidth() {
        int maxWidth = 0;
        for (List<V> layer : layers) {
            maxWidth = maxWidth < layer.size() ? layer.size() : maxWidth;
        }
        return maxWidth;
    }

    public void insertLayers(int position, int numberOfLayers) {
        if (position < 0 || position > layers.size()) {
            throw new IndexOutOfBoundsException("position must be inside of current Layers");
        }

        for (int i = layers.size() - 1; --i >= position;) {
            for (V vertex : layers.get(i)) {
                setLayer(vertex, getLayerFromVertex(vertex) + numberOfLayers);
            }
        }
    }

    @Override
    public int getLayerCount() {
        return layers.size();
    }

    //TODO Remove duplicate getHeight/getLayerCount
    @Override
    public int getHeight() {
        return layers.size();
    }

    @Override
    public int getVertexCount(int layerNum) {
        return layers.get(layerNum).size();
    }

    @Override
    public int getLayerFromVertex(Vertex vertex) {
        return getPosition(vertex).y;
    }

    public IntegerPoint getPosition(Vertex vertex) {
        if (!this.vertexToPoint.containsKey(vertex)) {
            throw new IllegalArgumentException("Vertex is not contained in layering!");
        }
        //return vertexToPoint.get(vertex).clone();
        return vertexToPoint.get(vertex).toImmutable();
    }
    
    public V getVertex(IntegerPoint point) {
        List<V> layer = layers.get(point.y);
        if (layer.size() <= point.x) {
            return null;
        }
        return layer.get(point.x);
    }
    
    public void setPosition(V vertex, IntegerPoint point) {
    	IntegerPoint oldPos = getPosition(vertex);
        assert (Objects.equals(vertex, getVertex(oldPos)));
        // Remove from old position
        ArrayList<V> vs = this.layers.get(oldPos.y);
        assert (oldPos.x <  vs.size());

        vs.remove(oldPos.x);
        for (int i = oldPos.x; i < vs.size(); i++) {
            //vertexToPoint.put(vs.get(i), new IntegerPoint(i, oldPos.y));
            vertexToPoint.get(vs.get(i)).setXandY(i, oldPos.y);
        }

        // Add enough layers to insert vertex
		for (int i = layers.size() - 1; i < point.y; i++) {
			this.layers.add(new ArrayList<>());
		}

		// Add to new position
		ArrayList<V> layer = this.layers.get(point.y);
        layer.add(point.x, vertex);
        vertexToPoint.put(vertex, new MutableIntegerPoint(point.x, point.y));

        for (int i = point.x + 1; i < layer.size(); i++) {
            //vertexToPoint.put(layer.get(i), new IntegerPoint(i, point.y));
            vertexToPoint.get(layer.get(i)).setXandY(i,point.y);
        }

        assert (Objects.equals(vertex, getVertex(getPosition(vertex))));
    }

    /**
     * Swaps the vertices on the specified layer number. Where the list newOrder describes the new indices of the vertices from this layer.
     * list [2,0,1] means the vertex that is current on position 2 will be on first position after this method, the vertex on position 0 will be on second position...
     *
     * @param layerNum number of the layer to swap its vertices
     * @param newOrder list of Integers describing the new order of the layers' vertices
     */
    public void swapVertices(int layerNum, List<Integer> newOrder){
        assert(layerNum < this.layers.size());
        List<V> layer = this.layers.get(layerNum);  //getting layer from layers, not getLayer, because of space overhead
        int layerSize = layer.size();
        int[] occurenceCheck = new int[layerSize];
        assert(Objects.equals(layerSize, newOrder.size()));
        ArrayList<V> newLayer = new ArrayList<>(layerSize);
        int idx = 0;
        for(Integer i : newOrder){
            assert(i >= 0 && i < layerSize);
            assert(occurenceCheck[i] == 0);
            occurenceCheck[i] = i;  //in order to assure no duplicate occurrences of any vertex position
            V vertex = layer.get(i);
            vertexToPoint.get(vertex).setX(idx);
            newLayer.add(vertex); //adding new vertex in specified order
            idx++;
        }
        this.layers.remove(layerNum);
        this.layers.add(layerNum, newLayer);
    }
    
    public void setLayer(V vertex, int layer) {
        if (getLayerFromVertex(vertex) == layer) {
            return;
        }
        List<V> assignedLayer;

        if (getHeight() <= layer) {
            assignedLayer = new LinkedList<>();
        } else {
            assignedLayer = layers.get(layer);
        }

        setPosition(vertex, new IntegerPoint(assignedLayer.size(), layer));
        assert (Objects.equals(vertex, getVertex(getPosition(vertex))));
    }
    
    public void addVertex(V vertex, int layer) {
        this.vertexToPoint.put(vertex, new MutableIntegerPoint(layers.get(0).size(), 0));
        this.layers.get(0).add(vertex);
        
        this.setLayer(vertex, layer);
    }

    public List<V> getSortedLayer(int layerIndex){
    	if (getHeight() <= layerIndex) {
            return new LinkedList<>();
        }
    	List<V> result = new LinkedList<>(layers.get(layerIndex));
    	result.sort(Comparator.comparingDouble(Vertex::getX));
        return result;
    }
    
    @Override
    public List<V> getLayer(int layerIndex) {
        if (getHeight() <= layerIndex) {
            return new LinkedList<>();
        }
        return new LinkedList<>(layers.get(layerIndex));
    }

    public List<List<V>> getSortedLayers(){
    	List<List<V>> copy = new LinkedList<>();
        for (List<V> layer : layers) {
            copy.add(new LinkedList<V>(layer));
        }
        copy.forEach(l->l.sort(Comparator.comparingDouble(Vertex::getX)));
        return copy;
    }
    
    @Override
    public List<List<V>> getLayers() {
        List<List<V>> copy = new LinkedList<>();
        for (List<V> layer : layers) {
            copy.add(new LinkedList<>(layer));
        }
        return copy;
    }

    public void cleanUpEmptyLayers() {
        while (true) {
            int size = layers.size();
            int start = size;
            int end = size;
            boolean lastEmpty = false;

            for (int i = 0; i < size; i++) {
                if (lastEmpty && layers.get(i).size() != 0) {
                    end = i;
                    break;
                }

                if (!lastEmpty && layers.get(i).size() == 0) {
                    start = i;
                    lastEmpty = true;
                }
            }

            int diff = end - start;
            if (diff == 0) {
                break;
            }

            for (int i = end; i < size; i++) {
                List<V> toMove = new ArrayList<>(layers.get(i));

                for (V vertex : toMove) {
                    setLayer(vertex, i - diff);
                }
            }

            for (int i = size - 1; i > size - diff - 1; i--) {
                layers.remove(i);
            }
        }
    }
}
