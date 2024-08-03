// Alternative to validateBehavior from hudson-behavior.js that uses findNearBy
// rather than findPreviousFormItem.
function validateButtonUsingFindNearBy(checkUrl, paramList, button) {
    button = button._button;

    var parameters = {};
    paramList.split(',').forEach(function(name) {
        // Use findNearBy rather than findPreviousFormItem.
        var p = findNearBy(button, name);
        if (p) {
            // Removing leading '../', etc.
            name = name.substring(name.lastIndexOf('/') + 1);
            parameters[name] = controlValue(p);
        }
    });

    var spinner = button.closest('DIV').nextElementSibling;
    var target = spinner.nextElementSibling;
    spinner.style.display = 'block';
    fetch(checkUrl, {
        method: "post",
        body: new URLSearchParams(parameters),
        headers: crumb.wrap(),
      }).then((rsp) => {
        console.log("RSP"+rsp);
        rsp.text().then((responseText) => {
          spinner.style.display = "none";
          applyErrorMessage(target, rsp);
          layoutUpdateCallback.call();
          var s = rsp.headers.get("script");
          try {
            geval(s);
          } catch (e) {
            window.alert("failed to evaluate " + s + "\n" + e.message);
          }
        });
      });

}
