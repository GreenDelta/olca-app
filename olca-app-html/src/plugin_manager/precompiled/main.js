var getPlugin, registerCallbacks, renderTemplate, setData;

renderTemplate = function(templateName, data) {
  return jade.templates[templateName](data);
};

setData = function(data, online) {
  var i, len, plugin;
  $('#container').empty();
  for (i = 0, len = data.length; i < len; i++) {
    plugin = data[i];
    plugin.online = online;
    $('#container').append(renderTemplate('plugin', plugin));
  }
  return registerCallbacks();
};

registerCallbacks = function() {
  $('.btn-install').on('click', function() {
    return install(getPlugin($(this)));
  });
  $('.btn-update').on('click', function() {
    return update(getPlugin($(this)));
  });
  $('.btn-uninstall').on('click', function() {
    return uninstall(getPlugin($(this)));
  });
  return $('.btn-install-local').on('click', function() {
    return installLocal();
  });
};

getPlugin = function(element) {
  while (!element.attr('data-plugin')) {
    element = element.parent();
  }
  return element.attr('data-plugin');
};
