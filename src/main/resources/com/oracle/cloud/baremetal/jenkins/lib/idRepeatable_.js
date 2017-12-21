Behaviour.specify('INPUT.idRepeatable', 'initialize', 0, function(idInput) {
  var nextIdField = idInput.getAttribute('nextIdField');
  var nextIdInput = findNearBy(idInput, nextIdField);
  idInput.value = nextIdInput.value++;
});
