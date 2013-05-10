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
    	var enable = AJS.$("#enable").attr("checked") == "checked" ? "true" : "false";
        AJS.$.ajax({
            url: baseUrl + "/rest/jira-irc-bot/1.0/globalConfig",
            type: "PUT",
            contentType: "application/json",
            data: '{ "enable": "' + enable + '", "ircServerName": "' + AJS.$("#ircServerName").attr("value") + '", "ircServerPort": ' +  AJS.$("#ircServerPort").attr("value") + ' }',
            processData: false
        });
    }

    function submitCheck(){
        var check = AJS.$("#enable").attr('checked') && AJS.$("#ircServerName").val != "" && AJS.$("#ircServerPort").val != "";
        //alert(check);
        if (check){
            AJS.$("#submit").removeAttr("disabled");
        }else{
            AJS.$("#submit").attr("disabled", "disabled");
        }
    }

    populateForm();
    submitCheck();
    pageModified = false;
    
    AJS.$("#admin").submit(function(e) {
        e.preventDefault();
        updateConfig();
    });
    AJS.$("#ircServerName").blur(function(){
        submitCheck();
    });
    AJS.$("#ircServerPort").blur(function(){
        submitCheck();
    });

});