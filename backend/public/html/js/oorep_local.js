function oorepLoadIntoContentDiv(path) {
  $('#content').load(path, function() {
    window.scrollTo(0, 0);
  });
}
