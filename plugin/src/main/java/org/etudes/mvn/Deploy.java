/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/plugin/src/main/java/org/etudes/mvn/Deploy.java $
 * $Id: Deploy.java 7244 2014-01-23 01:19:11Z ggolden $
 ***********************************************************************************
 *
 * Copyright (c) 2013 Etudes, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **********************************************************************************/

package org.etudes.mvn;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

/**
 * Deploy the artifact to the Tomcat image.
 * 
 * @goal deploy
 */
public class Deploy extends AbstractMojo
{
	/** @parameter default-value="${localRepository}" */
	private ArtifactRepository localRepository;

	/** @parameter default-value="${project.remoteArtifactRepositories}" */
	private List<Object> remoteRepositories;

	/** @component */
	private ArtifactResolver resolver;

	/** @parameter default-value="${maven.tomcat.home}" */
	private String tomcat;

	/** @parameter default-value="${project}" */
	protected MavenProject mavenProject;

	/**
	 * Do it!
	 */
	public void execute() throws MojoExecutionException
	{
		String packaging = mavenProject.getPackaging();
		String target = mavenProject.getProperties().getProperty("deploy.target");
		if (packaging.equals("war") || (packaging.equals("jar") && (target != null)) || (packaging.equals("pom") && (target != null)))
		{
			Artifact artifact = mavenProject.getArtifact();
			try
			{
				resolver.resolve(artifact, remoteRepositories, localRepository);

				if (packaging.equals("war"))
				{
					deployWar(artifact);
				}
				else if (packaging.equals("jar"))
				{
					deployJar(artifact);
				}
				else if (packaging.equals("pom"))
				{
					deployRuntimeDependentJars();
				}
			}
			catch (ArtifactResolutionException e)
			{
				getLog().info("artifact: file: missing: " + e.toString());
			}
			catch (ArtifactNotFoundException e)
			{
				getLog().info("artifact: file: missing: " + e.toString());
			}
		}
	}

	/**
	 * Copy a file from to.
	 * 
	 * @param from
	 *        The from location.
	 * @param to
	 *        The to location.
	 */
	protected void copyFile(File from, File to)
	{
		getLog().info("deploying from: " + from.getAbsolutePath() + " to: " + to.getAbsolutePath());
		try
		{
			FileUtils.copyFile(from, to);
		}
		catch (IOException e)
		{
			getLog().info("copyFile error: " + e.toString());
		}
	}

	/**
	 * Deploy the artifact jar file.
	 * 
	 * @param artifact
	 *        The artifact.
	 */
	protected void deployJar(Artifact artifact)
	{
		// jars go to (tomcat 7) tomcat/lib
		File to = new File(tomcat + "/lib/" + artifact.getArtifactId() + "-" + artifact.getVersion() + ".jar");
		copyFile(artifact.getFile(), to);
	}

	/**
	 * Deploy the artifact's dependencies marked as "runtime"
	 */
	@SuppressWarnings("unchecked")
	protected void deployRuntimeDependentJars()
	{
		// poms with deploy target property get all their dependencies marked as scope=runtime deployed
		Set<Artifact> artifacts = mavenProject.getDependencyArtifacts();
		for (Artifact a : artifacts)
		{
			// ignore any not with the runtime scope
			if (!"runtime".equals(a.getScope())) continue;

			try
			{
				resolver.resolve(a, remoteRepositories, localRepository);
				File to = new File(tomcat + "/lib/" + a.getArtifactId() + "-" + a.getVersion() + ".jar");
				copyFile(a.getFile(), to);
			}
			catch (ArtifactResolutionException e)
			{
				getLog().info("artifact: file: missing: " + e.toString());
			}
			catch (ArtifactNotFoundException e)
			{
				getLog().info("artifact: file: missing: " + e.toString());
			}
		}
	}

	/**
	 * Deploy the artifact war file to tomcat, removing the old expanded war directory.
	 * 
	 * @param artifact
	 *        The artifact
	 */
	protected void deployWar(Artifact artifact)
	{
		// wars go to tomcat/webapp - clear the old and expanded - war renamed without version
		File dir = new File(tomcat + "/webapps/" + artifact.getArtifactId());
		rmDir(dir);

		File to = new File(tomcat + "/webapps/" + artifact.getArtifactId() + ".war");
		copyFile(artifact.getFile(), to);
	}

	/**
	 * Delete this directory.
	 * 
	 * @param dir
	 *        The directory.
	 */
	protected void rmDir(File dir)
	{
		getLog().info("removing directory: " + dir.getAbsolutePath());
		try
		{
			FileUtils.deleteDirectory(dir);
		}
		catch (IOException e)
		{
			getLog().info("rmDir error: " + e.toString());
		}
	}
}
