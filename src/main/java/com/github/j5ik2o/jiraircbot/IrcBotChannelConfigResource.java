package com.github.j5ik2o.jiraircbot;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
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

@Path("/channelConfig")
public class IrcBotChannelConfigResource {
	private final UserManager userManager;
	private final PluginSettingsFactory pluginSettingsFactory;
	private final TransactionTemplate transactionTemplate;
	private static final Logger LOGGER = LoggerFactory
			.getLogger(ProjectServlet.class);

	public IrcBotChannelConfigResource(UserManager userManager,
			PluginSettingsFactory pluginSettingsFactory,
			TransactionTemplate transactionTemplate) {
		this.userManager = userManager;
		this.pluginSettingsFactory = pluginSettingsFactory;
		this.transactionTemplate = transactionTemplate;
	}

	@GET
	@Path("{projectId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response get(
			@PathParam("projectId") final String projectId,
			@Context HttpServletRequest request) {
		LOGGER.debug(String.format("get : start(%s,%s)",projectId,request));
		String username = userManager.getRemoteUsername(request);
		if (username != null
				&& !userManager.isSystemAdmin(username)) {
			Response response = Response.status(Status.UNAUTHORIZED).build();
			LOGGER.debug(String.format("get : finished(%s)",response));
			return response;
		}
		Response response = Response.ok(
				transactionTemplate
						.execute(new TransactionCallback() {
							public Object doInTransaction() {
								PluginSettings settings = pluginSettingsFactory
										.createGlobalSettings();
								IrcBotChannelConfig config = new IrcBotChannelConfig();
								boolean enable = Boolean
										.parseBoolean((String) settings
												.get(IrcBotChannelConfig.class
														.getName()
														+ "_"
														+ projectId
														+ ".enable"));
								config.setEnable(enable);

                                boolean notice = Boolean
                                        .parseBoolean((String) settings
                                                .get(IrcBotChannelConfig.class
                                                        .getName()
                                                        + "_"
                                                        + projectId
                                                        + ".notice"));
                                config.setNotice(notice);

								String channelName = (String) settings
										.get(IrcBotChannelConfig.class.getName()
												+ "_" + projectId + ".channelName");
								if (channelName != null) {
									config.setChannelName(channelName);
								}
								return config;
							}
						})).build();
		LOGGER.debug(String.format("get : finished(%s)",response));
		return response;
	}

	@PUT
	@Path("{projectId}")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response put(
			@PathParam("projectId") final String projectId,
			final IrcBotChannelConfig config,
			@Context HttpServletRequest request) {
		String username = userManager.getRemoteUsername(request);
		if (username != null
				&& !userManager.isSystemAdmin(username)) {
			return Response.status(Status.UNAUTHORIZED).build();
		}
		transactionTemplate.execute(new TransactionCallback() {
			public Object doInTransaction() {
				PluginSettings pluginSettings = pluginSettingsFactory
						.createGlobalSettings();
				pluginSettings.put(
						IrcBotChannelConfig.class.getName() + "_"
								+ projectId + ".enable", config.getEnable().toString());
                pluginSettings.put(
                        IrcBotChannelConfig.class.getName() + "_"
                                + projectId + ".notice", config.getNotice().toString());
				pluginSettings.put(
						IrcBotChannelConfig.class.getName() + "_"
								+ projectId + ".channelName",
						config.getChannelName());
				return null;
			}
		});

		return Response.noContent().build();
	}
}
