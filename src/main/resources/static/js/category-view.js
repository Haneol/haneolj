document.addEventListener('DOMContentLoaded', function() {
  console.log('카테고리 트리 스크립트 로딩됨');

  // 모든 디렉토리 요소에 이벤트 리스너 등록
  const directories = document.querySelectorAll('.category-directory');
  console.log('카테고리 디렉토리 요소 수: ' + directories.length);

  // 모든 디렉토리 초기화 - 처음에는 펼쳐진 상태로
  const items = document.querySelectorAll('.category-item');
  items.forEach(item => {
    if (item.querySelector('.category-children')) {
      // 최상위 카테고리만 펼치고, 나머지는 접기
      const level = parseInt(item.getAttribute('data-level') || '0');
      if (level > 0) {
        item.classList.add('collapsed');
      }
    }
  });

  // 클릭 이벤트 등록
  directories.forEach(dir => {
    dir.addEventListener('click', function(e) {
      console.log('카테고리 디렉토리 클릭됨', this);
      e.preventDefault();
      e.stopPropagation();

      const item = this.closest('.category-item');
      console.log('카테고리 아이템:', item);

      if (item) {
        item.classList.toggle('collapsed');
        console.log('클래스 토글 후 collapsed 상태:', item.classList.contains('collapsed'));
      }
    });
  });
});