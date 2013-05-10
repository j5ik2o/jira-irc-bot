package com.github.j5ik2o.jiraircbot;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public final class IrcBotGlobalConfig {

	@XmlElement
	private Boolean enable;

	@XmlElement
	private String ircServerName;

	@XmlElement
	private int ircServerPort;

	@XmlElement
	private String ircEncoding;
	
	public String getIrcEncoding() {
		return ircEncoding;
	}

	public void setIrcEncoding(String ircEncoding) {
		this.ircEncoding = ircEncoding;
	}

	public String getIrcServerName() {
		return ircServerName;
	}

	public void setIrcServerName(String name) {
		this.ircServerName = name;
	}

	public int getIrcServerPort() {
		return ircServerPort;
	}

	public void setIrcServerPort(int ircServerPort) {
		this.ircServerPort = ircServerPort;
	}

	public Boolean getEnable() {
		return enable;
	}

	public void setEnable(Boolean enable) {
		this.enable = enable;
	}
}