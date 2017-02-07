renderUpdate = (update) ->
	unless update.parentRefId
		update.parentRefId = null
	update.hasAttachment = window.java.hasAttachment update.refId
	template = $ jade.templates['update'] update
	for depRefId in update.dependencies
		dependency = JSON.parse window.java.getUpdate depRefId
		dependency.parentRefId = update.refId
		template.append renderUpdate dependency
	return template

setData = (data) ->
	$('#container').empty()
	for update in data
		update.parentRefId = null
		$('#container').append renderUpdate update
	toggleMessage()
	$('input[type=checkbox]').on 'change', () ->
		elem = $ @
		checked = elem.is ':checked'
		elem.attr 'data-clicked', checked
		refId = elem.attr 'id'
		$("##{refId}").trigger 'parentchanged'
	$('input[type=checkbox]').on 'parentchanged', () ->
		elem = $ @
		refId = elem.attr 'id'
		checked = elem.is ':checked'
		children = $ "[data-parent=#{refId}] > .left > .checkbox > input:not([data-executed]):not([data-required])"
		children.prop 'checked', checked
		children.prop 'disabled', checked
		$("[data-parent=#{refId}] > .left > .checkbox > input[data-clicked=true]:not(:checked):not(:disabled)").prop 'checked', true
		children.trigger 'parentchanged'
	$('a.open-attachment').on 'click', () ->
		refId = $(@).attr 'id'
		window.java.openAttachment refId

showExecuted = () ->
	$('.update-container[data-executed]:not([data-parent])').show()
	toggleMessage()

hideExecuted = () ->
	$('.update-container[data-executed]:not([data-parent])').hide()
	toggleMessage()

toggleMessage = () ->
	if $('.update-container').is(':visible')
		$('#no-unexecuted-message').hide()
	else
		$('#no-unexecuted-message').show()

getSelection = () ->
	selection = $ 'input[type=checkbox]:checked'
	unless selection.length
		return ''
	ids = ''
	for sel in selection
		ids += $(sel).attr('id') + ',' 
	return ids.substring 0, ids.length - 1
