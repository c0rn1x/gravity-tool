package org.gravity.eclipse.importer.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.gravity.eclipse.io.ExtensionFileVisitor;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * A class containing functionality for parsing pom files.
 *
 * @author speldszus
 *
 */
public class PomParser {

	private static final Logger LOGGER = Logger.getLogger(PomParser.class);

	private PomParser() {
		// This class shouldn't be instantiated
	}

	/**
	 * Discovers new libs from the given pom file
	 *
	 * @param pom       The pom file
	 * @param cacheFile A cache of already discovered libs
	 * @param results   A mapping between libs and their locations
	 * @param newLibs   The set of newly discovered libs
	 */
	public static void parsePomFile(File pom, File cacheFile, Map<String, Path> results, Set<String> newLibs) {
		try {
			final DocumentBuilder factory = createDocumentBuilder();
			final Document document = factory.parse(pom);
			document.getDocumentElement().normalize();
			newLibs.addAll(getDependencies(document, cacheFile, results));
		} catch (SAXException | IOException | ParserConfigurationException e) {
			LOGGER.warn(e);
		}
	}

	/**
	 * Creates a new document builder for parsing pom files
	 * 
	 * @return the builder
	 * @throws ParserConfigurationException
	 */
	public static DocumentBuilder createDocumentBuilder() throws ParserConfigurationException {
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		return factory.newDocumentBuilder();
	}

	/**
	 * Discovers new libs from the given pom file
	 * 
	 * @param pom       The pom file
	 * @param cacheFile A cache of already discovered libs
	 * @param results   A mapping between libs and their locations
	 * @return The by now undiscovered libs
	 * @throws IOException
	 * @throws IllegalAccessError
	 */
	static Set<String> getDependencies(final Document pom, File cacheFile, Map<String, Path> results)
			throws IOException, IllegalAccessError {
		Set<String> newLibs = new HashSet<>();
		final NodeList deps = pom.getElementsByTagName("dependency");
		for (int i = 0; i < deps.getLength(); i++) {
			final Set<String> libs = new HashSet<>();
			final String dependency = getDependency(deps.item(i));
			libs.add(dependency);
			final Map<String, Path> subResults = PomParser.searchInCache(libs, cacheFile);
			if (subResults.isEmpty()) {
				if (!results.containsKey(dependency)) {
					newLibs.add(dependency);
				}
			} else {
				results.putAll(subResults);
			}
		}
		return newLibs;
	}

	/**
	 * Creates a dependency string from a pom dependency node
	 *
	 * @param dependency The dependency node
	 * @return The string representation
	 */
	private static String getDependency(Node dependency) {
		String group = null;
		String artifact = null;
		String version = null;
		final NodeList childNodes = dependency.getChildNodes();
		for (int j = 0; j < childNodes.getLength(); j++) {
			final Node child = childNodes.item(j);
			final String nodeName = child.getNodeName();
			if ("groupId".equals(nodeName)) {
				group = child.getChildNodes().item(0).getNodeValue();
			} else if ("artifactId".equals(nodeName)) {
				artifact = child.getChildNodes().item(0).getNodeValue();
			} else if ("version".equals(nodeName)) {
				version = child.getChildNodes().item(0).getNodeValue();
			}
		}
		return group + ':' + artifact + ':' + version;
	}

	/**
	 * Searches the given libs in the maven cache
	 *
	 * @param libs      The names of the libs
	 * @param cacheFile The location of the maven cache
	 * @return A mapping to the binaries in the cache
	 * @throws IOException        if an I/O error is thrown by a visitor method
	 *                            walking through the cache location
	 * @throws IllegalAccessError If a library string doesn't has the expected
	 *                            amount of segments
	 */
	public static Map<String, Path> searchInCache(Set<String> libs, File cacheFile)
			throws IOException, IllegalAccessError {
		final Map<String, Path> results = new ConcurrentHashMap<>();
		final HashSet<String> newLibs = new HashSet<>();
		for (final String lib : libs) {
			final Library description = searchLibInCache(lib, cacheFile);
			if (description.getFile().exists()) {
				final ExtensionFileVisitor extensionFileVisitor = new ExtensionFileVisitor(Arrays.asList("jar", "aar"));
				Files.walkFileTree(description.getFile().toPath(), extensionFileVisitor);
				final List<Path> files = extensionFileVisitor.getFiles();
				if (!files.isEmpty()) {
					final Path libJar = getBestFit(description.getName(), description.getVersion(), files);
					results.put(lib, libJar);
					final File pom = libJar.getParent().resolve(description.getName() + ".pom").toFile();
					if (pom.exists()) {
						parsePomFile(pom, cacheFile, results, newLibs);
					}
				}
			}
			else {
				LOGGER.warn("Couldn't find lib file for \""+lib
						+ "\" tried: "+description.getFile());
			}
		}
		if (!newLibs.isEmpty()) {
			libs.addAll(newLibs);
		}
		return results;
	}

	/**
	 * Searches a lib in the cache
	 *
	 * @param lib   A string representing the lib
	 * @param cache The cache in which the lib should be searched
	 * @return The lib
	 */
	private static Library searchLibInCache(String lib, File cache) {
		final String[] segments = lib.split(":");
		File libFile = null;
		String version = null;
		String name = null;
		libFile = createNextFileSegment(cache, segments[0]);
		if (segments.length == 1) {
			name = segments[0];
		} else {
			name = segments[1];
			libFile = createNextFileSegment(libFile, segments[1]);
			if (segments.length > 2) {
				version = segments[2];
				if (segments.length > 3 && LOGGER.isEnabledFor(Level.WARN)) {
					LOGGER.warn("Unhandeled segments: " + lib);
				}
			}
		}
		return new Library(name, version, libFile);
	}

	/**
	 * Searches for the best fitting file based on the name and version
	 *
	 * @param name    The name of the lib
	 * @param version The desired version
	 * @param files   The available files
	 * @return The selected file
	 */
	private static Path getBestFit(String name, String version, List<Path> files) {
		Path libJar = null;
		for (final Path p : files) {
			final String string = p.getFileName().toString();
			final String fileVersion = string.substring(name.length() + 1, string.length() - 4);
			if (fileVersion.equals(version)) {
				libJar = p;
				break;
			}
		}
		if (libJar == null) {
			libJar = files.get(0);
		}
		return libJar;
	}

	/**
	 * Adds the segment to the location
	 *
	 * @param file The known parent
	 * @param next The next segment
	 * @return The resulting location
	 */
	private static File createNextFileSegment(File file, String next) {
		final File tmpLibFile = new File(file, next);
		if (tmpLibFile.exists()) {
			file = tmpLibFile;
		} else {
			file = new File(file, next.replace('.', '/'));
		}
		return file;
	}

	/**
	 * This class is used to store information about libs
	 *
	 * @author speldszus
	 *
	 */
	private static final class Library {

		private final File file;
		private final String version;
		private final String name;

		/**
		 * Creates a new final instance
		 *
		 * @param name    The name of the lib
		 * @param version The version of the lib
		 * @param file    The file containing the lib
		 */
		Library(String name, String version, File file) {
			this.name = name;
			this.version = version;
			this.file = file;
		}

		/**
		 * A getter for a file containing the lib
		 *
		 * @return the file
		 */
		public File getFile() {
			return this.file;
		}

		/**
		 * A getter for the version of the lib
		 *
		 * @return the version
		 */
		public String getVersion() {
			return this.version;
		}

		/**
		 * A getter for the name of the lib
		 *
		 * @return the name
		 */
		public String getName() {
			return this.name;
		}
	}
}
