/** 
 * SonarQube Xanitizer Plugin
 * Copyright 2012-2016 by RIGS IT GmbH, Switzerland, www.rigs-it.ch.
 * mailto: info@rigs-it.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Created on 25.07.2016
 *
 */
package com.rigsit.xanitizer.sqplugin.test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputPath;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssueLocation;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.utils.PathUtils;
import org.sonar.plugins.java.api.JavaResourceLocator;

import com.rigsit.xanitizer.sqplugin.XanitizerRulesDefinition;
import com.rigsit.xanitizer.sqplugin.XanitizerSensor;
import com.rigsit.xanitizer.sqplugin.XanitizerSonarQubePlugin;
import com.rigsit.xanitizer.sqplugin.metrics.GeneratedProblemType;

/**
 * @author nwe
 *
 */
public class SensorTest {
	
	private  final File resourcesDirectory = new File("src/test/resources");
	private final SensorContext context = mock(SensorContext.class);

	@Before
	public void setup() {
		final File webgoatDir = new File(resourcesDirectory, "webgoat");

		final FileSystem fileSystem = prepareFileSystem(webgoatDir);
		
		when(context.fileSystem()).thenReturn(fileSystem);
	}

	@Test
	public void testShouldExecuteOnProject() {

		/*
		 * no active Xanitizer rules
		 */
		{
			final Settings settings = new Settings();
			final String reportFile = new File (resourcesDirectory, "webgoat/webgoat-Findings-List-all.xml").getAbsolutePath();
			settings.setProperty(XanitizerSonarQubePlugin.XAN_XML_REPORT_FILE, reportFile);
			final XanitizerSensor sensor = new XanitizerSensor(mock(JavaResourceLocator.class), settings,
					mock(ActiveRules.class), context);
			assertEquals(false, sensor.shouldExecuteOnProject(mock(Project.class)));
		}

		/*
		 * Missing file, absolute path
		 */
		{
			final Settings settings = new Settings();
			final String reportFile = new File (resourcesDirectory, "webgoat/missing.xml").getAbsolutePath();
			settings.setProperty(XanitizerSonarQubePlugin.XAN_XML_REPORT_FILE, reportFile);
			final XanitizerSensor sensor = new XanitizerSensor(mock(JavaResourceLocator.class), settings,
					getActiveRules(), context);
			assertEquals(false, sensor.shouldExecuteOnProject(mock(Project.class)));
		}
		
		/*
		 * Missing file
		 */
		{
			final Settings settings = new Settings();
			final String reportFile = "webgoat/missing.xml";
			settings.setProperty(XanitizerSonarQubePlugin.XAN_XML_REPORT_FILE, reportFile);
			final XanitizerSensor sensor = new XanitizerSensor(mock(JavaResourceLocator.class), settings,
					getActiveRules(), context);
			assertEquals(false, sensor.shouldExecuteOnProject(mock(Project.class)));
		}

		/*
		 * Everything correct, absolute path
		 */
		{
			final Settings settings = new Settings();
			final String reportFile = new File (resourcesDirectory, "webgoat/webgoat-Findings-List-all.xml").getAbsolutePath();
			settings.setProperty(XanitizerSonarQubePlugin.XAN_XML_REPORT_FILE, reportFile);
			final XanitizerSensor sensor = new XanitizerSensor(mock(JavaResourceLocator.class), settings,
					getActiveRules(), context);
			assertEquals(true, sensor.shouldExecuteOnProject(mock(Project.class)));
		}
		
		/*
		 * Everything correct, relative to base dir
		 */
		{
			final Settings settings = new Settings();
			final String reportFileName = "WEB-INF" + File.separator + "nested-Findings-List.xml";
			settings.setProperty(XanitizerSonarQubePlugin.XAN_XML_REPORT_FILE, reportFileName);
			final XanitizerSensor sensor = new XanitizerSensor(mock(JavaResourceLocator.class), settings,
					getActiveRules(), context);
			assertEquals(true, sensor.shouldExecuteOnProject(mock(Project.class)));
		}
		
		{
			final Settings settings = new Settings();
			final String reportFileName = "." + File.separator + "WEB-INF" + File.separator + "nested-Findings-List.xml";
			settings.setProperty(XanitizerSonarQubePlugin.XAN_XML_REPORT_FILE, reportFileName);
			final XanitizerSensor sensor = new XanitizerSensor(mock(JavaResourceLocator.class), settings,
					getActiveRules(), context);
			assertEquals(true, sensor.shouldExecuteOnProject(mock(Project.class)));
		}
		
		/*
		 * Everything correct, nested file
		 */
		{
			final Settings settings = new Settings();
			final String reportFileName = "nested-Findings-List.xml";
			settings.setProperty(XanitizerSonarQubePlugin.XAN_XML_REPORT_FILE, reportFileName);
			final XanitizerSensor sensor = new XanitizerSensor(mock(JavaResourceLocator.class), settings,
					getActiveRules(), context);
			assertEquals(true, sensor.shouldExecuteOnProject(mock(Project.class)));
		}

	}

	@Test
	public void testAnalyze() {
		final Settings settings = new Settings();
		final String reportFile = new File (resourcesDirectory, "webgoat/webgoat-Findings-List-all.xml").getAbsolutePath();
		settings.setProperty(XanitizerSonarQubePlugin.XAN_XML_REPORT_FILE, reportFile);
		

		final int[] createdIssues = { 0 };
		when(context.newIssue()).thenAnswer(new Answer<NewIssue>() {
			@Override
			public NewIssue answer(final InvocationOnMock invocation) throws Throwable {
				createdIssues[0]++;

				final NewIssue newIssue = mock(NewIssue.class);
				when(newIssue.newLocation()).then(new Answer<NewIssueLocation>() {

					@Override
					public NewIssueLocation answer(InvocationOnMock invocation) throws Throwable {
						return new DefaultIssueLocation();
					}

				});
				return newIssue;
			}
		});
		
		when(context.getResource(Mockito.any(InputPath.class))).thenAnswer(new Answer<Resource>() {

			@Override
			public Resource answer(InvocationOnMock invocation) throws Throwable {
				final InputPath path = invocation.getArgumentAt(0, InputPath.class);
				return new org.sonar.api.resources.File(path.relativePath());
			}

		});

		final Project project = mock(Project.class);

		final XanitizerSensor sensor = new XanitizerSensor(mock(JavaResourceLocator.class), settings, getActiveRules(),
				context);

		sensor.analyse(project, context);
		assertEquals(180, createdIssues[0]);
	}

	@Test
	public void testSingleActiveRule() {
		final Settings settings = new Settings();
		final String reportFile = new File (resourcesDirectory, "webgoat/webgoat-Findings-List-all.xml").getAbsolutePath();
		settings.setProperty(XanitizerSonarQubePlugin.XAN_XML_REPORT_FILE, reportFile);

		final int[] createdIssues = { 0 };
		when(context.newIssue()).thenAnswer(new Answer<NewIssue>() {
			@Override
			public NewIssue answer(final InvocationOnMock invocation) throws Throwable {
				createdIssues[0]++;

				final NewIssue newIssue = mock(NewIssue.class);
				when(newIssue.newLocation()).then(new Answer<NewIssueLocation>() {

					@Override
					public NewIssueLocation answer(InvocationOnMock invocation) throws Throwable {
						return new DefaultIssueLocation();
					}

				});
				return newIssue;
			}
		});

		final Project project = mock(Project.class);

		final ActiveRulesBuilder builder = new ActiveRulesBuilder();
		for (GeneratedProblemType problemType : GeneratedProblemType.values()) {
			final RuleKey ruleKey = RuleKey.of(XanitizerRulesDefinition.REPOSITORY_KEY, problemType.name());
			final NewActiveRule activeRule = builder.create(ruleKey);
			if (problemType.getPresentationName().equals("SQL Injection")) {
				activeRule.activate();
			}
		}

		final XanitizerSensor sensor = new XanitizerSensor(mock(JavaResourceLocator.class), settings, builder.build(),
				context);

		sensor.analyse(project, context);
		assertEquals(26, createdIssues[0]);
	}

	private FileSystem prepareFileSystem(final File rootDir) {
		final DefaultFileSystem fileSystem = new DefaultFileSystem(rootDir);

		try {
			Files.walkFileTree(Paths.get(rootDir.getAbsolutePath()), new FileVisitor<Path>() {

				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					final String relative = PathUtils.sanitize(new PathResolver().relativePath(rootDir, file.toFile()));
					fileSystem.add(new DefaultInputFile("test", relative));
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return fileSystem;
	}

	@Test
	public void testAnalyzeOldReportFile() {
		final Settings settings = new Settings();
		final String reportFile = new File (resourcesDirectory, "webgoat/webgoat-Findings-List-oldversion.xml").getAbsolutePath();
		settings.setProperty(XanitizerSonarQubePlugin.XAN_XML_REPORT_FILE, reportFile);

		final int[] createdIssues = { 0 };

		final XanitizerSensor sensor = new XanitizerSensor(mock(JavaResourceLocator.class), settings, getActiveRules(),
				context);
		sensor.analyse(mock(Project.class), context);
		assertEquals(0, createdIssues[0]);
	}

	@Test
	public void testReportFileWOAnalysis() {
		final Settings settings = new Settings();
		final String reportFile = new File (resourcesDirectory, "webgoat/empty-Findings-List.xml").getAbsolutePath();
		settings.setProperty(XanitizerSonarQubePlugin.XAN_XML_REPORT_FILE, reportFile);

		final int[] createdIssues = { 0 };

		final XanitizerSensor sensor = new XanitizerSensor(mock(JavaResourceLocator.class), settings, getActiveRules(),
				context);
		sensor.analyse(mock(Project.class), context);
		assertEquals(0, createdIssues[0]);
	}

	private ActiveRules getActiveRules() {
		final ActiveRulesBuilder builder = new ActiveRulesBuilder();
		for (GeneratedProblemType problemType : GeneratedProblemType.values()) {
			final RuleKey ruleKey = RuleKey.of(XanitizerRulesDefinition.REPOSITORY_KEY, problemType.name());
			final NewActiveRule activeRule = builder.create(ruleKey);
			activeRule.activate();
		}
		return builder.build();
	}
}
