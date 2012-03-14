package com.github.j5ik2o.jiraircbot;

import java.io.IOException;
import java.net.URI;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.templaterenderer.TemplateRenderer;

@SuppressWarnings("serial")
public class AdminServlet extends HttpServlet {

	private final UserManager userManager;
	private final TemplateRenderer renderer;
	private final LoginUriProvider loginUriProvider;
	private static final Logger LOGGER = LoggerFactory
			.getLogger(AdminServlet.class);

	public AdminServlet(UserManager userManager,
			LoginUriProvider loginUriProvider,
			TemplateRenderer renderer) {
		this.userManager = userManager;
		this.loginUriProvider = loginUriProvider;
		this.renderer = renderer;
	}

	@Override
	public void doGet(HttpServletRequest request,
			HttpServletResponse response) throws IOException,
			ServletException {
        System.out.println("aaa");
		LOGGER.debug(String.format("doGet : start(%s, %s)", request, response));
		String username = userManager.getRemoteUsername(request);
		if (username != null
				&& !userManager.isSystemAdmin(username)) {
			redirectToLogin(request, response);
			LOGGER.debug("doGet : finished");
			return;
		}
		response.setContentType("text/html;charset=utf-8");
		renderer.render("admin.vm", response.getWriter());
		LOGGER.debug("doGet : finished");
	}

	private void redirectToLogin(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		LOGGER.debug(String.format("redirectToLogin : start(%s, %s)", request, response));
		response.sendRedirect(loginUriProvider.getLoginUri(
				getUri(request)).toASCIIString());
		LOGGER.debug("redirectToLogin : finished");
	}

	private URI getUri(HttpServletRequest request) {
		LOGGER
				.debug(String.format("getUri : start(%s)", request));
		StringBuffer builder = request.getRequestURL();
		if (request.getQueryString() != null) {
			builder.append("?");
			builder.append(request.getQueryString());
		}
		URI result = URI.create(builder.toString());
		LOGGER.debug(String.format("getUri : finished(%s)",
				result));
		return result;
	}
}