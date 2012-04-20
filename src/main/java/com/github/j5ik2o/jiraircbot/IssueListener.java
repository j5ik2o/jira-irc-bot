package com.github.j5ik2o.jiraircbot;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.jibble.pircbot.Colors;
import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.NickAlreadyInUseException;
import org.jibble.pircbot.PircBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import com.atlassian.core.util.DateUtils;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.event.type.EventType;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.comments.Comment;
import com.atlassian.jira.issue.worklog.Worklog;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.util.velocity.VelocityRequestContextFactory;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

/**
 * Simple JIRA listener using the atlassian-event library and demonstrating
 * plugin lifecycle integration.
 */
public class IssueListener implements InitializingBean, DisposableBean {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(IssueListener.class);

	private final EventPublisher eventPublisher;

	private final PluginSettingsFactory pluginSettingsFactory;

	private final PircBot pircBot = new PircBot() {
		{
			// setName("jira-irc-bot-" + UUID.randomUUID().toString());
			setName("jira-irc-bot");
		}
	};

	private final VelocityRequestContextFactory velocityRequestContextFactory;

	private final ProjectManager projectManager;

	private PluginSettings settings;

	/**
	 * Constructor.
	 * 
	 * @param eventPublisher
	 *            injected {@code EventPublisher} implementation.
	 */
	public IssueListener(EventPublisher eventPublisher,
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

	private void onIssueAssignedEvent(String channelName, IssueEvent issueEvent) {
		Issue issue = issueEvent.getIssue();
		String projectId = issue.getProjectObject().getId().toString();
		String issueTypeName = issue.getIssueTypeObject().getNameTranslation();
		String issueKey = issue.getKey();
		String issueSummary = issue.getSummary();
		String userDisplayName = issueEvent.getUser().getDisplayName();
		String userName = issueEvent.getUser().getName();
		String assigneeUserDisplayName = issue.getAssigneeUser()
				.getDisplayName();
		String assigneeUserName = issue.getAssigneeUser().getName();
		String messasge = String.format(Colors.RED + "[%s]" + Colors.NORMAL
				+ " %s(%s) が %s(%s) に " + Colors.BOLD + "%s:%s" + Colors.NORMAL
				+ " を割当てました。", issueKey, userDisplayName, userName,
				assigneeUserDisplayName, assigneeUserName, issueTypeName,
				issueSummary);
		sendMessage(projectId, channelName, messasge);
		sendIssueUrl(channelName, issue);
	}

	private void onIssueResolvedEvent(String channelName, IssueEvent issueEvent) {
		Issue issue = issueEvent.getIssue();
		String projectId = issue.getProjectObject().getId().toString();
		String issueTypeName = issue.getIssueTypeObject().getNameTranslation();
		String issueKey = issue.getKey();
		String issueSummary = issue.getSummary();
		String userDisplayName = issueEvent.getUser().getDisplayName();
		String userName = issueEvent.getUser().getName();

		String messasge = String.format(Colors.RED + "[%s]" + Colors.NORMAL
				+ " %s(%s) が " + Colors.BOLD + "%s:%s" + Colors.NORMAL
				+ " を解決しました。", issueKey, userDisplayName, userName,
				issueTypeName, issueSummary);
		sendMessage(projectId, channelName, messasge);
		sendTimeSpent(projectId, channelName, issue.getTimeSpent());
		sendIssueEventComment(settings, projectId, channelName, issueEvent);
		sendIssueUrl(channelName, issue);
	}

	private void sendIssueUrl(String channelName, Issue issue) {
		String projectId = issue.getProjectObject().getId().toString();
		String url = getIssueUrl(issue);
		sendMessage(projectId, channelName, url);
	}

	private void sendIssueUrl(PluginSettings settings, String channelName,
			Issue issue, String option) {
		String projectId = issue.getProjectObject().getId().toString();
		String url = getIssueUrl(issue);
		sendMessage(projectId, channelName, url.concat(option));
	}

	private void onIssueCreateEvent(String channelName, IssueEvent issueEvent) {
		LOGGER.debug(String.format("channelName = %s", channelName));
		Issue issue = issueEvent.getIssue();
		String projectId = issue.getProjectObject().getId().toString();
		String issueTypeName = issue.getIssueTypeObject().getNameTranslation();
		String issueKey = issue.getKey();
		String issueSummary = issue.getSummary();
		String userDisplayName = issueEvent.getUser().getDisplayName();
		String userName = issueEvent.getUser().getName();
		boolean hasAssigneeUser = issue.getAssigneeUser() != null;
		String messasge = String.format(Colors.RED + "[%s]" + Colors.NORMAL
				+ " %s(%s) が " + Colors.BOLD + "%s:%s" + Colors.NORMAL
				+ " を作成しました。", issueKey, userDisplayName, userName,
				issueTypeName, issueSummary);
		if (hasAssigneeUser) {
			String assigneeUserDisplayName = issue.getAssigneeUser()
					.getDisplayName();
			String assigneeUserName = issue.getAssigneeUser().getName();
			messasge = messasge.concat(String.format("担当者は%s(%s)です。",
					assigneeUserDisplayName, assigneeUserName));
		}
		sendMessage(projectId, channelName, messasge);
		sendIssueEventComment(settings, projectId, channelName, issueEvent);
		sendIssueUrl(channelName, issue);
	}

	private void sendIssueEventComment(PluginSettings settings,
			String projectId, String channelName, IssueEvent issueEvent) {
		if (issueEvent.getComment() != null
				&& StringUtils.isNotBlank(issueEvent.getComment().getBody())) {
			String comment = StringUtils.abbreviate(issueEvent.getComment()
					.getBody(), 60);
			sendMessage(projectId, channelName,
					String.format("\"%s\"", comment));
		}
	}

	private void onIssueWorkLoggedEvent(String channelName,
			IssueEvent issueEvent) {
		Issue issue = issueEvent.getIssue();
		String projectId = issue.getProjectObject().getId().toString();
		String issueTypeName = issue.getIssueTypeObject().getNameTranslation();
		String issueKey = issue.getKey();
		String issueSummary = issue.getSummary();
		Worklog worklog = issueEvent.getWorklog();
		String authorFullName = worklog.getAuthorFullName();
		String author = worklog.getAuthor();
		String messasge = String.format(Colors.RED + "[%s]" + Colors.NORMAL
				+ " %s(%s) が " + Colors.BOLD + "%s:%s" + Colors.NORMAL
				+ " に作業ログを記録しました。", issueKey, authorFullName, author,
				issueTypeName, issueSummary);
		sendMessage(projectId, channelName, messasge);
		Long timeSpent = worklog.getTimeSpent();
		sendTimeSpent(projectId, channelName, timeSpent);
		if (StringUtils.isNotBlank(worklog.getComment())) {
			String comment = StringUtils.abbreviate(worklog.getComment(), 20);
			sendMessage(projectId, channelName, String.format("\"%s\"", comment));
		}
		sendIssueUrl(
				settings,
				channelName,
				issue,
				String.format(
						"?focusedWorklogId=%s&page=com.atlassian.jira.plugin.system.issuetabpanels&worklog-tabpanel#worklog-%s",
						worklog.getId().toString(), worklog.getId().toString()));
	}

	private void sendTimeSpent(String projectId, String channelName,
			Long timeSpent) {
		if (timeSpent != null) {
			sendMessage(

					projectId,
					channelName,
					String.format("作業時間 : "
							+ DateUtils.getDurationString(timeSpent, 8, 5)));
		}
	}

	private void sendMessage(String projectId, String channelName,
			String message) {
		if (isIrcBotChannelNotice(settings, projectId)) {
			pircBot.sendNotice(channelName, message);
		} else {
			pircBot.sendMessage(channelName, message);
		}
	}

	private void onIssueCommentedEvent(String channelName, IssueEvent issueEvent) {
		Issue issue = issueEvent.getIssue();
		String projectId = issue.getProjectObject().getId().toString();
		String issueTypeName = issue.getIssueTypeObject().getNameTranslation();
		String issueKey = issue.getKey();
		String issueSummary = issue.getSummary();
		Comment comment = issueEvent.getComment();
		User authorUser = comment.getAuthorUser();
		String authUserDisplayName = authorUser.getDisplayName();
		String authUserName = authorUser.getName();
		String commentBody = StringUtils.abbreviate(comment.getBody(), 20);
		String messasge = String.format(Colors.RED + "[%s]" + Colors.NORMAL
				+ " %s(%s) が " + Colors.BOLD + "%s:%s" + Colors.NORMAL
				+ " にコメントしました。", issueKey, authUserDisplayName, authUserName,
				issueTypeName, issueSummary);
		sendMessage(projectId, channelName, messasge);
		sendMessage(projectId, channelName,
				String.format("\"%s\"", commentBody));
		sendIssueUrl(
				settings,
				channelName,
				issue,
				String.format(
						"?focusedCommentId=%d&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-%d",
						comment.getId().longValue(), comment.getId()
								.longValue()));
	}

	private String getIssueUrl(Issue issue) {
		String url = String.format("%s/browse/%s",
				velocityRequestContextFactory.getJiraVelocityRequestContext()
						.getCanonicalBaseUrl(), issue.getKey());
		return url;
	}

	/**
	 * Receives any {@code IssueEvent}s sent by JIRA.
	 * 
	 * @param issueEvent
	 *            the IssueEvent passed to us
	 */
	@EventListener
	public synchronized void onIssueEvent(IssueEvent issueEvent) {
		settings = pluginSettingsFactory.createGlobalSettings();
		Project project = issueEvent.getIssue().getProjectObject();
		String projectId = project.getId().toString();

		if (isIrcBotEnable(settings) == false
				|| isIrcBotChannelEnable(settings, projectId) == false) {
			return;
		}

		try {
			autoConnect();

			String channelName = getChannelName(settings, projectId);
			LOGGER.debug(String.format("channelName = %s", channelName));

			// 対象チャネルにジョインしていなければジョインする
			if (Arrays.asList(pircBot.getChannels()).contains(channelName) == false) {
				LOGGER.info(String.format("join channel (%s)", channelName));
				pircBot.joinChannel(channelName);
			}

			Long eventTypeId = issueEvent.getEventTypeId();
			if (eventTypeId.equals(EventType.ISSUE_CREATED_ID)) {
				onIssueCreateEvent(channelName, issueEvent);
			} else if (eventTypeId.equals(EventType.ISSUE_RESOLVED_ID)) {
				onIssueResolvedEvent(channelName, issueEvent);
			} else if (eventTypeId.equals(EventType.ISSUE_ASSIGNED_ID)) {
				onIssueAssignedEvent(channelName, issueEvent);
			} else if (eventTypeId.equals(EventType.ISSUE_WORKLOGGED_ID)) {
				onIssueWorkLoggedEvent(channelName, issueEvent);
			} else if (eventTypeId.equals(EventType.ISSUE_WORKSTARTED_ID)) {
				onIssueWorkStartedEvent(channelName, issueEvent);
			} else if (eventTypeId.equals(EventType.ISSUE_WORKSTOPPED_ID)) {
				onIssueWorkStopedEvent(channelName, issueEvent);
			} else if (eventTypeId.equals(EventType.ISSUE_COMMENTED_ID)) {
				onIssueCommentedEvent(channelName, issueEvent);
			} else if (eventTypeId.equals(EventType.ISSUE_REOPENED_ID)) {
				onIssueReOpenedEvent(channelName, issueEvent);
			} else if (eventTypeId.equals(EventType.ISSUE_CLOSED_ID)) {
				onIssueClosedEvent(channelName, issueEvent);
			}
		} catch (NickAlreadyInUseException e) {
			LOGGER.error("例外が発生しました。", e);
		} catch (IOException e) {
			LOGGER.error("例外が発生しました。", e);
		} catch (IrcException e) {
			LOGGER.error("例外が発生しました。", e);
		}
	}

	private void onIssueClosedEvent(String channelName, IssueEvent issueEvent) {
		Issue issue = issueEvent.getIssue();
		String projectId = issue.getProjectObject().getId().toString();
		String issueTypeName = issue.getIssueTypeObject().getNameTranslation();
		String issueKey = issue.getKey();
		String issueSummary = issue.getSummary();
		String userDisplayName = issueEvent.getUser().getDisplayName();
		String userName = issueEvent.getUser().getName();
		String messasge = String.format(Colors.RED + "[%s]" + Colors.NORMAL
				+ " %s(%s) が " + Colors.BOLD + "%s:%s" + Colors.NORMAL
				+ " をクローズしました。", issueKey, userDisplayName, userName,
				issueTypeName, issueSummary);
		sendMessage(projectId, channelName, messasge);
		sendIssueUrl(channelName, issue);
	}

	private void onIssueReOpenedEvent(String channelName, IssueEvent issueEvent) {
		Issue issue = issueEvent.getIssue();
		String projectId = issue.getProjectObject().getId().toString();
		String issueTypeName = issue.getIssueTypeObject().getNameTranslation();
		String issueKey = issue.getKey();
		String issueSummary = issue.getSummary();
		String userDisplayName = issueEvent.getUser().getDisplayName();
		String userName = issueEvent.getUser().getName();
		String messasge = String.format(Colors.RED + "[%s]" + Colors.NORMAL
				+ " %s(%s) が " + Colors.BOLD + "%s:%s" + Colors.NORMAL
				+ " を再オープンしました。", issueKey, userDisplayName, userName,
				issueTypeName, issueSummary);
		sendMessage(projectId, channelName, messasge);
		sendIssueUrl(channelName, issue);
	}

	private void onIssueWorkStartedEvent(String channelName,
			IssueEvent issueEvent) {
		Issue issue = issueEvent.getIssue();
		String projectId = issue.getProjectObject().getId().toString();
		String issueTypeName = issue.getIssueTypeObject().getNameTranslation();
		String issueKey = issue.getKey();
		String issueSummary = issue.getSummary();
		String userDisplayName = issueEvent.getUser().getDisplayName();
		String userName = issueEvent.getUser().getName();
		String messasge = String.format(Colors.RED + "[%s]" + Colors.NORMAL
				+ " %s(%s) が " + Colors.BOLD + "%s:%s" + Colors.NORMAL
				+ " を開始しました。", issueKey, userDisplayName, userName,
				issueTypeName, issueSummary);
		sendMessage(projectId, channelName, messasge);
		sendIssueUrl(channelName, issue);
	}

	private void onIssueWorkStopedEvent(String channelName,
			IssueEvent issueEvent) {
		Issue issue = issueEvent.getIssue();
		String projectId = issue.getProjectObject().getId().toString();
		String issueTypeName = issue.getIssueTypeObject().getNameTranslation();
		String issueKey = issue.getKey();
		String issueSummary = issue.getSummary();
		String userDisplayName = issueEvent.getUser().getDisplayName();
		String userName = issueEvent.getUser().getName();
		String messasge = String.format(Colors.RED + "[%s]" + Colors.NORMAL
				+ " %s(%s) が " + Colors.BOLD + "%s:%s" + Colors.NORMAL
				+ " を中止しました。", issueKey, userDisplayName, userName,
				issueTypeName, issueSummary);
		sendMessage(projectId, channelName, messasge);
		sendIssueUrl(channelName, issue);
	}

	private String getChannelName(PluginSettings settings, String projectId) {
		return (String) settings.get(IrcBotChannelConfig.class.getName() + "_"
				+ projectId + ".channelName");
	}

	private boolean isIrcBotChannelEnable(PluginSettings settings,
			String projectId) {
		return Boolean.parseBoolean((String) settings
				.get(IrcBotChannelConfig.class.getName() + "_" + projectId
						+ ".enable"));
	}

	private boolean isIrcBotChannelNotice(PluginSettings settings,
			String projectId) {
		return Boolean.parseBoolean((String) settings
				.get(IrcBotChannelConfig.class.getName() + "_" + projectId
						+ ".notice"));
	}

	private boolean isIrcBotEnable(PluginSettings settings) {
		boolean enable = Boolean.parseBoolean((String) settings
				.get(IrcBotGlobalConfig.class.getName() + ".enable"));
		return enable;
	}

	private String getIrcServerName(PluginSettings settings) {
		return (String) settings.get(IrcBotGlobalConfig.class.getName()
				+ ".ircServerName");
	}

	private Integer getIrcServerPort(PluginSettings settings) {
		String ircServerPort = (String) settings.get(IrcBotGlobalConfig.class
				.getName() + ".ircServerPort");
		if (ircServerPort != null) {
			return Integer.parseInt(ircServerPort);
		}
		return null;
	}

	private void autoConnect() throws NickAlreadyInUseException, IOException,
			IrcException {
		if (pircBot.isConnected()) {
			return;
		}

		String ircServerName = getIrcServerName(settings);
		LOGGER.debug("irc server name = " + ircServerName);
		Integer ircServerPort = getIrcServerPort(settings);
		LOGGER.debug("irc server port = " + ircServerPort);
		if (ircServerPort != null && ircServerPort.intValue() != 0) {
			pircBot.connect(ircServerName, ircServerPort.intValue());
		} else {
			pircBot.connect(ircServerName);
		}

		List<Project> projects = projectManager.getProjectObjects();
		for (Project project : projects) {
			String projectId = project.getId().toString();
			String projectName = project.getName();
			LOGGER.debug(String.format("projectName = %s, projectId = %s",
					projectName, projectId));
			if (isIrcBotEnable(settings)
					&& isIrcBotChannelEnable(settings, projectId)) {
				String channelName = getChannelName(settings, projectId);
				LOGGER.debug(String.format("channelName = %s", channelName));
				pircBot.joinChannel(channelName);
			}
		}
	}

	// @EventListener
	// public void onEvent(DraftWorkflowCreatedEvent workflowDeletedEvent) {
	//
	// }

}
