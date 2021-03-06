package org.gravity.hulk.ui.dialogs;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.gravity.hulk.HDetector;

public class DetectionTreeContentProvider implements ITreeContentProvider {

	/**
	 * The logger of this class
	 */
	private static final Logger LOGGER = Logger.getLogger(DetectionTreeContentProvider.class);

	@Override
	public void dispose() {
		LOGGER.info("Dispose");
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if(LOGGER.isInfoEnabled()) {
			LOGGER.info("Input changed from \""+oldInput+ "\" to \""+newInput+"\"");
		}
	}

	@Override
	public Object[] getElements(Object inputElement) {
		final List<Object> elements = new ArrayList<>();
		if (inputElement instanceof EPackage) {
			final EPackage epackage = (EPackage) inputElement;
			for (final EPackage esubpackage : epackage.getESubpackages()) {
				if (epackage.equals(esubpackage.getESuperPackage())) {
					elements.add(esubpackage);
				}
			}
			for (final EClassifier eclassifier : epackage.getEClassifiers()) {
				if (epackage.equals(eclassifier.getEPackage()) && !eclassifier.getInstanceClass().isInterface()) {
					elements.add(eclassifier);
				}
			}
		}
		return elements.toArray();
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		final List<Object> elements = new ArrayList<>();
		if (parentElement instanceof EPackage) {
			final EPackage epackage = (EPackage) parentElement;
			for (final EPackage esubpackage : epackage.getESubpackages()) {
				if (epackage.equals(esubpackage.getESuperPackage())) {
					elements.add(esubpackage);
				}
			}
			for (final EClassifier eclassifier : epackage.getEClassifiers()) {
				if (HDetector.class.isAssignableFrom(eclassifier.getInstanceClass())&&!((EClass) eclassifier).isAbstract()) {
					elements.add(eclassifier);

				}
			}
		}
		return elements.toArray();
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof EPackage) {
			return ((EPackage) element).getESuperPackage();
		} else if (element instanceof EClassifier) {
			return ((EClassifier) element).getEPackage();
		}
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof EPackage) {
			final EPackage ePackage = (EPackage) element;
			return !ePackage.getESubpackages().isEmpty() || !ePackage.getEClassifiers().isEmpty();
		}
		return false;
	}
}
