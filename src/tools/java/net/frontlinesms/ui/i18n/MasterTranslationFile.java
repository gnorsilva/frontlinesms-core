/**
 * 
 */
package net.frontlinesms.ui.i18n;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.frontlinesms.plugins.PluginController;
import net.frontlinesms.plugins.PluginProperties;

/**
 * This creates a master translation file, containing default translation for all available plugins.
 * @author Alex alex@frontlinesms.com
 */
public class MasterTranslationFile {
	private List<TextFileContent> translationFiles;
	
	/**
	 * Initialise a {@link MasterTranslationFile} for a language other than the default.
	 * The core translation will be loaded from the {@link FileLanguageBundle} provided, and plugins
	 * will have their translations fetched individually.
	 */
	private void init(FileLanguageBundle languageBundle) {
		try {
			initCoreLanguageBundle(new FileInputStream(languageBundle.getFile()));
		} catch (IOException ex) {
			throw new RuntimeException("Failed to load language bundle from file: " + languageBundle.getFile().getAbsolutePath(), ex);
		}
		
		// load localised bundles for all plugins
		Collection<Class<PluginController>> pluginClasses = PluginProperties.getInstance().getPluginClasses();
		for(Class<PluginController> pluginClass : pluginClasses) {
			try {
				PluginController controller = pluginClass.newInstance();
				this.translationFiles.add(TextFileContent.getFromMap(
						"Plugin: " + controller.getName(),
						controller.getTextResource(languageBundle.getLocale())));
			} catch (Exception ex) {
				throw new RuntimeException("Unable to instantiate plugin: " + pluginClass.getName(), ex);
			}
		}		
	}
	
	/** Initialise a {@link MasterTranslationFile} for the default language. */
	private void initDefault() {
		initCoreLanguageBundle(InternationalisationUtils.getDefaultLanguageBundleInputStream());
		
		// load default bundles for all plugins
		Collection<Class<PluginController>> pluginClasses = PluginProperties.getInstance().getPluginClasses();
		for(Class<PluginController> pluginClass : pluginClasses) {
			try {
				PluginController controller = pluginClass.newInstance();
				this.translationFiles.add(TextFileContent.getFromMap(
						"Plugin: " + controller.getName(),
						controller.getDefaultTextResource()));
			} catch (Exception ex) {
				throw new RuntimeException("Unable to instantiate plugin: " + pluginClass.getName(), ex);
			}
		}
	}
	
	/**
	 * Initialise a {@link MasterTranslationFile} from the supplied {@link InputStream}.  The core translations
	 * will be loaded from the {@link InputStream}.  Plugin translations will not be loaded.
	 * @param is input stream to the core language bundle
	 */
	private void initCoreLanguageBundle(InputStream is) {
		assert(this.translationFiles != null) : "init() should be run only once on this class.";
		this.translationFiles = new LinkedList<TextFileContent>();
		
		// load the default, english bundle
		this.translationFiles.add(TextFileContent.getFromStream("FrontlineSMS Core", is));
	}

	/** Save the MTF to a file 
	 * @throws IOException */
	private void saveToFile(File file) throws IOException {
		FileOutputStream fos = null;
		OutputStreamWriter osw = null;
		PrintWriter out = null;
		try {
			fos = new FileOutputStream(file);
			osw = new OutputStreamWriter(fos, InternationalisationUtils.CHARSET_UTF8);
			out = new PrintWriter(osw);
			for(TextFileContent translationFile : this.translationFiles) {
				out.write("###\n");
				out.write("# " + translationFile.getDescription() + "\n");
				for(String line : translationFile.getLines()) {
					out.write(line + "\n");
				}
			}
			out.write("\n");
		} finally {
			if(out != null) out.close();
			if(osw != null) try { osw.close(); } catch(IOException ex) {}
			if(fos != null) try { fos.close(); } catch(IOException ex) {}
		}
	}
	
	/**
	 * Create a {@link MasterTranslationFile} and save it locally.
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		System.out.println("Starting Master Language File generation...");
		String targetDirPath = args[0];
		File targetDir = new File(targetDirPath);
		
		processDefaultMtf(targetDir);
		
		for(FileLanguageBundle languageBundle : InternationalisationUtils.getLanguageBundles()) {
			MasterTranslationFile mtf = new MasterTranslationFile();
			mtf.init(languageBundle);
			mtf.saveToFile(new File(targetDir, languageBundle.getFile().getName()));	
		}
		
		System.out.println("Master language files generated at: " + targetDirPath);
	}

	/** Create the {@link MasterTranslationFile} for the default translation. */
	private static void processDefaultMtf(File targetDir) throws IOException {
		MasterTranslationFile mtf = new MasterTranslationFile();
		mtf.initDefault();
		mtf.saveToFile(new File(targetDir, "frontlineSMS.properties"));
	}
}

class TextFileContent {
	/** Description of this file */
	private String description;
	/** Lines in the file */
	private final LinkedList<String> lines = new LinkedList<String>();
	
	private TextFileContent(String description) {
		this.description = description;
	}
	
	public String getDescription() {
		return description;
	}
	
	public LinkedList<String> getLines() {
		return lines;
	}
	
	private void addLine(String line) {
		this.lines.add(line);
	}
	
	static TextFileContent getFromMap(String description, Map<String, String> map) {
		TextFileContent content = new TextFileContent(description);
		for(Entry<String, String> entry : map.entrySet()) {
			content.addLine(entry.getKey() + "=" + entry.getValue());
		}
		return content;
	}
	
	static TextFileContent getFromStream(String description, InputStream is) {
		BufferedReader in = null;
		TextFileContent content = new TextFileContent(description);
		try {
			in = new BufferedReader(new InputStreamReader(is, InternationalisationUtils.CHARSET_UTF8));
			String line;
			while((line = in.readLine()) != null) {
				content.addLine(line);
			}
			return content;
		} catch (IOException ex) {
			throw new IllegalStateException("Unhandled problem reading stream: '" + description + "'", ex);
		} finally {
			if(in != null) try { in.close(); } catch(IOException ex) {}
		}
	}
}