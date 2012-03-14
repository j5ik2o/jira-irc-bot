package com.github.j5ik2o.jiraircbot;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.JiraVelocityUtils;
import com.atlassian.jira.util.collect.MapBuilder;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.templaterenderer.TemplateRenderer;

@SuppressWarnings("serial")
public class ProjectServlet extends HttpServlet {
	private static final Logger LOGGER = LoggerFactory
			.getLogger(ProjectServlet.class);

	private final UserManager userManager;
	private final TemplateRenderer renderer;
	private final LoginUriProvider loginUriProvider;
	private final ApplicationProperties applicationProperties;
	
	private final JiraAuthenticationContext authenticationContext;

	public ProjectServlet(UserManager userManager,
			LoginUriProvider loginUriProvider,
			TemplateRenderer renderer,
			JiraAuthenticationContext authenticationContext,
			ApplicationProperties applicationProperties) {
		this.userManager = userManager;
		this.loginUriProvider = loginUriProvider;
		this.renderer = renderer;
		this.authenticationContext = authenticationContext;
		this.applicationProperties = applicationProperties;
	}

	@Override
	public void doGet(HttpServletRequest request,
			HttpServletResponse response) throws IOException,
			ServletException {
		LOGGER.debug(String.format("doGet : start(%s, %s)", request, response));
		String username = userManager.getRemoteUsername(request);
		if (username != null
				&& !userManager.isSystemAdmin(username)) {
			redirectToLogin(request, response);
			LOGGER.debug("doGet : finshed");
			return;
		}
		String projectId = request.getParameter("projectId");

		Map<String, Object> initContext = MapBuilder
				.<String, Object> newBuilder()
				.add("projectId", projectId).add("applicationProperties", applicationProperties)
				.toMap();
		Map<String, Object> root = JiraVelocityUtils
				.getDefaultVelocityParams(initContext,
						authenticationContext);
		response.setContentType("text/html;charset=utf-8");
		renderer.render("project.vm", root, response.getWriter());
		LOGGER.debug("doGet : finshed");
	}

	private void redirectToLogin(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		LOGGER.debug(String.format("redirectToLogin : start(%s, %s)", request, response));
		response.sendRedirect(loginUriProvider.getLoginUri(
				getUri(request)).toASCIIString());
		LOGGER.debug("redirectToLogin : finshed");
	}

	private URI getUri(HttpServletRequest request) {
		LOGGER.debug("getUri : start");
		StringBuffer builder = request.getRequestURL();
		if (request.getQueryString() != null) {
			builder.append("?");
			builder.append(request.getQueryString());
		}
		LOGGER.debug("getUri : finshed");
		return URI.create(builder.toString());
	}
}
