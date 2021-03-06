package org.gravity.eclipse.tests;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import org.eclipse.core.runtime.CoreException;
import org.gravity.eclipse.importer.ImportException;
import org.gravity.eclipse.importer.maven.MavenImport;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class MavenImportTest extends ImportTest {

	public MavenImportTest(String name, File projectLocation, Map<String, String> expected) throws ImportException {
		super(new MavenImport(projectLocation, false), name, projectLocation, expected);
	}

	@Parameters(name = "{0}")
	public static Collection<Object[]> getTestProjects() throws CoreException, IOException {
		return getTestProjects(".mvn.csv");
	}
}
