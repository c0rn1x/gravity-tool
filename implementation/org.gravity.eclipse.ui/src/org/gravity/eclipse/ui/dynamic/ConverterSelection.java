package org.gravity.eclipse.ui.dynamic;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.gravity.eclipse.GravityActivator;
import org.gravity.eclipse.converter.IPGConverterFactory;
import org.gravity.eclipse.exceptions.NoConverterRegisteredException;

/**
 * This class fills an menu with all available modisco to pm converters and
 * allows the selection of the active converter
 * 
 * @author speldszus
 *
 */
public class ConverterSelection extends ContributionItem {

	private static final Logger LOGGER = Logger.getLogger(ConverterSelection.class);

	@Override
	public void fill(Menu menu, int index) {
		IExtensionRegistry extension_registry = Platform.getExtensionRegistry();

		IConfigurationElement[] configuration_elements = extension_registry
				.getConfigurationElementsFor("org.gravity.eclipse.converters"); //$NON-NLS-1$

		IPGConverterFactory selected_converter;
		try {
			selected_converter = GravityActivator.getDefault().getSelectedConverterFactory();
		} catch (NoConverterRegisteredException e1) {
			MessageDialog.openError(getShell(), "No Converter installed",
					"Please install a converter from the GRaViTY updatesite.");
			return;
		} catch (CoreException e) {
			MessageDialog.openError(getShell(), "Critical ERROR",
					"The converter extensionpoint cannot be accessed, pleade contact the GRaViTY developers.");
			return;
		}

		for (IConfigurationElement element : configuration_elements) {
			try {
				IPGConverterFactory converter = (IPGConverterFactory) element.createExecutableExtension("class"); //$NON-NLS-1$

				MenuItem item = new MenuItem(menu, SWT.RADIO, index);
				item.setText(converter.getName());
				item.setToolTipText(converter.getDescription());
				if (selected_converter.getClass().equals(converter.getClass())) {
					item.setSelection(true);
				}
				item.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						item.setSelection(true);
						GravityActivator.getDefault().setSelectedConverterFactory(converter);
					}
				});
			} catch (CoreException e) {
				LOGGER.log(Level.ERROR, e.getLocalizedMessage(), e);
			}
		}
	}

	/**
	 * Gets the current active shell
	 * 
	 * @return the shell
	 */
	public static Shell getShell() {
		Display display = Display.getCurrent();
		// may be null if outside the UI thread
		if (display == null)
			display = Display.getDefault();
		return display.getActiveShell();
	}
}
