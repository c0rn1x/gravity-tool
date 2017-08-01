package org.gravity.hulk.detection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.eclipse.emf.ecore.EClass;
import org.gravity.hulk.HAntiPatternHandling;
import org.gravity.hulk.HDetector;
import org.gravity.hulk.Messages;
import org.gravity.hulk.antipatterngraph.values.HRelativeValueConstants;
import org.gravity.hulk.detection.codesmells.HContollerClassDetector;
import org.gravity.hulk.detection.codesmells.HDataClassDetector;
import org.gravity.hulk.detection.codesmells.HIntenseFieldUsageDetector;
import org.gravity.hulk.detection.codesmells.HLargeClassDetector;
import org.gravity.hulk.detection.codesmells.HLowCohesionDetector;
import org.gravity.hulk.detection.codesmells.HManyParametersDetector;
import org.gravity.hulk.detection.codesmells.HMuchOverloadingDetector;
import org.moflon.core.dfs.DepthFirstSearch;
import org.moflon.core.dfs.DfsFactory;
import org.moflon.core.dfs.Node;

public class HulkDetector {
	
	private Hashtable<String, String> thresholds;
	private HAntiPatternHandling hulk;
	private Set<HDetector> initialized;
	
	public HulkDetector(HAntiPatternHandling hulk, Hashtable<String, String> thresholds) {
		this.hulk = hulk;
		this.thresholds = thresholds;
		initialized = new HashSet<>();
	}
	
	private List<HDetector> getSorted(HDetector detector){
		initDFS().processNode(detector);

		Comparator<Node> comp = new Comparator<Node>() {

			@Override
			public int compare(Node arg0, Node arg1) {
				return arg0.getPostTraversal() - arg1.getPostTraversal();
			}

		};

		List<HDetector> sorted = new ArrayList<>();
		for (HDetector n : hulk.getHDetector()) {
			if (n.getPreTraversal() > 0) {
				sorted.add(n);
			}
		}
		sorted.sort(comp);
		return sorted;
	}
	
	private void handleDetector(HDetector detector, Stack<HDetector> worklist, Set<HDetector> processed_detectors, boolean verbose){
		List<HDetector> sorted = getSorted(detector);
		for (HDetector n : sorted) {
			long t2 = 0;
			if (!processed_detectors.contains(n)) {
				if (worklist.contains(n)) {
					worklist.remove(n);
				}
				if(verbose){
					t2 = System.currentTimeMillis();
					System.out.println(t2 + " Hulk " + n.getGuiName());
				}
				if (n.detect(hulk.getApg())) {
					n.setPostTraversal(0);
					n.setPreTraversal(0);
					processed_detectors.add(n);
				} else {
					System.err.println(Messages.getString("detector.failed") + n); //$NON-NLS-1$
				}
				if(verbose){
					long t3 = System.currentTimeMillis();
					System.out.println(t3 + " Hulk " + n.getGuiName() + " - done " + (t3 - t2) + "ms");
				}
			}
		}
	}
	
	
	public boolean detectSelectedAntiPattern(Stack<HDetector> worklist, Set<HDetector> processed_detectors){
		return detectSelectedAntiPattern(worklist, processed_detectors, true);
	}
	
	public boolean detectSelectedAntiPattern(Stack<HDetector> worklist, Set<HDetector> processed_detectors, boolean verbose){
		long h0 = 0;
		if(verbose){
			h0 = System.currentTimeMillis();
			System.out.println(h0 + " Hulk Anti-Pattern Detection");
		}
		while (!worklist.isEmpty()) {
			HDetector detector = worklist.pop();
			if (detector instanceof HRelativeDetector) {
				HRelativeDetector relativeDetector = (HRelativeDetector) detector;
				if(thresholds.containsKey(relativeDetector.getClass().getName())){
					relativeDetector.setRelative(true);
					if(!initialized.contains(detector)){
						String value = thresholds.get(relativeDetector.getClass().getName());
						HRelativeValueConstants constant = HRelativeValueConstants.getByName(value);
						if(constant!=null){
							relativeDetector.setThreshold(relativeDetector.calculateRelativeThreshold(constant));
						}
						Double number = Double.valueOf(value); 
						if(number!=null){
							relativeDetector.setThreshold(number.doubleValue());
						}
						else {
							throw new RuntimeException();
						}
					}
				}
			}
			handleDetector(detector, worklist, processed_detectors, verbose);
		}
		if(verbose){
			long h1 = System.currentTimeMillis();
			System.out.println(h1 + " Hulk Anti-Pattern Detection - done " + (h1 - h0) + "ms");
		}

		return true;
	}

	public boolean detectSelectedAntiPattern(Set<EClass> selection,
			Set<HDetector> selected_detectors, Set<HDetector> processed_detectors) {
		Stack<HDetector> worklist = new Stack<>();

		// Fill worklist
		for (HDetector detector : hulk.getHDetector()) {
			if (selection.contains(detector.eClass())) {
				selected_detectors.add(detector);
				worklist.add(detector);
			}
		}
		
		if(selected_detectors.size()!=selection.size()){
			System.err.println("Not all detecors found.");
			return false;
		}
		
		return detectSelectedAntiPattern(worklist, processed_detectors);		
	}

	public static Hashtable<String, String> getDefaultThresholds() {
		Hashtable<String, String> thresholds = new Hashtable<String, String>();
		thresholds.put(HDataClassDetector.class.getName(), HRelativeValueConstants.HIGH.getName());
		thresholds.put(HLargeClassDetector.class.getName(), HRelativeValueConstants.VERY_HIGH.getName());
		thresholds.put(HLowCohesionDetector.class.getName(), HRelativeValueConstants.HIGH.getName());
		thresholds.put(HContollerClassDetector.class.getName(), HRelativeValueConstants.MEDIUM.getName());
		thresholds.put(HIntenseFieldUsageDetector.class.getName(), HRelativeValueConstants.VERY_HIGH.getName());
		thresholds.put(HMuchOverloadingDetector.class.getName(), HRelativeValueConstants.HIGH.getName());
		thresholds.put(HManyParametersDetector.class.getName(), HRelativeValueConstants.VERY_HIGH.getName());
		return thresholds;
	}

	private static DepthFirstSearch initDFS() {
		DepthFirstSearch dfs = DfsFactory.eINSTANCE.createDepthFirstSearch();
		DepthFirstSearch delegate = DfsFactory.eINSTANCE.createDepthFirstSearch();
		dfs.setDelegate(delegate);
		delegate.setDelegate(dfs);
		return dfs;
	}
}
