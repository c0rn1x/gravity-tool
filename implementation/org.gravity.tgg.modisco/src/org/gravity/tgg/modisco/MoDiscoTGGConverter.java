package org.gravity.tgg.modisco;

import static org.gravity.eclipse.io.ModelSaver.saveModel;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.function.Consumer;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.common.util.BasicMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.modisco.infra.discovery.core.exception.DiscoveryException;
import org.eclipse.modisco.java.generation.files.GenerateJavaExtended;
import org.gravity.eclipse.GravityActivator;
import org.gravity.eclipse.converter.IPGConverter;
import org.gravity.eclipse.util.EclipseProjectUtil;
import org.gravity.modisco.GravityMoDiscoModelPatcher;
import org.gravity.modisco.MGravityModel;
import org.gravity.modisco.discovery.GravityModiscoProjectDiscoverer;
import org.gravity.modisco.processing.GravityMoDiscoProcessorUtil;
import org.gravity.modisco.processing.IMoDiscoProcessor;
import org.gravity.tgg.modisco.processing.pg.IProgramGraphProcessor;
import org.gravity.tgg.modisco.processing.pg.ProgramGraphProcesorUtil;
import org.gravity.typegraph.basic.TypeGraph;
import org.moflon.tgg.algorithm.synchronization.SynchronizationHelper;
import org.moflon.tgg.language.analysis.StaticAnalysis;

/**
 * A converter for creating a program model from eclipse projects using MoDisco
 *
 * @author speldszus
 *
 */
public class MoDiscoTGGConverter extends SynchronizationHelper implements IPGConverter {

	private final IJavaProject iJavaProject;

	private boolean debug;

	private final GravityModiscoProjectDiscoverer discoverer;

	private MGravityModel preprocessedModiscoModel;

	private Resource tggRulesResource;

	private static final Logger LOGGER = Logger.getLogger(MoDiscoTGGConverter.class);

	/**
	 * Initializes ResourceSet for EMF and eMoflon
	 *
	 * @param project The project this converter is created for
	 *
	 * @throws IOException If the eMoflon TGG rules couldn't be loaded
	 */
	public MoDiscoTGGConverter(final IJavaProject project) throws IOException {
		this.discoverer = new GravityModiscoProjectDiscoverer();
		this.iJavaProject = project;
//		BasicConfigurator.configure();
		init(this.discoverer.getResourceSet());
		GravityActivator.getDefault().addProject(project.getProject());
	}

	/**
	 * Initializes the class
	 * 
	 * @param set The resource set which should be used
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	private void init(ResourceSet set) throws IOException, MalformedURLException {
		this.set = set;
		this.set.getResourceFactoryRegistry().getExtensionToFactoryMap()
				.put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());

		setCorrPackage(ModiscoPackage.eINSTANCE);

		this.configurator = new org.moflon.tgg.algorithm.configuration.Configurator() {
		};
		clearChanges();

		loadRulesFromProject();
	}

	private void loadRulesFromProject() throws IOException, MalformedURLException {
		String smaXmiURI = "platform:/plugin/org.gravity.tgg.modisco/model/Modisco.sma.xmi"; //$NON-NLS-1$
		try (InputStream tggRulesStream = new URL(smaXmiURI).openConnection().getInputStream()) {
			this.tggRulesResource = this.set.createResource(URI.createURI(smaXmiURI));
			this.tggRulesResource.load(tggRulesStream, Collections.EMPTY_MAP);
		}

		setRules((StaticAnalysis) this.tggRulesResource.getContents().get(0));
	}

	@Override
	public boolean discard() {
		clearChanges();
		reset();
		return true;
	}

	@Override
	public boolean convertProject(final IProgressMonitor monitor) {
		return convertProject(new HashSet<>(), monitor);
	}

	@Override
	public boolean convertProject(final Collection<IPath> libs, final IProgressMonitor monitor) {
		IProgressMonitor progressMonitor;
		if (monitor == null) {
			progressMonitor = new NullProgressMonitor();
		} else {
			progressMonitor = monitor;
		}

		long start = 0;
		final boolean infoEnabled = LOGGER.isInfoEnabled();
		if (infoEnabled) {
			start = System.currentTimeMillis();
			LOGGER.log(Level.INFO, "GRaViTY convert project: " + this.iJavaProject.getProject().getName());
		}

		try {
			this.preprocessedModiscoModel = this.discoverer.discoverMGravityModelFromProject(this.iJavaProject, libs,
					progressMonitor);
		} catch (final DiscoveryException e) {
			LOGGER.log(Level.ERROR, e.getMessage(), e);
			return false;
		}

		final boolean success = convertModel(this.preprocessedModiscoModel, progressMonitor);

		if (infoEnabled) {
			LOGGER.log(Level.INFO, "GRaViTY convert project - done " + (System.currentTimeMillis() - start) + "ms");
		}

		return success;
	}

	/**
	 * Converts the modisco model of the given project into a program model
	 *
	 * @param targetModel The modisco model of the Java project
	 * @param monitor     A progress monitor
	 * @return If the model has been converted successfully
	 */
	public boolean convertModel(final MGravityModel targetModel, final IProgressMonitor monitor) {
		Resource eResource = targetModel.eResource();
		if (!getResourceSet().equals(eResource.getResourceSet())) {
			getResourceSet().createResource(eResource.getURI()).getContents().add(targetModel);
		}
		setSrc(targetModel);
		setChangeSrc(null);
		clearChanges();
		setSynchronizationProtocol(null);

		final boolean infoEnabled = LOGGER.isInfoEnabled();
		long start = 0;
		if (infoEnabled) {
			start = System.currentTimeMillis();
			LOGGER.log(Level.INFO, "eMoflon TGG fwd trafo");
		}

		integrateForward();
		if (infoEnabled) {
			LOGGER.log(Level.INFO, "eMoflon TGG fwd trafo - done " + (System.currentTimeMillis() - start) + "ms");
		}

		boolean success = trg != null && trg instanceof TypeGraph;
		if (success) {
			final Collection<IProgramGraphProcessor> sortedProcessors = ProgramGraphProcesorUtil
					.getSortedProcessors(MoDiscoTGGActivator.PROCESS_PG_FWD);
			if (infoEnabled) {
				LOGGER.log(Level.INFO, "Start postprocessing with " + sortedProcessors.size() + " post-processors");
			}
			for (final IProgramGraphProcessor processor : sortedProcessors) {
				processor.process(getPG(), monitor);
			}
			if (infoEnabled) {
				LOGGER.log(Level.INFO, "Postprocessing - done ");
			}
		}
		return success;
	}

	@Override
	public boolean syncProjectFwd(final IProgressMonitor monitor) {
		if (this.discoverer == null || this.iJavaProject == null) {
			return false;
		}
		long start = 0;
		final boolean infoEnabled = LOGGER.isInfoEnabled();
		if (infoEnabled) {
			start = System.currentTimeMillis();
			LOGGER.log(Level.INFO, start + " MoDisco sync project: " + this.iJavaProject.getProject().getName());
		}

		if (this.preprocessedModiscoModel == null) {
			return convertProject(monitor);
		}

		final MGravityModel oldProject = this.preprocessedModiscoModel;

		if (infoEnabled) {
			LOGGER.log(Level.INFO, System.currentTimeMillis() + " Discover Project");
		}

		try {
			this.preprocessedModiscoModel = this.discoverer.discoverMGravityModelFromProject(this.iJavaProject,
					monitor);
		} catch (final DiscoveryException e) {
			LOGGER.error(e);
			return false;
		}
		if (infoEnabled) {
			LOGGER.log(Level.INFO, System.currentTimeMillis() + " Discover Project - Done"); // NOPMD by speldszus on
			// 12/4/19, 9:09 PM
		}

		final GravityMoDiscoModelPatcher patcher = MoDiscoTGGActivator.getDefault().getSelectedPatcher();

		// final Consumer<EObject> changes = SynchronizationHelper -> {

		LOGGER.log(Level.INFO, System.currentTimeMillis() + " Calculate Patch");
		patcher.update(oldProject, this.preprocessedModiscoModel);
		LOGGER.log(Level.INFO, System.currentTimeMillis() + " Calculate Patch - Done");

		// };

		final boolean success = syncProjectFwd(null, monitor);

		if (infoEnabled) {
			final long stop = System.currentTimeMillis();
			LOGGER.log(Level.INFO, stop + "MoDisco sync project -done: " + (stop - start) + "ms");
		}

		return success;
	}

	@Override
	public boolean syncProjectFwd(final Consumer<EObject> consumer, final IProgressMonitor monitor) {
		if (getSrc() == null) {
			LOGGER.error("No initial transformation has been performed!");
			return false;
		}
		final boolean infoEnabled = LOGGER.isInfoEnabled();
		if (infoEnabled) {
			LOGGER.log(Level.INFO, System.currentTimeMillis() + " Integrate FWD");
		}

		setChangeSrc(consumer);
		integrateForward();

		if (infoEnabled) {
			LOGGER.log(Level.INFO, System.currentTimeMillis() + " Integrate FWD - Done");
		}

		if (this.debug) {
			save(monitor);
		}
		return getTrg() != null;
	}

	@Override
	public boolean syncProjectBwd(final Consumer<EObject> consumer, final IProgressMonitor monitor) {
		if (this.discoverer == null || getSrc() == null || getTrg() == null) {
			return false;
		}

		IProgressMonitor progressMonitor;
		if (monitor == null) {
			progressMonitor = new NullProgressMonitor();
		} else {
			progressMonitor = monitor;
		}

		for (final IProgramGraphProcessor processor : ProgramGraphProcesorUtil
				.getSortedProcessors(MoDiscoTGGActivator.PROCESS_PG_BWD)) {
			processor.process(getPG(), progressMonitor);
		}
		setChangeTrg(consumer);
		integrateBackward();

		if (this.debug) {
			save(progressMonitor);

		}

		final EObject src = getSrc();
		for (final IMoDiscoProcessor processor : GravityMoDiscoProcessorUtil
				.getSortedProcessors(MoDiscoTGGActivator.PROCESS_MODISCO_BWD)) {
			processor.process((MGravityModel) src, null, progressMonitor);
		}

		try {
			final IFolder srcFile = this.iJavaProject.getProject().getFolder("src");
			new GenerateJavaExtended(src, srcFile.getLocation().toFile(), Collections.emptyList())
					.doGenerate(new BasicMonitor.EclipseSubProgress(progressMonitor, 1));
			this.iJavaProject.getProject().refreshLocal(IResource.DEPTH_INFINITE, progressMonitor);
		} catch (IOException | CoreException e) {
			return false;
		}

		return true;
	}

	/**
	 * Saves program model, MoDisco model, correspondence model and synchronization
	 * protocol to the modisco folder
	 *
	 * @param direction       a description of the direction of the conversion
	 * @param progressMonitor a progress monitor return false if the models couldn't
	 *                        be saved, true otherwise
	 */
	boolean save(final IProgressMonitor progressMonitor) {
		IFolder folder;
		try {
			folder = EclipseProjectUtil.getGravityFolder(this.iJavaProject.getProject(), progressMonitor);
		} catch (final IOException e) {
			LOGGER.error(e);
			return false;
		}
		if (!savePG(folder.getFile("sync_pm.xmi"), progressMonitor)) { //$NON-NLS-1$
			return false;
		}
		if (!saveModel(getSrc(), folder.getFile("sync__modisco.xmi"), //$NON-NLS-1$
				progressMonitor)) {
			return false;
		}
		if (!saveModel(getCorr(), folder.getFile("sync_correspondence_model.xmi"), //$NON-NLS-1$
				progressMonitor)) {
			return false;
		}
		saveSynchronizationProtocol(folder.getFile("sync__protocol.xmi").getLocation().toString()); //$NON-NLS-1$
		return true;
	}

	@Override
	public boolean savePG(final IFile file, final IProgressMonitor monitor) {
		return saveModel(getTrg(), file, monitor);
	}

	@Override
	public TypeGraph getPG() {
		return (TypeGraph) getTrg();
	}

	/**
	 * Resets the converter to initial values
	 */
	private void reset() {
		if (src != null) {
			src.eResource().unload();
			src = null;
		}
		if (trg != null) {
			trg.eResource().unload();
			trg = null;
		}
		if (corr != null) {
			corr.eResource().unload();
			corr = null;
		}
		if (protocol != null) {
			protocol = null;
		}
		set = new ResourceSetImpl();
	}

	/**
	 * Sets the eMoflon changes to empty consumers
	 */
	private void clearChanges() {
		this.changeSrc = (root -> {
		});
		this.changeTrg = (root -> {
		});
	}

	@Override
	public boolean isDebug() {
		return this.debug;
	}

	@Override
	public void setDebug(final boolean debug) {
		this.debug = debug;
	}
}
