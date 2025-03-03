window.MathJax = {
  tex: {
    inlineMath: [['$', '$'], ['\\(', '\\)']],
    displayMath: [['$$', '$$'], ['\\[', '\\]']],
    processEscapes: true,
    tags: 'ams',
    parseError: function(error) {
      console.warn("MathJax 파싱 오류:", error);
      return "";
    }
  },
  svg: {
    fontCache: 'global'
  }
};