package org.gravity.modisco.processing.fwd.dataflow;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.emf.ecore.EObject;
import org.gravity.modisco.MAbstractMethodInvocation;

/**
 * An intermediate representation of data flow elements. It captures the flow
 * information of a single statement node.
 *
 * @author dmebus
 *
 */
public class FlowNode {

	/**
	 * Collection of all statements from which a data flow goes <b>into</b> this
	 * statement.
	 */
	private final Set<FlowNode> inRef = new HashSet<>();

	/**
	 * Collection of all statements to which data flows <b>from</b> this statement.
	 */
	private final Set<FlowNode> outRef = new HashSet<>();

	/**
	 * The model element, which corresponds to this statement node.
	 */
	private final EObject modelElement;

	/**
	 * A flag to indicate, whether this FlowNode has already been processed and thus
	 * was drawn from the alreadySeen HashMap at least once.
	 */
	private boolean alreadySeen;

	/**
	 * A field to store a reference to the MMethodInvocation, which is a the
	 * flowOwner of the parameter flow.
	 */
	private MAbstractMethodInvocation flowOwner;

	public FlowNode(EObject correspondingElement) {
		this.modelElement = correspondingElement;
	}

	public Set<FlowNode> getInRef() {
		return this.inRef;
	}

	public Set<FlowNode> getOutRef() {
		return this.outRef;
	}

	public void addInRef(FlowNode node) {
		this.inRef.add(node);
		node.getOutRef().add(this);
	}

	public void addOutRef(FlowNode node) {
		this.outRef.add(node);
		node.getInRef().add(this);
	}

	public EObject getModelElement() {
		return this.modelElement;
	}

	public boolean alreadySeen() {
		return this.alreadySeen;
	}

	public void setToAlreadySeen() {
		this.alreadySeen = true;
	}

	public MAbstractMethodInvocation getFlowOwner() {
		return this.flowOwner;
	}

	public void setFlowOwner(MAbstractMethodInvocation modelElement) {
		this.flowOwner = modelElement;
	}
}
