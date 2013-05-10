package com.github.j5ik2o.jiraircbot;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.atlassian.sal.api.user.UserManager;

@Path("/globalConfig")
public class IrcBotGlobalConfigResource {
	private final UserManager userManager;
	private final PluginSettingsFactory pluginSettingsFactory;
	private final TransactionTemplate transactionTemplate;
	private static final Logger LOGGER = LoggerFactory
			.getLogger(IrcBotGlobalConfigResource.class);

	public IrcBotGlobalConfigResource(UserManager userManager,
			PluginSettingsFactory pluginSettingsFactory,
			TransactionTemplate transactionTemplate) {
		this.userManager = userManager;
		this.pluginSettingsFactory = pluginSettingsFactory;
		this.transactionTemplate = transactionTemplate;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response get(@Context HttpServletRequest request) {
		LOGGER.debug(String.format("get : start(%s)", request));
		String username = userManager.getRemoteUsername(request);
		if (username != null && !userManager.isSystemAdmin(username)) {
			LOGGER.debug(String.format("get : finished(%s)", request));
			return Response.status(Status.UNAUTHORIZED).build();
		}
		Response result = Response.ok(
				transactionTemplate.execute(new TransactionCallback() {
					@Override
					public Object doInTransaction() {
						PluginSettings settings = pluginSettingsFactory
								.createGlobalSettings();
						IrcBotGlobalConfig config = new IrcBotGlobalConfig();
						boolean enable = Boolean.parseBoolean((String) settings
								.get(IrcBotGlobalConfig.class.getName()
										+ ".enable"));
						config.setEnable(enable);

						config.setIrcServerName((String) settings
								.get(IrcBotGlobalConfig.class.getName()
										+ ".ircServerName"));

						String ircServerPort = (String) settings
								.get(IrcBotGlobalConfig.class.getName()
										+ ".ircServerPort");
						if (ircServerPort != null) {
							config.setIrcServerPort(Integer
									.parseInt(ircServerPort));
						}
						
						config.setIrcEncoding((String) settings
								.get(IrcBotGlobalConfig.class.getName()
										+ ".ircEncoding"));
						return config;
					}
				})).build();
		LOGGER.debug(String.format("get : finished(%s)", result));
		return result;
	}

	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	public Response put(final IrcBotGlobalConfig config,
			@Context HttpServletRequest request) {
		String username = userManager.getRemoteUsername(request);
		if (username != null && !userManager.isSystemAdmin(username)) {
			return Response.status(Status.UNAUTHORIZED).build();
		}

		transactionTemplate.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction() {
				PluginSettings pluginSettings = pluginSettingsFactory
						.createGlobalSettings();
				pluginSettings.put(IrcBotGlobalConfig.class.getName()
						+ ".enable", config.getEnable().toString());
				pluginSettings.put(IrcBotGlobalConfig.class.getName()
						+ ".ircServerName", config.getIrcServerName());
				pluginSettings.put(IrcBotGlobalConfig.class.getName()
						+ ".ircServerPort",
						Integer.toString(config.getIrcServerPort()));
				pluginSettings.put(IrcBotGlobalConfig.class.getName()
						+ ".ircEncoding", config.getIrcEncoding());
				return null;
			}
		});

		return Response.noContent().build();
	}
}