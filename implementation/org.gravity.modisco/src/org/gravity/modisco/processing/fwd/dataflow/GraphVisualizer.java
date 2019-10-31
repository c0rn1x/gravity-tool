package org.gravity.modisco.processing.fwd.dataflow;

import static guru.nidi.graphviz.model.Factory.mutGraph;
import static guru.nidi.graphviz.model.Factory.mutNode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.gmt.modisco.java.Assignment;
import org.eclipse.gmt.modisco.java.ClassInstanceCreation;
import org.eclipse.gmt.modisco.java.MethodInvocation;
import org.eclipse.gmt.modisco.java.NamedElement;
import org.eclipse.gmt.modisco.java.VariableDeclarationFragment;
import org.gravity.eclipse.GravityActivator;
import org.gravity.eclipse.util.EclipseProjectUtil;
import org.gravity.modisco.MAbstractMethodDefinition;
import org.gravity.modisco.MAnonymous;
import org.gravity.modisco.MGravityModel;

import guru.nidi.graphviz.attribute.Color;
import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.attribute.RankDir;
import guru.nidi.graphviz.attribute.Style;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;

/**
 * Functionality to draw dot graphs with intra-procedural data flow edges for
 * method/field definitions of Java programs. The drawn graphs are based on the
 * program representations created by the data flow handlers.
 *
 * @author dmebus
 *
 */
public class GraphVisualizer {

	private static final Logger LOGGER = Logger.getLogger(GraphVisualizer.class);

	private GraphVisualizer() {
		// This class shouldn't be instantiated
	}

	/**
	 * Draws the dot graphs for each StatementHandlerDataFlow instance in the given
	 * list. The graphs are exported as PNG image files to a folder with the given
	 * folder name (inside the project folder).
	 *
	 * @param model      The model of which graphs should be drawn
	 * @param handlers   The list of StatementHandlerDataFlow instances, for which a
	 *                   graph is supposed to be drawn.
	 *
	 * @param folderName The name, that should be given to the output folder. If a
	 *                   folder with that name already exists, it is used.
	 */
	public static void drawGraphs(MGravityModel model, List<StatementHandlerDataFlow> handlers, String folderName) {
		final File folder = getOutputLocation(model, folderName);
		for (int i = 0; i < handlers.size(); i++) {
			final StatementHandlerDataFlow handler = handlers.get(i);
			final EObject memberDef = handler.getMemberDef();
			String className = "Unknown";
			final String memberName = ((NamedElement) memberDef).getName();
			String memberType = "Misc";
			final EObject defContainer = memberDef.eContainer();
			if (memberDef instanceof MAbstractMethodDefinition) {
				memberType = "Method";
				if (defContainer instanceof MAnonymous) {
					className = "Anonymous-class";
				} else {
					className = ((NamedElement) defContainer).getName();
				}
			} else if (memberDef instanceof VariableDeclarationFragment) {
				memberType = "Field";
				className = ((NamedElement) defContainer.eContainer()).getName();
			}
			final MutableGraph g = mutGraph("graph" + i).setDirected(true).graphAttrs().add(RankDir.TOP_TO_BOTTOM)
					.graphAttrs().add("dpi", 72);
			final HashMap<FlowNode, MutableNode> graphNodes = new HashMap<>();
			// Creating a graph node for each FlowNode
			final Collection<FlowNode> alreadySeenNodes = new ArrayList<>(handler.getAlreadySeen().values());
			for (final FlowNode node : alreadySeenNodes) {
				final MutableNode graphNode = getDotNode(node);
				g.add(graphNode);
				graphNodes.put(node, graphNode);
			}
			// Set containment edges (+ links to nodes of called/accessed methods/fields)
			// for all graph nodes
			drawContainmentEdges(handler, graphNodes, alreadySeenNodes);
			// Set flow edges
			drawFlowEdges(graphNodes, alreadySeenNodes);
			try {

				Graphviz.fromGraph(g).width(5000).render(Format.PNG)
				.toFile(new File(folder, "Class-" + className + "-" + memberType + "-" + memberName + ".png"));
			} catch (final IOException e) {
				LOGGER.log(Level.ERROR, e.getMessage(), e);
			}
		}
	}

	/**
	 * Creates an output folder with the given name for the model
	 *
	 * @param model The model
	 * @param folderName The name of the output folder
	 * @return The ouput folder
	 */
	private static File getOutputLocation(MGravityModel model, String folderName) {
		final String projectName = model.getName();
		final IProject project = GravityActivator.getDefault().getProject(projectName);
		File folder;
		if (project != null) {
			try {
				final IFolder ifolder = EclipseProjectUtil.getGravityFolder(project, new NullProgressMonitor());
				folder = ifolder.getFolder(folderName).getLocation().toFile();
			} catch (final IOException e) {
				LOGGER.error(e.getMessage(), e);
				folder = new File(folderName);
			}
		} else {
			folder = new File(folderName);
		}
		if(!folder.exists()) {
			folder.mkdirs();
		}
		return folder;
	}

	/**
	 * Draws the flow edges between the graph nodes
	 *
	 * @param graphNodes       The nodes of the graph
	 * @param alreadySeenNodes
	 */
	private static void drawFlowEdges(HashMap<FlowNode, MutableNode> graphNodes,
			Collection<FlowNode> alreadySeenNodes) {
		for (final FlowNode node : alreadySeenNodes) {
			final MutableNode graphNode = graphNodes.get(node);
			for (final FlowNode out : node.getOutRef()) {
				final MutableNode outNode = graphNodes.get(out);
				if (outNode != null) {
					graphNode.addLink(graphNode.linkTo(outNode).with(Style.DASHED, Label.of("flows to"), Color.BLUE));
				}
			}
		}
	}

	/**
	 * Set containment edges (+ links to nodes of called/accessed methods/fields)
	 * for all graph nodes
	 *
	 * @param handler
	 * @param graphNodes       The nodes of the graph
	 * @param alreadySeenNodes
	 */
	private static void drawContainmentEdges(StatementHandlerDataFlow handler,
			HashMap<FlowNode, MutableNode> graphNodes, Collection<FlowNode> alreadySeenNodes) {
		for (final FlowNode node : alreadySeenNodes) {
			final MutableNode graphNode = graphNodes.get(node);
			final EObject modelElement = node.getModelElement();
			if (modelElement == null) {
				continue;
			}
			final EObject eContainer = modelElement.eContainer();
			final FlowNode flowCont = handler.getAlreadySeen().get(eContainer);
			if (flowCont == null) {
				continue;
			}
			if (modelElement instanceof MethodInvocation) {
				graphNode.addLink(graphNode
						.linkTo(getDotNode(handler.getFlowNodeForElement(((MethodInvocation) modelElement).getMethod()))
								.add(Style.FILLED, Color.GRAY))
						.with(Style.DOTTED, Label.of("calls"), Color.GRAY));
			}
			if (modelElement instanceof ClassInstanceCreation) {
				graphNode.addLink(graphNode.linkTo(
						getDotNode(handler.getFlowNodeForElement(((ClassInstanceCreation) modelElement).getMethod()))
						.add(Style.FILLED, Color.GRAY))
						.with(Style.DOTTED, Label.of("calls"), Color.GRAY));
			}
			graphNodes.get(flowCont).addLink(graphNode);
		}
	}

	/**
	 * Returns a (new) dot graph node for the given FlowNode. If the node already
	 * exists, the existing one is returned (by the mutNode constructor), so
	 * duplicates are prevented.
	 *
	 * @param node The FlowNode, for which a dot graph node is requested.
	 * @return The requested dot graph node, which corresponds to the given
	 *         FlowNode.
	 */
	private static MutableNode getDotNode(FlowNode node) {
		final EObject modelElement = node.getModelElement();
		MutableNode graphNode = null;
		if (modelElement != null) {
			String label = modelElement.eClass().getName();
			if (modelElement instanceof NamedElement) {
				final String name = ((NamedElement) modelElement).getName();
				if (name != null) {
					label += " " + name;
				}
			}
			if (modelElement instanceof Assignment) {
				label += " " + ((Assignment) modelElement).getOperator().toString();
			}
			graphNode = mutNode(Integer.toString(modelElement.hashCode())).add(Label.of(label));
		} else {
			graphNode = mutNode(Integer.toString(node.hashCode())).add(Label.of("null"));
		}
		return graphNode;
	}
}
