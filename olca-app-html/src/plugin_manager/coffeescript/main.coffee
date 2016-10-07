renderTemplate = (templateName, data) ->
	return jade.templates[templateName] data

setData = (data, online) ->
	$('#container').empty()
	for plugin in data
		plugin.online = online
		$('#container').append renderTemplate 'plugin', plugin 		
	registerCallbacks()

registerCallbacks = () ->
	$('.btn-install').on 'click', () ->
		window.java.install getPlugin $(@)
	$('.btn-update').on 'click', () ->
		window.java.update getPlugin $(@)
	$('.btn-uninstall').on 'click', () ->
		window.java.uninstall getPlugin $(@)

getPlugin = (element) ->
	while !element.attr('data-plugin')
		element = element.parent()
	return element.attr 'data-plugin'
