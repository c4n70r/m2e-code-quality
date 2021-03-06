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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.eclipse.core.runtime.IPath;

/**
 * A utility class to resolve resources, which includes searching in resources
 * specified with the {@literal <dependencies>} of the Maven pluginWrapper
 * configuration.
 * 
 * @since 0.9.8
 */
public final class ResourceResolver {

	private final ClassRealm pluginRealm;
	private final IPath projectLocation;

	public ResourceResolver(ClassRealm pluginRealm, IPath projectLocation) {
		this.pluginRealm = pluginRealm;
		this.projectLocation = projectLocation;
	}

	/**
	 * Resolve the resource location as per the maven pluginWrapper spec.
	 * <ol>
	 * <li>As a resource.</li>
	 * <li>As a URL.</li>
	 * <li>As a filesystem resource.</li>
	 * </ol>
	 * 
	 * @param location
	 *            the resource location as a string.
	 * @return the {@code URL} of the resolved location or {@code null}.
	 */
	public URL resolveLocation(final String location) {
		// 1. Try it as a resource first.
		if (pluginRealm != null) {
			String urlLocation = location;
			// note that class loaders don't want leading slashes.
			if (urlLocation.startsWith("/")) {
				urlLocation = urlLocation.substring(1);
			}
			URL url = pluginRealm.getResource(urlLocation);
			if (url != null) {
				return url;
			}
		}

		// 2. Try it as a remote resource.
		try {
			URL url = new URL(location);
			// check if valid.
			url.openStream();
			return url;
		} catch (MalformedURLException ex) {
			// ignored, try next
		} catch (Exception ex) {
			// ignored, try next
		}

		// 3. Try to see if it exists as a filesystem resource.
		File file = new File(location);
		if (file.exists()) {
			try {
				return file.toURI().toURL();
			} catch (MalformedURLException ex) {
				// ignored, try next
			}
		}

		File projectFile = projectLocation.append(location).toFile();
		if (projectFile.exists()) {
			try {
				return projectFile.toURI().toURL();
			} catch (MalformedURLException ex) {
				// ignored, try next
			}
		}

		// 4. null
		return null;
	}

}
