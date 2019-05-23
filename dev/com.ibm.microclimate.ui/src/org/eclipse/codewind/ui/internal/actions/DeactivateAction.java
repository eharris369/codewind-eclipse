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

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.eclipse.codewind.core.internal.InstallUtil;
import org.eclipse.codewind.core.internal.MCLogger;
import org.eclipse.codewind.core.internal.ProcessHelper.ProcessResult;
import org.eclipse.codewind.core.internal.connection.MicroclimateConnection;
import org.eclipse.codewind.core.internal.connection.MicroclimateConnectionManager;
import org.eclipse.codewind.ui.MicroclimateUIPlugin;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.codewind.ui.internal.views.ViewHelper;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.actions.SelectionProviderAction;

/**
 * Action to create a new project.
 */
public class DeactivateAction extends SelectionProviderAction {

	protected MicroclimateConnection connection;
	
	public DeactivateAction(ISelectionProvider selectionProvider) {
		super(selectionProvider, Messages.DeactivateActionLabel);
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
			MCLogger.logError("DeactivateAction ran but no Microclimate connection was selected"); //$NON-NLS-1$
			return;
		}

		try {
			MicroclimateConnectionManager.removeConnection(connection.baseUrl.toString());
			connection.close();
			ViewHelper.refreshMicroclimateExplorerView(null);
			Job job = new Job(Messages.DeactivateActionJobLabel) {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					// Try to stop Codewind
					try {
						ProcessResult result = InstallUtil.stopCodewind(monitor);
						if (result.getExitValue() != 0) {
							return new Status(IStatus.ERROR, MicroclimateUIPlugin.PLUGIN_ID, NLS.bind(Messages.DeactivateActionErrorWithMsg, result.getError()));
						}
					} catch (IOException e) {
						return new Status(IStatus.ERROR, MicroclimateUIPlugin.PLUGIN_ID, Messages.DeactivateActionError, e);
					} catch (TimeoutException e) {
						return new Status(IStatus.ERROR, MicroclimateUIPlugin.PLUGIN_ID, Messages.DeactivateActionTimeout, e);
					}
					return Status.OK_STATUS;
				}
			};
			job.schedule();
		} catch (Exception e) {
			MCLogger.logError("An error occurred deactivating connection: " + connection.baseUrl, e); //$NON-NLS-1$
		}
	}
}
