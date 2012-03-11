AJS.toInit(function() {
    var baseUrl = AJS.$("meta[name='application-base-url']").attr("content");
    
    function populateForm() {
        AJS.$.ajax({
            url: baseUrl + "/rest/jira-irc-bot/1.0/globalConfig",
            dataType: "json",
            success: function(config) {
            	if (config.enable){
                	AJS.$("#enable").attr('checked', 'checked');
                }else{
                	AJS.$("#enable").removeAttr("checked");
                }
                AJS.$("#ircServerName").attr("value", config.ircServerName);
                AJS.$("#ircServerPort").attr("value", config.ircServerPort);
            }
        });
    }
    
    function updateConfig() {
        AJS.$.ajax({
            url: baseUrl + "/rest/jira-irc-bot/1.0/globalConfig",
            type: "PUT",
            contentType: "application/json",
            data: '{ "enable": "' + AJS.$("#enable").attr("checked") + '", "ircServerName": "' + AJS.$("#ircServerName").attr("value") + '", "ircServerPort": ' +  AJS.$("#ircServerPort").attr("value") + ' }',
            processData: false
        });
    }
	    
    populateForm();
    
    AJS.$("#admin").submit(function(e) {
        e.preventDefault();
        updateConfig();
    });
});