package org.gravity.hulk.tests.api;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.gravity.hulk.HulkAPI;
import org.gravity.hulk.HulkAPI.AntiPatternNames;
import org.gravity.hulk.antipatterngraph.HAnnotation;
import org.gravity.hulk.antipatterngraph.HMetric;
import org.gravity.hulk.antipatterngraph.antipattern.HBlobAntiPattern;
import org.gravity.hulk.antipatterngraph.metrics.HIGAMMetric;
import org.gravity.hulk.antipatterngraph.metrics.HIGATMetric;
import org.gravity.hulk.exceptions.DetectionFailedException;
import org.gravity.typegraph.basic.TypeGraph;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * A class for testing the HulkAPI
 * This test should be executed as JUnit plugin test.
 * The test subjects have to be loaded in the workspace of the plugin test
 * 
 * @author speldszus
 *
 */
@RunWith(Parameterized.class)
public class HulkApiTest {

	private static final Logger LOGGER = Logger.getLogger(HulkApiTest.class.getName());

	private IJavaProject javaProject;

	/**
	 * Creates a new test instance for a a project
	 * 
	 * @param name    The name of the project
	 * @param project The project
	 */
	public HulkApiTest(String name, IJavaProject project) {
		LOGGER.log(Level.INFO, "Starting HulkAPI test with project: " + name);
		this.javaProject = project;
	}

	/**
	 * Collects the projects from the current workspace on which Hulk should be executed
	 * 
	 * @return A list of projects and their names
	 */
	@Parameters(name = "{index}: Test HulkAPI on \"{0}\"")
	public static Collection<Object[]> collectProjects() {
		Collection<Object[]> data = new LinkedList<>();
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		for (IProject project : root.getProjects()) {
			try {
				if (project.hasNature(JavaCore.NATURE_ID)) {
					IJavaProject javaProject = JavaCore.create(project);
					data.add(new Object[] { project.getName(), javaProject });
				}
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		return data;
	}

	/**
	 * The HulkAPI is used to detect Blob anti-pattern
	 * 
	 * @throws DetectionFailedException If the detection failed
	 */
	@Test
	public void detectBlobs() throws DetectionFailedException {
		List<HAnnotation> results = HulkAPI.detect(javaProject, new NullProgressMonitor(), AntiPatternNames.BLOB);
		LOGGER.log(Level.INFO, "Number of Blobs = " + results.size());
	}

	/**
	 * The HulkAPI is used to calculate the IGAM metric
	 * 
	 * @throws DetectionFailedException If the detection failed
	 */
	@Test
	public void detectIGAM() throws DetectionFailedException {
		List<HAnnotation> results = HulkAPI.detect(javaProject, new NullProgressMonitor(), AntiPatternNames.IGAM);
		for (HAnnotation hAnnotation : results) {
			if (hAnnotation.getTAnnotated() instanceof TypeGraph) {
				LOGGER.log(Level.INFO, "IGAM = " + ((HMetric) hAnnotation).getValue());
			}
		}
	}

	/**
	 * The HulkAPI is used to calculate the IGAT metric
	 * 
	 * @throws DetectionFailedException If the detection failed
	 */
	@Test
	public void detectIGAT() throws DetectionFailedException {
		List<HAnnotation> results = HulkAPI.detect(javaProject, new NullProgressMonitor(), AntiPatternNames.IGAT);
		for (HAnnotation hAnnotation : results) {
			if (hAnnotation.getTAnnotated() instanceof TypeGraph) {
				LOGGER.log(Level.INFO, "IGAT = " + ((HMetric) hAnnotation).getValue());
			}
		}
	}

	/**
	 * The HulkAPI is used to calculate multiple metrics and anti-patterns
	 * 
	 * @throws DetectionFailedException If the detection failed
	 */
	@Test
	public void detectAll() throws DetectionFailedException {
		List<HAnnotation> results = HulkAPI.detect(javaProject, new NullProgressMonitor(), AntiPatternNames.BLOB,
				AntiPatternNames.IGAT, AntiPatternNames.IGAM);
		int blobs = 0;
		for (HAnnotation hAnnotation : results) {
			if (hAnnotation instanceof HBlobAntiPattern) {
				blobs++;
			}
			if (hAnnotation.getTAnnotated() instanceof TypeGraph) {
				if (hAnnotation instanceof HIGAMMetric) {
					LOGGER.log(Level.INFO, "IGAM = " + ((HMetric) hAnnotation).getValue());
				} else if (hAnnotation instanceof HIGATMetric) {
					LOGGER.log(Level.INFO, "IGAT = " + ((HMetric) hAnnotation).getValue());
				}
			}
		}
		LOGGER.log(Level.INFO, "Blobs = " + blobs);
	}
}