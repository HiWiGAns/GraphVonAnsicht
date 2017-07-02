package edu.kit.student.joana;

import edu.kit.student.graphmodel.DefaultVertex;
import edu.kit.student.graphmodel.VertexReference;
import edu.kit.student.graphmodel.ViewableVertex;
import edu.kit.student.objectproperty.GAnsProperty;
import edu.kit.student.util.DoublePoint;
import javafx.scene.paint.Color;

import java.util.List;

/**
 * A Joana specific Vertex. It contains parameters which are only used/useful
 * for Joana.
 */
public class JoanaVertex extends DefaultVertex implements ViewableVertex {

    private GAnsProperty<VertexKind> nodeKind;
    private GAnsProperty<JavaSource> nodeSource;
    private GAnsProperty<Integer> nodeProc;
    private GAnsProperty<Operation> nodeOperation;
    private GAnsProperty<String> nodeBcName;
    private GAnsProperty<Integer> nodeBcIndex;
    private GAnsProperty<Integer> nodeSr;
    private GAnsProperty<Integer> nodeSc;
    private GAnsProperty<Integer> nodeEr;
    private GAnsProperty<Integer> nodeEc;
    private GAnsProperty<String>  nodeLocalDef;
    private GAnsProperty<String>  nodeLocalUse;

    private DoublePoint setSize;
    private boolean sizeSet;
    private String nodeLabel;
    
    /**
     * Constructs a new JoanaVertex, giving it a nameId,
     * label and setting what kind of joana vertex it is.
     * @param name  the nameId
     * @param label the label
     * @param kind  the kind (type)
     */
    public JoanaVertex(String name, String label, VertexKind kind) {
        this(name, label, kind,null, 0, Operation.EMPTY, "", 0, 0, 0, 0, 0, "", "");
    }

    public JoanaVertex(String name, String label, VertexKind nodeKind, JavaSource source,
            int nodeProc, Operation nodeOperation, String nodeBcName, int nodeBcIndex, int nodeSr,
            int nodeSc, int nodeEr, int nodeEc,
            String nodeLocalDef, String nodeLocalUse) {

        super(name, label);

        this.nodeKind = new GAnsProperty<>("nodeKind", nodeKind);
        this.nodeSource = new GAnsProperty<>("nodeSource", source);
        this.nodeProc = new GAnsProperty<>("nodeProc", nodeProc);
        this.nodeOperation = new GAnsProperty<>("nodeOperation", nodeOperation);
        this.nodeBcName = new GAnsProperty<>("nodeBcName", nodeBcName);
        this.nodeBcIndex = new GAnsProperty<>("nodeBCIndex", nodeBcIndex);
        this.nodeSr = new GAnsProperty<>("nodeSr", nodeSr);
        this.nodeSc = new GAnsProperty<>("nodeSc", nodeSc);
        this.nodeEr = new GAnsProperty<>("nodeEr", nodeEr);
        this.nodeEc = new GAnsProperty<>("nodeEc", nodeEc);
        this.nodeLocalDef = new GAnsProperty<>("nodeLocalDef", nodeLocalDef);
        this.nodeLocalUse = new GAnsProperty<>("nodeLocalUse", nodeLocalUse);
        this.sizeSet = false;
        this.nodeLabel = label;
    }


    @Override
	public List<GAnsProperty<?>> getProperties() {
    	List<GAnsProperty<?>> properties = super.getProperties();
		properties.add(nodeKind);
		properties.add(nodeSource);
		properties.add(nodeProc);
		properties.add(nodeOperation);
		properties.add(nodeBcName);
		properties.add(nodeBcIndex);
		properties.add(nodeSr);
		properties.add(nodeSc);
		properties.add(nodeEr);
		properties.add(nodeEc);
		if (nodeLocalDef.getValue() != null)
            properties.add(nodeLocalDef);
        if (nodeLocalUse.getValue() != null)
            properties.add(nodeLocalUse);
		return properties;
	}

    /**
     * Returns the nodeKind of the JoanaVertex.
     * 
     * @return The nodeKind of the JoanaVertex.
     */
    public VertexKind getNodeKind() {
        return nodeKind.getValue();
    }

    /**
     * Returns the nodeSource of the JoanaVertex.
     * 
     * @return The nodeSource of the JoanaVertex.
     */
    public JavaSource getNodeSource() {
        return nodeSource.getValue();
    }

    /**
     * Returns the nodeProc of the JoanaVertex.
     * 
     * @return The nodeProc of the JoanaVertex.
     */
    public Integer getNodeProc() {
        return nodeProc.getValue();
    }

    /**
     * Returns the nodeOperation of the JoanaVertex.
     * 
     * @return The nodeOperation of the JoanaVertex.
     */
    public Operation getNodeOperation() {
        return nodeOperation.getValue();
    }

    /**
     * Returns the nodeBcName of the JoanaVertex.
     * 
     * @return The nodeBcName of the JoanaVertex.
     */
    public String getNodeBcName() {
        return nodeBcName.getValue();
    }

    /**
     * Returns the nodeBCIndex of the JoanaVertex.
     * 
     * @return The nodeBCIndex of the JoanaVertex.
     */
    public Integer getNodeBcIndex() {
        return nodeBcIndex.getValue();
    }

    /**
     * Returns the nodeSr of the JoanaVertex.
     * 
     * @return The nodeSr of the JoanaVertex.
     */
    public Integer getNodeSr() {
        return nodeSr.getValue();
    }

    /**
     * Returns the nodeSc of the JoanaVertex.
     * 
     * @return The nodeSc of the JoanaVertex.
     */
    public Integer getNodeSc() {
        return nodeSc.getValue();
    }

    /**
     * Returns the nodeEr of the JoanaVertex.
     * 
     * @return The nodeEr of the JoanaVertex.
     */
    public Integer getNodeEr() {
        return nodeEr.getValue();
    }

    /**
     * Returns the nodeEc of the JoanaVertex.
     * 
     * @return The nodeEc of the JoanaVertex.
     */
    public Integer getNodeEc() {
        return nodeEc.getValue();
    }

    /**
     * Returns the nodeLocalDef of the JoanaVertex.
     *
     * @return The nodeLocalDef of the JoanaVertex.
     */
    public String getNodelLocalDef() {
        return nodeLocalDef.getValue();
    }

    /**
     * Returns the nodeLocalUse of the JoanaVertex.
     *
     * @return The nodeLocalUse of the JoanaVertex.
     */
    public String getNodeLocalUse() {
        return nodeLocalUse.getValue();
    }


    @Override
	public DoublePoint getSize() {
		// TODO: calculating size with different max/min values depending on KIND
		if(this.sizeSet){
			return this.setSize;
		} else{
			return super.getSize();
		}
	}
	
	@Override
	public String getLabel(){
		return this.getName() + "  " + this.getNodeKind().toString() + '\n' + this.nodeLabel;
	}

	@Override
	public Color getColor() {
		return nodeKind.getValue().color();
	}
    
    public enum VertexKind {
        NORM(Color.web("0xFFC125")), CALL(Color.web("0xBFEFFF")), EXIT(Color.web("0xFFC125")),
        ENTR(Color.web("0xCBCBCB")), ACTI(Color.web("0x87CEFA")), ACTO(Color.web("0x00BFFF")),
        FRMO(Color.web("0x00EE00")), FRMI(Color.web("0x98FB98")), EXPR(Color.web("0xFFFF00")),
        PRED(Color.web("0xFFC125")), SYNC(Color.web("0xFFC125")), FOLD(Color.BROWN),
        SUMMARY(Color.BROWN), FIELDACCESS(Color.LIGHTGREEN);

        public final Color backgroundColor;

        VertexKind(Color backgroundColor) {
            this.backgroundColor = backgroundColor;
        }

    	@Override
    	public String toString() {
    		return this.name();
    	}
        
        public Color color() {
            return this.backgroundColor;
        }
        
        public VertexPriority priority() {
        	switch(this) {
        	case FIELDACCESS: return VertexPriority.LOW;
        	default: return VertexPriority.HIGH;
        	}
        }
    }

    @Override
    public VertexReference getLink() {
        return null;
    }

    public void setSize(DoublePoint newSize){
        this.setSize = newSize;
        this.sizeSet = true;
    }

    @Override
    public VertexPriority getPriority() {
        return this.nodeKind.getValue().priority();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JoanaVertex that = (JoanaVertex) o;

        if (sizeSet != that.sizeSet) return false;
        if (nodeKind != null ? !nodeKind.equals(that.nodeKind) : that.nodeKind != null) return false;
        if (nodeSource != null ? !nodeSource.equals(that.nodeSource) : that.nodeSource != null) return false;
        if (nodeProc != null ? !nodeProc.equals(that.nodeProc) : that.nodeProc != null) return false;
        if (nodeOperation != null ? !nodeOperation.equals(that.nodeOperation) : that.nodeOperation != null)
            return false;
        if (nodeBcName != null ? !nodeBcName.equals(that.nodeBcName) : that.nodeBcName != null) return false;
        if (nodeBcIndex != null ? !nodeBcIndex.equals(that.nodeBcIndex) : that.nodeBcIndex != null) return false;
        if (nodeSr != null ? !nodeSr.equals(that.nodeSr) : that.nodeSr != null) return false;
        if (nodeSc != null ? !nodeSc.equals(that.nodeSc) : that.nodeSc != null) return false;
        if (nodeEr != null ? !nodeEr.equals(that.nodeEr) : that.nodeEr != null) return false;
        if (nodeEc != null ? !nodeEc.equals(that.nodeEc) : that.nodeEc != null) return false;
        if (nodeLocalDef != null ? !nodeLocalDef.equals(that.nodeLocalDef) : that.nodeLocalDef != null) return false;
        if (nodeLocalUse != null ? !nodeLocalUse.equals(that.nodeLocalUse) : that.nodeLocalUse != null) return false;
        if (setSize != null ? !setSize.equals(that.setSize) : that.setSize != null) return false;
        return nodeLabel != null ? nodeLabel.equals(that.nodeLabel) : that.nodeLabel == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (nodeKind != null ? nodeKind.hashCode() : 0);
        result = 31 * result + (nodeSource != null ? nodeSource.hashCode() : 0);
        result = 31 * result + (nodeProc != null ? nodeProc.hashCode() : 0);
        result = 31 * result + (nodeOperation != null ? nodeOperation.hashCode() : 0);
        result = 31 * result + (nodeBcName != null ? nodeBcName.hashCode() : 0);
        result = 31 * result + (nodeBcIndex != null ? nodeBcIndex.hashCode() : 0);
        result = 31 * result + (nodeSr != null ? nodeSr.hashCode() : 0);
        result = 31 * result + (nodeSc != null ? nodeSc.hashCode() : 0);
        result = 31 * result + (nodeEr != null ? nodeEr.hashCode() : 0);
        result = 31 * result + (nodeEc != null ? nodeEc.hashCode() : 0);
        result = 31 * result + (nodeLocalDef != null ? nodeLocalDef.hashCode() : 0);
        result = 31 * result + (nodeLocalUse != null ? nodeLocalUse.hashCode() : 0);
        result = 31 * result + (setSize != null ? setSize.hashCode() : 0);
        result = 31 * result + (sizeSet ? 1 : 0);
        result = 31 * result + (nodeLabel != null ? nodeLabel.hashCode() : 0);
        return result;
    }



    /**
     * The following section was taken from the Joana IFC project in compliance
     * with the authors of the project.
     * The following disclaimer was copied from the file, the section has been extracted from:
     *
     * This file is part of the Joana IFC project. It is developed at the
     * Programming Paradigms Group of the Karlsruhe Institute of Technology.
     *
     * For further details on licensing please read the information at
     * http://joana.ipd.kit.edu or contact the authors.
     */
    public enum Operation {

        EMPTY("empty", VertexKind.EXPR, VertexKind.FOLD), //
        INT_CONST("intconst", VertexKind.EXPR, VertexKind.PRED, VertexKind.ACTI),
        FLOAT_CONST("floatconst", VertexKind.EXPR),
        CHAR_CONST("charconst", VertexKind.EXPR),
        STRING_CONST("stringconst", VertexKind.EXPR),
        FUNCTION_CONST("functionconst", VertexKind.EXPR),
        SHORTCUT("shortcut", VertexKind.EXPR, VertexKind.PRED, VertexKind.ACTI), // && ||
        QUESTION("question", VertexKind.EXPR), // ? :
        BINARY("binary", VertexKind.EXPR, VertexKind.PRED),
        UNARY("unary", VertexKind.EXPR),
        //			//	     | "lookup"
        DEREFER("derefer", VertexKind.EXPR, VertexKind.PRED, VertexKind.ACTI),
        REFER("refer", VertexKind.EXPR, VertexKind.PRED, VertexKind.ACTI),
        ARRAY("array", VertexKind.EXPR),
        SELECT("select", VertexKind.EXPR),
        REFERENCE("reference", VertexKind.EXPR, VertexKind.PRED, VertexKind.ACTI),
        DECLARATION("declaration", VertexKind.NORM),
        MODIFY("modify", VertexKind.EXPR, VertexKind.PRED), // ++ --
        MODASSIGN("modassign", VertexKind.EXPR), // += etc.
        ASSIGN("assign", VertexKind.EXPR),
        IF("IF", VertexKind.PRED, VertexKind.NORM),
        LOOP("loop", VertexKind.NORM),
        JUMP("jump", VertexKind.NORM),
        COMPOUND("compound", VertexKind.NORM),
        CALL("call", VertexKind.CALL),
        ENTRY("entry", VertexKind.ENTR),
        EXIT("exit", VertexKind.EXIT),
        FORMAL_IN("form-in", VertexKind.FRMI),
        FORMAL_ELLIP("form-ellip", VertexKind.FRMI), // Ellipse-Parameter "..."
        FORMAL_OUT("form-out", VertexKind.FRMO),
        ACTI("act-in", VertexKind.ACTI),
        ACTUAL_OUT("act-out", VertexKind.ACTO),
        //		NONE("None"
        MONITOR("monitor", VertexKind.SYNC),
        SUMMARY("summary", VertexKind.SUMMARY);

        private final String value;
        private final VertexKind[] kind;

        Operation(String s, VertexKind k) { value = s; kind = new VertexKind[]{k}; }

        Operation(String s, VertexKind k1, VertexKind k2) {
            value = s;
            kind = new VertexKind[]{k1, k2};
        }

        Operation(String s, VertexKind k1, VertexKind k2, VertexKind k3) {
            value = s;
            kind = new VertexKind[]{k1, k2, k3};
        }

        public VertexKind getKind(int i) {
            return kind[i];
        }

        public String toString() {
            return value;
        }

        VertexKind[] getCorrespondingKind() {
            return kind;
        }

        public static Operation getOperationByValue(String value) {
            for (Operation op : values()) {
                if (op.value.equals(value)) {
                    return op;
                }
            }
            return null;
        }
    }
}
