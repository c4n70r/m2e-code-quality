/*******************************************************************************
 * Copyright 2010 Mohan KR
 * Copyright 2010 Basis Technology Corp.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.basistech.m2e.code.quality.shared;

import java.util.List;
import java.util.Map;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;

import com.google.common.base.Preconditions;

/**
 * An abstract class merging the required callbacks of
 * {@link AbstractProjectConfigurator}, suitable for maven plugins that have a
 * corresponding Eclipse Plugin, whose configuration can be established by the
 * plugin configuration.
 * 
 */
public abstract class AbstractMavenPluginProjectConfigurator extends
        AbstractProjectConfigurator {

	@Override
	public <T> T getParameterValue(MavenProject project, String parameter,
	        Class<T> asType, MojoExecution mojoExecution,
	        IProgressMonitor monitor) throws CoreException {
		return super.getParameterValue(project, parameter, asType,
		        mojoExecution, monitor);
	}

	protected MojoExecution findForkedExecution(MojoExecution primary,
	        String groupId, String artifactId, String goal) {
		Map<String, List<MojoExecution>> forkedExecutions =
		        primary.getForkedExecutions();
		MojoExecution goalExecution = null;
		for (List<MojoExecution> possibleExecutionList : forkedExecutions
		        .values()) {
			for (MojoExecution possibleExecution : possibleExecutionList) {
				if (groupId.equals(possibleExecution.getGroupId())
				        && artifactId.equals(possibleExecution.getArtifactId())
				        && goal.equals(possibleExecution.getGoal())) {
					goalExecution = possibleExecution;
					break;
				}
			}
			if (goalExecution != null) {
				break;
			}
		}
		return goalExecution;
	}

	@Override
	public void configure(final ProjectConfigurationRequest request,
	        final IProgressMonitor monitor) throws CoreException {

		final MavenProject mavenProject = request.getMavenProject();
		if (mavenProject == null) {
			return;
		}

		final MavenPluginWrapper pluginWrapper =
		        this.getMavenPlugin(monitor, request.getMavenProjectFacade());
		final IProject project = request.getProject();

		if (!pluginWrapper.isPluginConfigured()) {
			return;
		}

		@SuppressWarnings("deprecation")
		MavenSession mavenSession = request.getMavenSession();

		this.handleProjectConfigurationChange(request.getMavenProjectFacade(),
		        project, monitor, pluginWrapper, mavenSession);
	}

	@Override
	public void mavenProjectChanged(
	        final MavenProjectChangedEvent mavenProjectChangedEvent,
	        final IProgressMonitor monitor) throws CoreException {
		final IMavenProjectFacade mavenProjectFacade =
		        mavenProjectChangedEvent.getMavenProject();
		final MavenPluginWrapper pluginWrapper =
		        this.getMavenPlugin(monitor, mavenProjectFacade);
		final IProject project = mavenProjectFacade.getProject();

		if (this.checkUnconfigurationRequired(monitor, mavenProjectFacade,
		        mavenProjectChangedEvent.getOldMavenProject())) {
			this.unconfigureEclipsePlugin(project, monitor);
			return;
		}
		if (pluginWrapper.isPluginConfigured()) {
			@SuppressWarnings("deprecation")
			MavenExecutionRequest request =
			        maven.createExecutionRequest(monitor);
			@SuppressWarnings("deprecation")
			MavenSession session =
			        maven.createSession(request, mavenProjectChangedEvent
			                .getMavenProject().getMavenProject(monitor));
			this.handleProjectConfigurationChange(mavenProjectFacade, project,
			        monitor, pluginWrapper, session);
		} else {
			// TODO: redirect to eclipse logger.
			// this.console.logMessage(String.format(
			// "Will not configure the Eclipse Plugin for Maven Plugin [%s:%s],"
			// +
			// "(Could not find maven plugin instance or configuration in pom)",
			// this.getMavenPluginGroupId(),
			// this.getMavenPluginArtifactId()));
		}
	}

	protected abstract void handleProjectConfigurationChange(
	        final IMavenProjectFacade mavenProjectFacade,
	        final IProject project, final IProgressMonitor monitor,
	        final MavenPluginWrapper mavenPluginWrapper, MavenSession session)
	        throws CoreException;

	/**
	 * Get the maven plugin {@code groupId}.
	 * 
	 * @return the {@code artifactId}.
	 */
	protected abstract String getMavenPluginGroupId();

	/**
	 * Get the maven plugin {@code artifactId}.
	 * 
	 * @return the {@code groupId}.
	 */
	protected abstract String getMavenPluginArtifactId();

	/**
	 * @return the specific goals that this class works on, or null if it all
	 *         goals apply. Null may lead to chaotic overlaying of multiple
	 *         configurations. If more than one, this will process in order
	 *         looking for an execution.
	 */
	protected String[] getMavenPluginGoal() {
		return null;
	}

	/**
	 * Unconfigure the associated Eclipse plugin.
	 * 
	 * @param project
	 *            the {@link IProject} instance.
	 * @param monitor
	 *            the {@link IProgressMonitor} instance.
	 * @throws CoreException
	 *             if unconfiguring the eclipse plugin fails.
	 */
	protected abstract void unconfigureEclipsePlugin(final IProject project,
	        final IProgressMonitor monitor) throws CoreException;

	/**
	 * Helper to check if a Eclipse plugin unconfiguration is needed. This
	 * usually happens if the maven plugin has been unconfigured.
	 * 
	 * @param curMavenProjectFacade
	 *            the current {@code IMavenProjectFacade}.
	 * @param oldMavenProjectFacade
	 *            the previous {@code IMavenProjectFacade}.
	 * @return {@code true} if the Eclipse plugin configuration needs to be
	 *         deleted.
	 * @throws CoreException
	 */
	private boolean checkUnconfigurationRequired(IProgressMonitor monitor,
	        final IMavenProjectFacade curMavenProjectFacade,
	        final IMavenProjectFacade oldMavenProjectFacade)
	        throws CoreException {
		Preconditions.checkNotNull(curMavenProjectFacade);

		if (oldMavenProjectFacade == null) {
			return false;
		}
		final MavenPluginWrapper newMavenPlugin =
		        this.getMavenPlugin(monitor, curMavenProjectFacade);
		final MavenPluginWrapper oldMavenPlugin =
		        this.getMavenPlugin(monitor, oldMavenProjectFacade);
		if (!newMavenPlugin.isPluginConfigured()
		        && oldMavenPlugin.isPluginConfigured()) {
			return true;
		}
		return false;
	}

	public ResourceResolver getResourceResolver(MojoExecution mojoExecution,
	        MavenSession session, IPath projectLocation) throws CoreException {
		// call for side effect of ensuring that the realm is set in the
		// descriptor.
		IMaven mvn = MavenPlugin.getMaven();
		Mojo configuredMojo =
		        mvn.getConfiguredMojo(session, mojoExecution, Mojo.class);
		mvn.releaseMojo(configuredMojo, mojoExecution);
		return new ResourceResolver(mojoExecution.getMojoDescriptor()
		        .getPluginDescriptor().getClassRealm(), projectLocation);
	}

	private MavenPluginWrapper getMavenPlugin(IProgressMonitor monitor,
	        final IMavenProjectFacade projectFacade) throws CoreException {
		return MavenPluginWrapper
		        .newInstance(monitor, getMavenPluginGroupId(),
		                getMavenPluginArtifactId(), getMavenPluginGoal(),
		                projectFacade);
	}

}
