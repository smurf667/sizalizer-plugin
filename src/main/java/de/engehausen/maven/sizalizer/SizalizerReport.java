package de.engehausen.maven.sizalizer;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ServiceLoader;

import javax.imageio.ImageIO;

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.engehausen.treemap.IColorProvider;
import de.engehausen.treemap.ILabelProvider;
import de.engehausen.treemap.IRectangle;
import de.engehausen.treemap.IRectangleRenderer;
import de.engehausen.treemap.ITreeModel;
import de.engehausen.treemap.impl.CushionRectangleRenderer;
import de.engehausen.treemap.impl.LabelRenderer;
import de.engehausen.treemap.impl.SquarifiedLayout;
import de.engehausen.treemap.impl.StaticColorProvider;
import de.engehausen.treemap.impl.StaticTreeMap;
import de.engehausen.treemap.impl.TreeModel;

@Mojo(name = "sizalizer", defaultPhase = LifecyclePhase.SITE, requiresDependencyResolution = ResolutionScope.RUNTIME, requiresProject = true, threadSafe = true)
public class SizalizerReport extends AbstractMavenReport {

	private static final String TREEMAP_JS;
	private static final String CONFIG_JS;
	private static final String UNCOMPRESSED = "Uncompressed total size: ";
	private static final String CANVAS = "<div id=\"jstree\"><div>" + UNCOMPRESSED + "<span id=\"total\"></span></div><canvas id=\"treemap\" oncontextmenu=\"return false;\">Your browser does not support the &lt;canvas &gt; element. Sorry.</canvas><div id=\"treemap.info\"></div><div id=\"treemap.size\"></div></div>";
	private static final String STATIC = "<a href=\"#\" onclick=\"document.getElementById('static').style = 'display: block'; document.getElementById('jstree').style = 'display: none'; return false\">Static image of tree map...</a><img id=\"static\" src=\"sizalizer.png\" style=\"display: none\">";

	private static final Mapping[] DEFAULT_COLORS;
	private static final StaticImage DEFAULT_IMAGE = new StaticImage();

	static {
		TREEMAP_JS = read("/treemap.js");
		CONFIG_JS = read("/config.js");
		final List<Mapping> defaults = Arrays.asList(
			new Mapping("\\.class$", "#a00000"),
			new Mapping("\\.css$", "#00a000"),
			new Mapping("\\.jsp$", "#0000a0"),
			new Mapping("\\.json$", "#00f0a0"),
			new Mapping("\\.xml$", "#4040e0"),
			new Mapping("\\.jar$", "#20c040"),
			new Mapping("\\.properties$", "#606000")
		);
		DEFAULT_COLORS = defaults.toArray(new Mapping[defaults.size()]);
	}
	
	protected final Map<String, Analyzer> analyzers;

	@Parameter
	protected Mapping[] mappings;

	@Parameter
	protected StaticImage staticImage;

	public SizalizerReport() {
		super();
		analyzers = new HashMap<>();
		final ServiceLoader<Analyzer> loader = ServiceLoader.load(Analyzer.class);
		for (final Analyzer analyzer : loader) {
			analyzer
				.supportedPackagings()
				.forEach( packaging -> analyzers.put(packaging, analyzer) );
		}
	}

	private static String read(final String resource) {
		try (final InputStream is = SizalizerReport.class.getResourceAsStream(resource)) {
			final ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
			final byte[] buffer = new byte[512];
			while (is.available() > 0) {
				final int size = is.read(buffer);
				if (size > 0) {
					out.write(buffer, 0, size);
				}
			}
			return new String(out.toByteArray(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
	
	@Override
	public String getName(final Locale locale) {
		return "Size Analysis";
	}

	@Override
	public String getDescription(final Locale locale) {
		return "Size analysis of build artifact";
	}

	@Override
	public String getOutputName() {
		return "sizalizer";
	}

	@Override
	protected void executeReport(final Locale locale) throws MavenReportException {
		final Log logger = getLog();

		final boolean isPDF = getSink().getClass().getName().contains("PdfMojo"); // what a hack!

		logger.info("Generating " + getOutputName() + (isPDF ? ".xml" : ".html") + " for " + project.getName() + " " + project.getVersion());

		final Analyzer analyzer = analyzers.get(project.getPackaging());
		if (analyzer == null) {
			logger.error(project.getPackaging() + " not supported");
			return;
		}

		final Sink mainSink = getSink();
		if (mainSink == null) {
			throw new MavenReportException("Could not get the Doxia sink");
		}

		final Node root = analyzer.analyze(project, getLog());

		if (staticImage == null) {
			staticImage = DEFAULT_IMAGE;
		}
		if (mappings == null) {
			mappings = DEFAULT_COLORS;
		}
		if (isPDF) {
			staticImage.only = true;
		}
		renderImage(root, staticImage);

		final String title = "Size Analysis for " + project.getName() + " " + project.getVersion();
		mainSink.head();
		mainSink.title();
		mainSink.text(title);
		mainSink.title_();
		mainSink.head_();

		mainSink.body();

		mainSink.section1();
		mainSink.sectionTitle1();
		mainSink.text(title);
		mainSink.sectionTitle1_();

		if (staticImage.only) {
			mainSink.figure();
			mainSink.figureGraphics(isPDF ? "../site/sizalizer.png" : "sizalizer.png" );
			mainSink.figureCaption();
			mainSink.text("tree map of contents");
			mainSink.figureCaption_();
			mainSink.figure_();
		} else {
			mainSink.rawText(CANVAS);
			mainSink.rawText(TREEMAP_JS);
			mainSink.rawText(TREEMAP_JS);
			mainSink.rawText(writeConfig(root));
			mainSink.rawText(STATIC);
		}

		mainSink.section1_();
		mainSink.body_();
	}

	private String writeConfig(final Node treeModel) throws MavenReportException {
		final StringBuilder colors = new StringBuilder(256);
		for (final Mapping mapping : mappings) {
			colors.append(colors.length() == 0 ? "if (/" : "} else if (/");
			colors.append(mapping.match)
				.append("/.test(node.label)) { result = \"")
				.append(mapping.rgb)
				.append("\";");
		}
		if (colors.length() > 0) {
			colors.append("}");
		}
		try {
			return CONFIG_JS
				.replace("@COLORS@", colors.toString())
				.replace("@TREEMODEL@", new ObjectMapper().writeValueAsString(treeModel));
		} catch (JsonProcessingException e) {
			throw new MavenReportException("Cannot write tree model", e);
		}
	}

	protected void renderImage(final Node root, final StaticImage info) {
		final TreeModel treeModel = new TreeModel(root);
		final StaticTreeMap treeMap = new StaticTreeMap();
		treeMap.setTreeMapLayout(new SquarifiedLayout<Node>(info.depth));
		treeMap.setLabelProvider((model, rectangle) -> ((IRectangle<Node>) rectangle).getNode().label );
		treeMap.setColorProvider(new StaticColorProvider(mappings));
		final Font font = new Font("SansSerif", Font.BOLD, info.fontSize);
		final IRectangleRenderer<Node, Graphics2D, Color> delegate = new LabelRenderer<Node>(font);
		treeMap.setRectangleRenderer(new CushionRectangleRenderer<Node>(128) {
			@Override
			public void render(final Graphics2D graphics,
					final ITreeModel<IRectangle<Node>> model,
					final IRectangle<Node> rectangle,
					final IColorProvider<Node, Color> colorProvider,
					final ILabelProvider<Node> labelProvider) {
				super.render(graphics, model, rectangle, colorProvider, labelProvider);
				delegate.render(graphics, model, rectangle, colorProvider, labelProvider);
			}
		});
		treeMap.setTreeModel(treeModel);
		final BufferedImage result = treeMap.build(info.width, info.height);
		if (result != null) {
			renderTotalSize(result, font, treeModel.getRoot().weight);
			try {
				final Path path = Paths.get(getOutputDirectory(), "sizalizer.png");
				ImageIO.write(result, "png", new FileOutputStream(path.toFile()));
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}
	}

	private void renderTotalSize(final BufferedImage image, final Font font, final long size) {
		final Graphics2D g2d = image.createGraphics();
		try {
			g2d.setFont(font);
			g2d.setColor(Color.WHITE);
			g2d.drawString(UNCOMPRESSED + size, 2 * font.getSize(), 2 * font.getSize());
		} finally {
			g2d.dispose();
		}
	}

}
