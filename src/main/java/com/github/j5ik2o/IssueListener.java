package com.github.j5ik2o;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.jibble.pircbot.Colors;
import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.NickAlreadyInUseException;
import org.jibble.pircbot.PircBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.event.type.EventType;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.util.velocity.VelocityRequestContextFactory;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

/**
 * Simple JIRA listener using the atlassian-event library and demonstrating
 * plugin lifecycle integration.
 */
public class IssueListener implements InitializingBean,
		DisposableBean {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(IssueListener.class);

	private final EventPublisher eventPublisher;

	private final PluginSettingsFactory pluginSettingsFactory;

	private PircBot pircBot = new PircBot() {
		{
			setName("jira-irc-bot");
		}
	};

	private final VelocityRequestContextFactory velocityRequestContextFactory;

	private final ProjectManager projectManager;

	/**
	 * Constructor.
	 * 
	 * @param eventPublisher
	 *          injected {@code EventPublisher} implementation.
	 */
	public IssueListener(
			EventPublisher eventPublisher,
			PluginSettingsFactory pluginSettingsFactory,
			VelocityRequestContextFactory velocityRequestContextFactory,
			ProjectManager projectManager) {
		this.eventPublisher = eventPublisher;
		this.pluginSettingsFactory = pluginSettingsFactory;
		this.velocityRequestContextFactory = velocityRequestContextFactory;
		this.projectManager = projectManager;
	}

	/**
	 * Called when the plugin has been enabled.
	 * 
	 * @throws Exception
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		// register ourselves with the EventPublisher
		eventPublisher.register(this);
	}

	/**
	 * Called when the plugin is being disabled or removed.
	 * 
	 * @throws Exception
	 */
	@Override
	public void destroy() throws Exception {
		// unregister ourselves with the EventPublisher
		if (pircBot.isConnected()) {
			pircBot.disconnect();
		}
		eventPublisher.unregister(this);
	}

	/**
	 * Receives any {@code IssueEvent}s sent by JIRA.
	 * 
	 * @param issueEvent
	 *          the IssueEvent passed to us
	 */
	@EventListener
	public synchronized void onIssueEvent(IssueEvent issueEvent) {
		Project project = issueEvent.getIssue()
				.getProjectObject();
		String projectId = project.getId().toString();
		PluginSettings settings = pluginSettingsFactory
				.createGlobalSettings();

		if (isIrcBotEnable(settings) == false
				|| isIrcBotChannelEnable(settings, projectId) == false) {
			return;
		}

		try {
			autoConnect();

			String channelName = getChannelName(settings, projectId);
			LOGGER.debug(String.format("channelName = %s",
					channelName));

			if (Arrays.asList(pircBot.getChannels()).contains(
					channelName) == false) {
				LOGGER.info(String.format("join channel (%s)",
						channelName));
				pircBot.joinChannel(channelName);
			}

			Long eventTypeId = issueEvent.getEventTypeId();
			Issue issue = issueEvent.getIssue();

			String issueTypeName = issue.getIssueTypeObject()
					.getNameTranslation();

			String url = String.format("%s/browse/%s",
					velocityRequestContextFactory
							.getJiraVelocityRequestContext()
							.getCanonicalBaseUrl(), issue.getKey());
			String issueKey = issue.getKey();
			String issueSummary = issue.getSummary();
			String userDisplayName = issueEvent.getUser()
					.getDisplayName();
			String userName = issueEvent.getUser().getName();
			boolean hasAssigneeUser = issue.getAssigneeUser() != null;

			if (eventTypeId.equals(EventType.ISSUE_CREATED_ID)) {
				String messasge = String.format(Colors.RED + "[%s]"
						+ Colors.NORMAL + " %s(%s) が " + Colors.BOLD
						+ "%s:%s" + Colors.NORMAL + " を作成しました。",
						issueKey, userDisplayName, userName,
						issueTypeName, issueSummary);
				if (hasAssigneeUser) {
					String assigneeUserDisplayName = issue
							.getAssigneeUser().getDisplayName();
					String assigneeUserName = issue.getAssigneeUser()
							.getName();
					messasge = messasge.concat(String.format(
							"担当者は%s(%s)です。", assigneeUserDisplayName,
							assigneeUserName));
				}
				pircBot.sendMessage(channelName, messasge);
			} else if (eventTypeId
					.equals(EventType.ISSUE_RESOLVED_ID)) {
				String messasge = String.format(Colors.RED + "[%s]"
						+ Colors.NORMAL + " %s(%s) が " + Colors.BOLD
						+ "%s:%s" + Colors.NORMAL + " を解決しました。",
						issueKey, userDisplayName, userName,
						issueTypeName, issueSummary);
				pircBot.sendMessage(channelName, messasge);
			} else if (eventTypeId
					.equals(EventType.ISSUE_ASSIGNED_ID)) {
				String assigneeUserDisplayName = issue
						.getAssigneeUser().getDisplayName();
				String assigneeUserName = issue.getAssigneeUser()
						.getName();
				String messasge = String.format(Colors.RED + "[%s]"
						+ Colors.NORMAL + " %s(%s) が %s(%s) に "
						+ Colors.BOLD + "%s:%s" + Colors.NORMAL
						+ " を割当てました。", issueKey, userDisplayName,
						userName, assigneeUserDisplayName,
						assigneeUserName, issueTypeName, issueSummary);
				pircBot.sendMessage(channelName, messasge);
			}
			pircBot.sendMessage(channelName, url);
		} catch (NickAlreadyInUseException e) {
			LOGGER.error("例外が発生しました。", e);
		} catch (IOException e) {
			LOGGER.error("例外が発生しました。", e);
		} catch (IrcException e) {
			LOGGER.error("例外が発生しました。", e);
		}
	}

	private String getChannelName(PluginSettings settings,
			String projectId) {
		return (String) settings.get(IrcBotChannelConfig.class
				.getName() + "_" + projectId + ".channelName");
	}

	private boolean isIrcBotChannelEnable(
			PluginSettings settings, String projectId) {
		return Boolean.parseBoolean((String) settings
				.get(IrcBotChannelConfig.class.getName() + "_"
						+ projectId + ".enable"));
	}

	private boolean isIrcBotEnable(PluginSettings settings) {
		boolean enable = Boolean.parseBoolean((String) settings
				.get(IrcBotGlobalConfig.class.getName() + ".enable"));
		return enable;
	}

	private String getIrcServerName(PluginSettings settings) {
		return (String) settings.get(IrcBotGlobalConfig.class
				.getName() + ".ircServerName");
	}

	private Integer getIrcServerPort(PluginSettings settings) {
		String ircServerPort = (String) settings
				.get(IrcBotGlobalConfig.class.getName()
						+ ".ircServerPort");
		if (ircServerPort != null) {
			return Integer.parseInt(ircServerPort);
		}
		return null;
	}

	private void autoConnect()
			throws NickAlreadyInUseException, IOException,
			IrcException {
		if (pircBot.isConnected()) {
			return;
		}
		PluginSettings settings = pluginSettingsFactory
				.createGlobalSettings();

		String ircServerName = getIrcServerName(settings);
		LOGGER.debug("irc server name = " + ircServerName);
		Integer ircServerPort = getIrcServerPort(settings);
		LOGGER.debug("irc server port = " + ircServerPort);
		if (ircServerPort != null
				&& ircServerPort.intValue() != 0) {
			pircBot
					.connect(ircServerName, ircServerPort.intValue());
		} else {
			pircBot.connect(ircServerName);
		}

		List<Project> projects = projectManager
				.getProjectObjects();
		for (Project project : projects) {
			String projectId = project.getId().toString();
			String projectName = project.getName();
			LOGGER.debug(String.format(
					"projectName = %s, projectId = %s", projectName,
					projectId));
			if (isIrcBotEnable(settings)
					&& isIrcBotChannelEnable(settings, projectId)) {
				String channelName = getChannelName(settings,
						projectId);
				LOGGER.debug(String.format("channelName = %s",
						channelName));
				pircBot.joinChannel(channelName);
			}
		}
	}

	// @EventListener
	// public void onEvent(DraftWorkflowCreatedEvent workflowDeletedEvent) {
	//
	// }

}
