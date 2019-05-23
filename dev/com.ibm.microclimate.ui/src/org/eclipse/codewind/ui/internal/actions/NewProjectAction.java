/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *	 IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.ui.internal.actions;

import java.util.List;

import org.eclipse.codewind.core.internal.MCLogger;
import org.eclipse.codewind.core.internal.connection.MicroclimateConnection;
import org.eclipse.codewind.core.internal.console.ProjectTemplateInfo;
import org.eclipse.codewind.ui.MicroclimateUIPlugin;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.codewind.ui.internal.wizards.NewMicroclimateProjectWizard;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.actions.SelectionProviderAction;

/**
 * Action to create a new project.
 */
public class NewProjectAction extends SelectionProviderAction {

	protected MicroclimateConnection connection;
	
	public NewProjectAction(ISelectionProvider selectionProvider) {
		super(selectionProvider, Messages.NewProjectAction_Label);
		setImageDescriptor(MicroclimateUIPlugin.getDefaultIcon());
		selectionChanged(getStructuredSelection());
	}


	@Override
	public void selectionChanged(IStructuredSelection sel) {
		if (sel.size() == 1) {
			Object obj = sel.getFirstElement();
			if (obj instanceof MicroclimateConnection) {
				connection = (MicroclimateConnection)obj;
				setEnabled(connection.isConnected());
				return;
			}
		}
		setEnabled(false);
	}

	@Override
	public void run() {
		if (connection == null) {
			// should not be possible
			MCLogger.logError("NewProjectAction ran but no Microclimate connection was selected"); //$NON-NLS-1$
			return;
		}

		try {
			List<ProjectTemplateInfo> templates = connection.requestProjectTemplates();
			NewMicroclimateProjectWizard wizard = new NewMicroclimateProjectWizard(connection, templates);
			WizardDialog dialog = new WizardDialog(Display.getDefault().getActiveShell(), wizard);
			if (dialog.open() == Window.CANCEL) {
				return;
			}
		} catch (Exception e) {
			MCLogger.logError("An error occurred running the new project action on connection: " + connection.baseUrl, e); //$NON-NLS-1$
		}
	}
}
