// Alternative to validateBehavior from hudson-behavior.js that uses findNearBy
// rather than findPreviousFormItem.
function validateButtonUsingFindNearBy(checkUrl, paramList, button) {
    button = button._button;

    var parameters = {};
    paramList.split(',').each(function(name) {
        // Use findNearBy rather than findPreviousFormItem.
        var p = findNearBy(button, name);
        if (p) {
            // Removing leading '../', etc.
            name = name.substring(name.lastIndexOf('/') + 1);
            parameters[name] = controlValue(p);
        }
    });

    var spinner = $(button).up('DIV').next();
    var target = spinner.next();
    spinner.style.display = 'block';

    new Ajax.Request(checkUrl, {
        parameters: parameters,
        onComplete: function(rsp) {
            spinner.style.display = 'none';
            applyErrorMessage(target, rsp);
            layoutUpdateCallback.call();
            var s = rsp.getResponseHeader('script');
            try {
                geval(s);
            } catch(e) {
                window.alert('failed to evaluate ' + s + '\n' + e.message);
            }
        }
    });
}
