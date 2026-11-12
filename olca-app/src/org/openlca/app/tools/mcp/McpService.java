package org.openlca.app.tools.mcp;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.openlca.app.tools.mcp.tools.DataSync;
import org.openlca.app.tools.mcp.tools.McpTools;
import org.openlca.app.tools.mcp.tools.ToolArgs;
import org.openlca.app.tools.mcp.tools.ToolResults;
import org.openlca.core.database.IDatabase;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.json.schema.jackson3.DefaultJsonSchemaValidator;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.DefaultServerTransportSecurityValidator;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import tools.jackson.databind.json.JsonMapper;

/// The openLCA MCP server. It runs an embedded Jetty server with the
/// streamable HTTP transport of the MCP Java SDK and exposes the tools of
/// [McpTools] for the given database.
///
/// There is at most one running server in the application, which is managed
/// via the static methods of this class. The server is only bound to the
/// loopback interface and requests with foreign `Origin` or `Host` headers are
/// rejected (protection against DNS rebinding attacks).
public final class McpService {

	public static final String ENDPOINT = "/mcp";
	private static final int MAX_HISTORY = 200;

	private static McpService current;

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final IDatabase db;
	private final int port;
	private final Server jetty;
	private final McpSyncServer mcp;

	private final ArrayDeque<ToolCall> history = new ArrayDeque<>();
	private final List<Consumer<ToolCall>> listeners = new CopyOnWriteArrayList<>();

	/// Information about a tool call, e.g. for displaying the activity of
	/// agents in the UI.
	public record ToolCall(
		Instant time, String tool, boolean isError, long millis) {
	}

	private McpService(IDatabase db, int port, DataSync sync) {
		this.db = db;
		this.port = port;

		// we set the JSON mapper and schema validator explicitly, because the
		// service-loader based discovery of the SDK does not work reliably in
		// an OSGi environment
		var jackson = JsonMapper.builder().build();
		McpJsonMapper jsonMapper = new JacksonMcpJsonMapper(jackson);

		// load the tools first, as this fails on invalid tool definitions, and
		// the transport starts background threads when it is created
		var specs = McpTools.of(db, sync, jsonMapper).stream()
			.map(this::specOf)
			.toList();
		var instructions = instructions();

		var security = DefaultServerTransportSecurityValidator.builder()
			.allowedOrigins(List.of(
				"http://localhost:*",
				"http://127.0.0.1:*"))
			.allowedHosts(List.of(
				"localhost:*",
				"127.0.0.1:*"))
			.build();

		var transport = HttpServletStreamableServerTransportProvider.builder()
			.jsonMapper(jsonMapper)
			.mcpEndpoint(ENDPOINT)
			.httpHeaderValidator(security)
			// the session sweeper of the SDK (2.1.0) runs on a non-daemon
			// thread that is not stopped when the transport is closed; for a
			// local server, we do not need it: all sessions are closed when
			// the server is stopped
			.sessionSweepInterval(null)
			.build();

		mcp = McpServer.sync(transport)
			.serverInfo("openLCA", versionOf())
			.instructions(instructions)
			.jsonMapper(jsonMapper)
			.jsonSchemaValidator(new DefaultJsonSchemaValidator(jackson))
			.capabilities(McpSchema.ServerCapabilities.builder()
				.tools(true)
				.build())
			.tools(specs)
			.build();

		jetty = jettyOf(transport, port);
	}

	private static String versionOf() {
		var bundle = FrameworkUtil.getBundle(McpService.class);
		return bundle != null
			? bundle.getVersion().toString()
			: "dev";
	}

	private static Server jettyOf(
		HttpServletStreamableServerTransportProvider transport, int port) {

		// daemon threads, so that a running server never blocks the shutdown
		// of the application
		var threads = new QueuedThreadPool();
		threads.setName("olca-mcp");
		threads.setDaemon(true);

		var server = new Server(threads);
		var connector = new ServerConnector(server);
		connector.setHost("127.0.0.1");
		connector.setPort(port);
		server.addConnector(connector);

		var context = new ServletContextHandler();
		context.setContextPath("/");
		var servlet = new ServletHolder(transport);
		servlet.setAsyncSupported(true);
		context.addServlet(servlet, "/*");
		server.setHandler(context);
		server.setStopTimeout(2000);
		return server;
	}

	/// Returns the general instructions that are sent to the agent when it
	/// connects to the server. They describe the data model and the typical
	/// workflow and are stored in the resource file `instructions.md`.
	private static String instructions() {
		try (var stream = McpService.class.getResourceAsStream("instructions.md")) {
			if (stream == null)
				throw new IllegalStateException(
					"MCP resource not found: instructions.md");
			return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new UncheckedIOException(
				"failed to read MCP resource: instructions.md", e);
		}
	}

	private SyncToolSpecification specOf(McpTools.Spec spec) {
		return SyncToolSpecification.builder()
			.tool(spec.definition())
			.callHandler((_, req) -> call(spec, req))
			.build();
	}

	private CallToolResult call(McpTools.Spec spec, CallToolRequest req) {
		var tool = spec.tool();
		long start = System.currentTimeMillis();
		CallToolResult result;
		try {
			result = tool.call(new ToolArgs(req.arguments()));
		} catch (Exception e) {
			log.error("MCP tool call {} failed", tool.name(), e);
			result = ToolResults.error(
				"tool call failed: " + e.getClass().getSimpleName()
					+ ": " + e.getMessage());
		}
		var isError = result.isError() != null && result.isError();
		var call = new ToolCall(Instant.ofEpochMilli(start), tool.name(),
			isError, System.currentTimeMillis() - start);
		log.trace("MCP tool call {}", call);
		synchronized (history) {
			history.addLast(call);
			if (history.size() > MAX_HISTORY) {
				history.removeFirst();
			}
		}
		for (var listener : listeners) {
			listener.accept(call);
		}
		return result;
	}

	/// Starts a new MCP server for the given database and port. Throws an
	/// exception if a server is already running or the server could not be
	/// started.
	public static synchronized McpService start(
		IDatabase db, int port, DataSync sync
	) throws Exception {
		if (current != null)
			throw new IllegalStateException(
				"an MCP server is already running on port " + current.port);
		var service = new McpService(db, port, sync);
		try {
			service.jetty.start();
		} catch (Exception e) {
			service.dispose();
			throw e;
		}
		service.log.info("started MCP server at {}", service.url());
		current = service;
		return service;
	}

	/// Returns the currently running server, if any.
	public static synchronized Optional<McpService> current() {
		return Optional.ofNullable(current);
	}

	/// Stops the currently running server, if any.
	public static synchronized void stopCurrent() {
		if (current == null)
			return;
		var service = current;
		current = null;
		service.dispose();
		service.log.info("stopped MCP server at {}", service.url());
	}

	/// Stops the currently running server if it is connected to the given
	/// database, e.g. when this database is closed.
	public static synchronized void stopIfConnectedTo(IDatabase db) {
		if (current != null && current.db == db) {
			stopCurrent();
		}
	}

	private void dispose() {
		try {
			mcp.closeGracefully();
		} catch (Exception e) {
			log.warn("failed to close MCP sessions", e);
		}
		try {
			jetty.stop();
		} catch (Exception e) {
			log.error("failed to stop Jetty server of MCP service", e);
		}
	}

	public IDatabase db() {
		return db;
	}

	public int port() {
		return port;
	}

	public String url() {
		return "http://127.0.0.1:" + port + ENDPOINT;
	}

	/// Returns the most recent tool calls, oldest first.
	public List<ToolCall> history() {
		synchronized (history) {
			return new ArrayList<>(history);
		}
	}

	public void addListener(Consumer<ToolCall> listener) {
		if (listener != null) {
			listeners.add(listener);
		}
	}

	public void removeListener(Consumer<ToolCall> listener) {
		listeners.remove(listener);
	}
}
