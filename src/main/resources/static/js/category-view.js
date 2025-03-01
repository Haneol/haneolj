document.addEventListener('DOMContentLoaded', function() {
  console.log('카테고리 트리 스크립트 로딩됨');

  // 모든 디렉토리 요소에 이벤트 리스너 등록
  const directories = document.querySelectorAll('.category-directory');
  console.log('카테고리 디렉토리 요소 수: ' + directories.length);

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

  // 현재 페이지 경로 가져오기
  const currentPath = window.location.pathname;
  console.log('현재 페이지 경로:', currentPath);

  // 모든 카테고리 항목 접기 (초기 상태)
  const items = document.querySelectorAll('.category-item');
  items.forEach(item => {
    if (item.querySelector('.category-children')) {
      item.classList.add('collapsed');
    }
  });

  // 현재 경로가 마크다운 뷰인 경우 현재 항목과 상위 항목 펼치기
  if (currentPath.includes('/study/view/')) {
    // 현재 문서 파일 경로 찾기 시도
    findAndHighlightCurrentPath();
  } else {
    // 마크다운 뷰가 아닌 경우 최상위 디렉토리만 펼치기
    const rootItems = document.querySelectorAll('.category-tree > .category-item');
    rootItems.forEach(item => {
      item.classList.remove('collapsed');
    });
  }

  // 현재 선택된 파일 스타일 추가
  addActiveFileStyles();
});

// 현재 문서 경로를 찾아서 하이라이트하고 펼치는 함수
function findAndHighlightCurrentPath() {
  try {
    const match = window.location.pathname.match(/\/study\/view\/([^/]+)/);
    if (!match || !match[1]) {
      console.error('URL에서 인코딩된 경로를 찾을 수 없습니다.');
      return;
    }

    const encodedPath = match[1];
    console.log('URL에서 추출한 인코딩된 경로:', encodedPath);

    // Base64 디코딩 시도
    let decodedPath;
    try {
      const base64Safe = encodedPath.replace(/-/g, '+').replace(/_/g, '/');
      decodedPath = atob(base64Safe);
      console.log('Base64 디코딩 결과:', decodedPath);
    } catch (e) {
      console.warn('Base64 디코딩 실패:', e);
      console.log('URL 디코딩 시도...');

      decodedPath = decodeURIComponent(encodedPath);
      console.log('URL 디코딩 결과:', decodedPath);
    }

    if (!decodedPath) {
      console.error('경로 디코딩 실패');
      return;
    }

    // 모든 카테고리 파일 항목 순회
    let foundMatch = false;
    const fileItems = document.querySelectorAll('.category-file');
    console.log('총 파일 항목 수:', fileItems.length);

    fileItems.forEach(fileItem => {
      const dataPath = fileItem.getAttribute('data-path');
      const href = fileItem.getAttribute('href');

      console.log(`파일 항목 검사 - href: ${href}, data-path: ${dataPath}`);

      // 현재 경로 포함 여부 확인 (여러 방식으로 시도)
      const isCurrentFile =
          (dataPath && dataPath === decodedPath) ||
          (dataPath && decodedPath.includes(dataPath)) ||
          (href && href.includes(encodedPath));

      if (isCurrentFile) {
        console.log('현재 파일 찾음:', dataPath);
        foundMatch = true;

        // 현재 파일 항목에 active 클래스 추가
        fileItem.classList.add('active');

        // 현재 파일의 모든 상위 디렉토리 펼치기
        let parent = fileItem.closest('.category-item');
        while (parent) {
          parent.classList.remove('collapsed');
          console.log('상위 디렉토리 펼침:', parent);
          parent = parent.parentElement ? parent.parentElement.closest('.category-item') : null;
        }
      }
    });

    if (!foundMatch) {
      console.warn('현재 문서와 일치하는 카테고리 항목을 찾지 못했습니다.');

      // 파일명 부분 비교로 다시 시도
      fileItems.forEach(fileItem => {
        const dataPath = fileItem.getAttribute('data-path');
        if (!dataPath) return;

        // 파일명만 추출하여 비교
        const itemFileName = dataPath.split('/').pop().split('\\').pop();
        const currentFileName = decodedPath.split('/').pop().split('\\').pop();

        if (itemFileName === currentFileName) {
          console.log('파일명 일치하는 항목 찾음:', dataPath);
          fileItem.classList.add('active');

          // 상위 디렉토리 펼치기
          let parent = fileItem.closest('.category-item');
          while (parent) {
            parent.classList.remove('collapsed');
            parent = parent.parentElement ? parent.parentElement.closest('.category-item') : null;
          }
          foundMatch = true;
        }
      });

      // 여전히 일치하는 항목을 찾지 못한 경우 최상위 디렉토리만 펼치기
      if (!foundMatch) {
        const rootItems = document.querySelectorAll('.category-tree > .category-item');
        rootItems.forEach(item => {
          item.classList.remove('collapsed');
        });
      }
    }
  } catch (error) {
    console.error('경로 처리 중 오류 발생:', error);
  }
}

// 활성화된 파일 항목에 스타일 추가
function addActiveFileStyles() {
  // 스타일 태그 생성
  const styleTag = document.createElement('style');
  styleTag.textContent = `
    /* 활성화된 파일 항목 스타일 */
    .category-file.active {
      color: var(--primary);
      font-weight: 700;
      position: relative;
    }
    
    /* 왼쪽에 강조 막대 표시 */
    .category-file.active::before {
      content: '';
      position: absolute;
      left: -10px;
      top: 0;
      bottom: 0;
      width: 3px;
      background-color: var(--primary);
      border-radius: 3px;
    }
    
    /* 호버 효과 유지 */
    .category-file.active:hover {
      color: var(--primary-dark);
    }
  `;

  // 스타일 태그를 헤드에 추가
  document.head.appendChild(styleTag);
}