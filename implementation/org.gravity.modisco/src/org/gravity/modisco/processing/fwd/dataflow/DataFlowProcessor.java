package org.gravity.modisco.processing.fwd.dataflow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.gmt.modisco.java.AbstractMethodDeclaration;
import org.eclipse.gmt.modisco.java.AbstractMethodInvocation;
import org.eclipse.gmt.modisco.java.DoStatement;
import org.eclipse.gmt.modisco.java.EnhancedForStatement;
import org.eclipse.gmt.modisco.java.ForStatement;
import org.eclipse.gmt.modisco.java.IfStatement;
import org.eclipse.gmt.modisco.java.MethodDeclaration;
import org.eclipse.gmt.modisco.java.ReturnStatement;
import org.eclipse.gmt.modisco.java.SingleVariableDeclaration;
import org.eclipse.gmt.modisco.java.SwitchStatement;
import org.eclipse.gmt.modisco.java.TypeAccess;
import org.eclipse.gmt.modisco.java.UnresolvedMethodDeclaration;
import org.eclipse.gmt.modisco.java.VariableDeclaration;
import org.eclipse.gmt.modisco.java.VariableDeclarationFragment;
import org.eclipse.gmt.modisco.java.VariableDeclarationStatement;
import org.eclipse.gmt.modisco.java.WhileStatement;
import org.gravity.modisco.MFlow;
import org.gravity.eclipse.GravityActivator;
import org.gravity.modisco.MAbstractFlowElement;
import org.gravity.modisco.MAbstractMethodDefinition;
import org.gravity.modisco.MAbstractMethodInvocation;
import org.gravity.modisco.MConstructorDefinition;
import org.gravity.modisco.MDefinition;
import org.gravity.modisco.MEntry;
import org.gravity.modisco.MFieldDefinition;
import org.gravity.modisco.MGravityModel;
import org.gravity.modisco.MMethodDefinition;
import org.gravity.modisco.MSingleVariableAccess;
import org.gravity.modisco.MSingleVariableDeclaration;
import org.gravity.modisco.ModiscoFactory;
import org.gravity.modisco.processing.AbstractTypedModiscoProcessor;

/**
 * A preprocessor for calculating data flow edges<br/><br/>
 * 
 * <b>IMPORTANT REMARK:</b><br/>
 * To work properly, this processor requires FieldPreprocessing and MethodPreprocessing to be executed prior to it, 
 * as it depends on the decomposition of statements with multiple declarations into one statement for each declaration.
 * 
 * @author speldszus
 *
 */
public class DataFlowProcessor extends AbstractTypedModiscoProcessor<MDefinition> {
	
	private static final Logger LOGGER = Logger.getLogger(DataFlowProcessor.class.getName());

	@Override
	public boolean process(MGravityModel model, Collection<MDefinition> elements, IProgressMonitor monitor) {
		SubMonitor sub = SubMonitor.convert(monitor, "Create model elements for data flow", elements.size());
		boolean success = true;
		sub.beginTask("Statement pre-processing", 50);
		List<StatementHandlerDataFlow> handlers = preProcessStatements(model);
		if (handlers == null) {
			success = false;
		}
		sub.internalWorked(50);
		sub.beginTask("Insertion of data flow edges", 5);
		
		// Per handler: Reduction of intra-DFGs and then insertion of inter-procedural data flows 
		// TODO: Extract into own method (+ submethods)
		// TODO: Work with alreadySeen-Sets instead of handlers?
		for (StatementHandlerDataFlow handler : handlers) {
			
			// Determination of member's type
			EObject memberDef = handler.getMemberDef();
			MDefinition memberDefTyped = null;
			if (memberDef instanceof MAbstractMethodDefinition) {
				memberDefTyped = (MDefinition) memberDef;
			} else if (memberDef instanceof VariableDeclarationFragment) {
				memberDefTyped = (MDefinition) memberDef.eContainer();
			} else {
				LOGGER.log(Level.INFO, "ERROR: Handler with unknown member type " + memberDef.getClass().getName() + " in DataFlowProcessor");
				return false;
			}
			
			// Reduction of intra-DFGs
			HashMap<EObject, FlowNode> reducedDFG = handler.getAlreadySeen();
			for (EObject node : new ArrayList<>(reducedDFG.keySet())) {
				if (node instanceof MAbstractMethodDefinition) {
					// Keep node (for readability of dot graphs only)
				} else if (node instanceof VariableDeclarationFragment && node.eContainer() instanceof MFieldDefinition) {
					// Keep node
				} else if (node instanceof ReturnStatement) {
					// Keep node
				} else if (node instanceof MSingleVariableAccess) {
					// Keep variable access node only if its a field access
					VariableDeclaration variable = ((MSingleVariableAccess) node).getVariable();
					if(variable == null) {
						LOGGER.error("Declared variable is null");
					}
					else if (variable.eContainer() instanceof VariableDeclarationStatement) {
						reduceNodeInDFG(node, reducedDFG);
						continue;
					}
				} else if (node instanceof AbstractMethodInvocation) {
					// Keep node
				} else if (node instanceof SingleVariableDeclaration) {
					// Keep node
				} else if (node instanceof IfStatement 
						|| node instanceof WhileStatement
						|| node instanceof ForStatement
						|| node instanceof EnhancedForStatement
						|| node instanceof DoStatement
						|| node instanceof SwitchStatement) { 
					// Keep nodes, as a flow into them indicates, that sensitive info can leak implicitly through observation of the construct's behavior
				} else { // Everything else is reduced in the same way
					reduceNodeInDFG(node, reducedDFG);
				}
			}

			// Insertion of inter-procedural data flows
			for (FlowNode node : handler.getMemberRef()) {
				// Removing unnecessary out edges (self-flows and flows to calls in paramFlows)
				Set<FlowNode> outRef = node.getOutRef();
				int size = outRef.size();
				if (size > 1) {
					Set<FlowNode> toRemove = new HashSet<>();
					for (FlowNode flowNode : outRef) {
						EObject modelElement = flowNode.getModelElement();
						if (modelElement == node) { // Remove self-flow
							toRemove.add(flowNode);
							reducedDFG.get(modelElement).getInRef().remove(node);
						} else if (modelElement instanceof MAbstractMethodInvocation) { // Remove flow into call, if there's actually a paramFlow
							node.setFlowOwner((MAbstractMethodInvocation) modelElement);
							toRemove.add(flowNode);
							reducedDFG.get(modelElement).getInRef().remove(node);
						}
					}
					outRef.removeAll(toRemove);
				}
				
				// A constructor invocation or method invocation with a return value should explicitly have the MethodDef/MethodSig set as incoming flow
				MAbstractFlowElement access = (MAbstractFlowElement) node.getModelElement();
				Set<FlowNode> inRef = node.getInRef();
				if (access instanceof AbstractMethodInvocation) {
					AbstractMethodDeclaration methodDef = ((AbstractMethodInvocation) access).getMethod();
					if (methodDef instanceof MConstructorDefinition) {
						FlowNode defNode = handler.getFlowNodeForElement(methodDef);
						inRef.add(defNode);
						defNode.addOutRef(node);
					} else {
						TypeAccess returnType = null;
						if (methodDef != null) {
							returnType = ((MethodDeclaration) methodDef).getReturnType();
							if (returnType != null) {
								if (!returnType.getType().getName().equals("void")) {
									FlowNode sigNode = handler.getFlowNodeForElement(((MMethodDefinition) methodDef).getMMethodSignature());
									inRef.add(sigNode);
									sigNode.addOutRef(node);
								}
							} else {
								if (!(methodDef instanceof UnresolvedMethodDeclaration)) { // Ignoring UnresolvedMethodDeclarations for now
									FlowNode sigNode = handler.getFlowNodeForElement(((MMethodDefinition) methodDef).getMMethodSignature());
									inRef.add(sigNode);
									sigNode.addOutRef(node);
								}
							}
						}
					}
				}
				
				// Setting flows
				MFlow accessOut = null;
				for (FlowNode inNode : inRef) {
					EObject inElement = inNode.getModelElement();
					if (inElement instanceof MSingleVariableDeclaration) {
						accessOut = ModiscoFactory.eINSTANCE.createMFlow();
						accessOut.setFlowSource(((MSingleVariableDeclaration) inElement).getMEntry());
						if (outRef.isEmpty()) { // Handling parameter flows, which end in an access (e. g. if access is in an assignment to a local)
							accessOut.setFlowOwner(access);
							accessOut.setFlowTarget(access);
						} else { // Set flowOwner to parameter's member, as the access will be removed in the TGG transformation
							accessOut.setFlowOwner((MAbstractMethodDefinition) ((MSingleVariableDeclaration) inElement).getMethodDeclaration());
						}
					} else {
						// Also create incoming flow here, if it's not coming from an access (to avoid redundancy)
						if (!(inElement instanceof MSingleVariableAccess || inElement instanceof AbstractMethodInvocation)) {
							MFlow accessIn = ModiscoFactory.eINSTANCE.createMFlow();
							if (inElement instanceof VariableDeclarationFragment) {
								MFieldDefinition fieldDef = (MFieldDefinition) ((VariableDeclarationFragment) inElement).getVariablesContainer();
								accessIn.setFlowSource(fieldDef.getMFieldSignature());
							} else {
								accessIn.setFlowSource((MAbstractFlowElement) inElement);
							}
							accessIn.setFlowTarget(access);
							accessIn.setFlowOwner(access);
						}
					}
				}
				for (FlowNode outNode : outRef) {
					if (accessOut == null || !(accessOut.getFlowSource() instanceof MEntry)) {
						accessOut = ModiscoFactory.eINSTANCE.createMFlow();
						accessOut.setFlowSource(access);
						accessOut.setFlowOwner(access);
					}
					EObject outElement = outNode.getModelElement();
					if (outElement instanceof ReturnStatement) {
						accessOut.setFlowTarget(memberDefTyped);
					} else if (outElement instanceof VariableDeclarationFragment) {
						MFieldDefinition fieldDef = (MFieldDefinition) ((VariableDeclarationFragment) outElement).getVariablesContainer();
						accessOut.setFlowTarget(fieldDef);
					} else if (outElement instanceof MSingleVariableDeclaration) {
						completeFlowForSigParam(outElement, node, accessOut);
					} else if (outElement instanceof IfStatement
							|| outElement instanceof WhileStatement
							|| outElement instanceof ForStatement
							|| outElement instanceof EnhancedForStatement
							|| outElement instanceof DoStatement
							|| outElement instanceof SwitchStatement) {
						accessOut.setFlowTarget(memberDefTyped);
					} else {
						if (outElement instanceof MSingleVariableAccess) { // Omitting accesses of parameters, when the target is another access
							MSingleVariableAccess mSVA = (MSingleVariableAccess) outElement;
							VariableDeclaration variable = mSVA.getVariable();
							if (variable instanceof MSingleVariableDeclaration) {
								accessOut.setFlowTarget(((MSingleVariableDeclaration) variable).getMEntry());
							} else if (variable.eContainer() instanceof MFieldDefinition
									&& accessOut.getFlowSource() instanceof MEntry) {
								accessOut.setFlowOwner(mSVA);
								accessOut.setFlowTarget(mSVA);
							} else { // Basically flows into field accesses without MEntry as source
								MAbstractFlowElement outTarget = (MAbstractFlowElement) outElement;
								accessOut.setFlowTarget(outTarget);
							}
						} else {
							MAbstractFlowElement outTarget = (MAbstractFlowElement) outElement;
							accessOut.setFlowTarget(outTarget);
						}
					}
				}
			}
		}
		if (GravityActivator.getDefault().isVerbose()) {
			GraphVisualizer.drawGraphs(handlers, "reducedGraphs");
		}
		sub.internalWorked(5);
		return success;
	}

	/**
	 * Removes the given node (including its flows) from reducedDFG and inserts direct flows from his inNodes to his outNodes. 
	 * 
	 * @param node The node's key in reducedDFG.
	 * @param reducedDFG The alreadySeen on which the reduction should be performed.
	 */
	private void reduceNodeInDFG(EObject node, HashMap<EObject, FlowNode> reducedDFG) {
		FlowNode flowNode = reducedDFG.get(node);
		Set<FlowNode> inRef = flowNode.getInRef();
		Set<FlowNode> outRef = flowNode.getOutRef();
		// TODO Review this loop
		for (FlowNode outNode : outRef) {
			for (FlowNode inNode : inRef) {
				if (inNode != flowNode) {
					if (outNode != inNode) {
						outNode.addInRef(inNode);
					}
					inNode.getOutRef().remove(flowNode);
				}
			}
			outNode.getInRef().remove(flowNode);
		}
		// Making sure, that all incoming flows into terminal node are removed
		if (outRef.isEmpty()) {
			for (FlowNode inNode : inRef) {
				inNode.getOutRef().remove(flowNode);
			}
		}
		inRef.clear();
		outRef.clear();
		reducedDFG.remove(node);
	}

	/**
	 * Completes the given flow node's entries (target and owner) 
	 * by obtaining the signature parameter corresponding to defParamObj (and setting it as target) 
	 * and setting the flowOwner to the MMethodInvocation in which currentAccess is contained.
	 * 
	 * @param defParamObj The model object of the called method's definition.
	 * @param currentAccess The FlowNode of the currently processed access.
	 * @param flow The flow, which is supposed to be completed.
	 */
	private void completeFlowForSigParam(EObject defParamObj, FlowNode currentAccess, MFlow flow) {
		// Set target
		MSingleVariableDeclaration paramTarget = (MSingleVariableDeclaration) defParamObj;
		MEntry sigParamTarget = paramTarget.getMEntry();
		flow.setFlowTarget(sigParamTarget);
		
		// Set owner
		MAbstractMethodInvocation invocation = currentAccess.getFlowOwner();
		if (invocation != null) {
			flow.setFlowOwner(invocation);
		} else {
			LOGGER.log(Level.INFO, "AbstractMethodInvocation for argument access wasn't found. FlowOwner is set to default (flow target).");
			flow.setFlowOwner((MAbstractMethodDefinition) paramTarget.getMethodDeclaration());
		}
	}

	@Override
	public Class<MDefinition> getSupportedType() {
		return MDefinition.class;
	}
	
	/**
	 * Pre-processes the statements of all field and method definitions to determine the data flows between statement nodes.
	 * The resulting intermediate representation of data flow is then used to derive the data flow of each field/method.
	 * 
	 * @param model The model, whose statements are processed.
	 * @return A list of the statement handlers resulting from the pre-processing.
	 */
	private List<StatementHandlerDataFlow> preProcessStatements(MGravityModel model) {
		if (model == null) {
			return null;
		}
		List<StatementHandlerDataFlow> handlers = new ArrayList<>();
		for (MAbstractMethodDefinition methodDef : model.getMAbstractMethodDefinitions()) {
			StatementHandlerDataFlow methodProcessor = new StatementHandlerDataFlow(methodDef);
			methodProcessor.getFlowNodeForElement(methodDef);
			for (SingleVariableDeclaration param : methodDef.getParameters()) {
				methodProcessor.getFlowNodeForElement(param);
			}
			methodProcessor.handle(methodDef.getBody());
			handlers.add(methodProcessor);
		}
		for (MFieldDefinition fieldDef : model.getMFieldDefinitions()) {
			for (VariableDeclarationFragment fragment : fieldDef.getFragments()) {
				StatementHandlerDataFlow fieldProcessor = new StatementHandlerDataFlow(fragment);
				fieldProcessor.getMiscHandler().handle(fragment);
				fieldProcessor.getFlowNodeForElement(fieldDef);
				handlers.add(fieldProcessor);
			}
		}
		if(GravityActivator.getDefault().isVerbose()) {
			GraphVisualizer.drawGraphs(handlers, "graphs");
		}
		return handlers;
	}
}