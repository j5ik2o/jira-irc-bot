AJS.toInit(function() {
    var baseUrl = AJS.$("meta[name='application-base-url']").attr("content");
    var projectId = AJS.$("#projectId").attr("value");
    function populateForm() {
    	AJS.$.ajax({
            url: baseUrl + "/rest/jira-irc-bot/1.0/globalConfig",
            dataType: "json",
            success: function(config) {
            	if (config.enable){
                	AJS.$("#project").removeAttr("disabled");
                	AJS.$("#enable").removeAttr("disabled");
                	AJS.$("#channelName").removeAttr("disabled");
                	AJS.$("#submit").removeAttr("disabled");	
                }else{
                	AJS.$("#project").attr("disabled", "disabled");
                	AJS.$("#enable").attr("disabled", "disabled");
                	AJS.$("#channelName").attr("disabled", "disabled");
                	AJS.$("#submit").attr("disabled", "disabled");
                }
            }
        });
        AJS.$.ajax({
            url: baseUrl + "/rest/jira-irc-bot/1.0/channelConfig/" + projectId,
            dataType: "json",
            success: function(config) {   
                if (config.enable){
                	AJS.$("#enable").attr('checked', 'checked');
                }else{
                	AJS.$("#enable").removeAttr("checked");
                }
                AJS.$("#channelName").attr("value", config.channelName);
            }
        });
    }
    
    function updateConfig() {
        AJS.$.ajax({
            url: baseUrl + "/rest/jira-irc-bot/1.0/channelConfig/" + projectId,
            type: "PUT",
            contentType: "application/json",
            data: '{ "enable": "' + AJS.$("#enable").attr("checked") + '", "channelName": "' +  AJS.$("#channelName").attr("value") + '" }',
            processData: false
        });
    }
    
    populateForm();
    
    AJS.$("#project").submit(function(e) {
        e.preventDefault();
        updateConfig();
    });
});