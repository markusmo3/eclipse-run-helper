/*
 *    Copyright 2012 Chris Sinjakli
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package uk.co.sinjakli.eclipserunhelper.handlers;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationManager;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jdt.internal.junit.launcher.JUnitLaunchConfigurationConstants;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import uk.co.sinjakli.eclipserunhelper.RunHelperPlugin;
import uk.co.sinjakli.eclipserunhelper.ui.RunHelperDialog;

@SuppressWarnings("restriction")
public class DisplayRunHelperHandler extends AbstractHandler {

	private static final String[] KEY_CODES = new String[] {
			"1", "2", "3", "4", "5", "6", "7", "8", "9",
			"a", "s", "d", "f", "g", "h", "j", "k", "l",
	};
	private ILog logger;

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		logger = RunHelperPlugin.getDefault().getLog();

		final String launchTypeParemeter = event.getParameter("uk.co.sinjakli.eclipserunhelper.launchType");
		final String launchType;
		final String launchGroupId;
		if (launchTypeParemeter.startsWith("RUN")) {
			launchType = ILaunchManager.RUN_MODE;
			launchGroupId = IDebugUIConstants.ID_RUN_LAUNCH_GROUP;
		} else { // startsWith "DEBUG"
			launchType = ILaunchManager.DEBUG_MODE;
			launchGroupId = IDebugUIConstants.ID_DEBUG_LAUNCH_GROUP;
		}

		final ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		final LaunchConfigurationManager launchConfigurationManager = DebugUIPlugin.getDefault()
				.getLaunchConfigurationManager();

		final String launchCategory;
		ILaunchConfiguration[] launchConfigs = null;
		if (launchTypeParemeter.endsWith("HISTORY")) {
			launchConfigs = launchConfigurationManager
					.getLaunchHistory(launchGroupId)
					.getHistory();
			launchCategory = "History";
		} else { // endsWith "FAVORITES"
			launchConfigs = launchConfigurationManager
					.getLaunchHistory(launchGroupId)
					.getFavorites();
			launchCategory = "Favorites";
		}
		launchConfigs = LaunchConfigurationManager.filterConfigs(launchConfigs);

		
		final Map<String, ILaunchConfiguration> availableLaunches = new LinkedHashMap<String, ILaunchConfiguration>();
		for (int i = 0; i < KEY_CODES.length && i < launchConfigs.length; i++) {
			availableLaunches.put(KEY_CODES[i], launchConfigs[i]);
		}

		final ILaunchConfiguration lastJUnitLaunch = getLastJunitLaunch(launchManager, launchConfigs);
		if (lastJUnitLaunch != null) {
			availableLaunches.put("t", lastJUnitLaunch);
		}
		
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				final Shell activeShell = PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow().getShell();

				final RunHelperDialog dialog = new RunHelperDialog(activeShell,
						availableLaunches, launchType, launchCategory);

				dialog.open();
			}
		});

		return null;
	}
	

	private ILaunchConfiguration getLastJunitLaunch(final ILaunchManager launchManager,
			final ILaunchConfiguration[] launchHistory) {
		final ILaunchConfigurationType jUnitLaunchType = launchManager
				.getLaunchConfigurationType(JUnitLaunchConfigurationConstants.ID_JUNIT_APPLICATION);

		for (final ILaunchConfiguration launchConfiguration : launchHistory) {
			try {
				if (launchConfiguration.getType().equals(jUnitLaunchType)) {
					return launchConfiguration;
				}
			} catch (final CoreException e) {
				final IStatus errorStatus = RunHelperPlugin.errorStatus("Error finding JUnit launch configuration.", e);
				logger.log(errorStatus);
			}
		}

		return null;
	}
}
