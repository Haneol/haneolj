document.addEventListener('DOMContentLoaded', function() {
  const tocToggleBtn = document.getElementById('toc-toggle-btn');
  const tocCloseBtn = document.getElementById('toc-close-btn');
  const mobileToc = document.getElementById('mobile-toc');

  // MathJax가 로드되었는지 확인하고 재처리
  if (window.MathJax) {
    window.MathJax.typeset();
  }

  // 목차 항목에 인덱스 추가 (순차적 애니메이션을 위함)
  const tocItems = document.querySelectorAll('.mobile-toc-body ul.category-tree > li');
  tocItems.forEach((item, index) => {
    item.style.setProperty('--item-index', index);
  });

  // 목차 버튼 클릭 시 팝업 표시
  tocToggleBtn.addEventListener('click', function() {
    mobileToc.classList.add('show');
    document.body.classList.add('toc-open');

    // 포커스를 팝업 내부로 이동 (접근성)
    setTimeout(() => {
      tocCloseBtn.focus();
    }, 100);
  });

  // 닫기 버튼 클릭 시 팝업 닫기
  tocCloseBtn.addEventListener('click', function() {
    closeMobileToc();
  });

  // 팝업 바깥 클릭 시 닫기
  mobileToc.addEventListener('click', function(e) {
    if (e.target === mobileToc) {
      closeMobileToc();
    }
  });

  // ESC 키 누를 때 팝업 닫기 (접근성)
  document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape' && mobileToc.classList.contains('show')) {
      closeMobileToc();
    }
  });

  function closeMobileToc() {
    mobileToc.classList.remove('show');
    document.body.classList.remove('toc-open');
    // 포커스를 원래 버튼으로 돌려놓기 (접근성)
    setTimeout(() => {
      tocToggleBtn.focus();
    }, 100);
  }

  // 코드 블록에 하이라이트 적용
  document.querySelectorAll('pre code').forEach((block) => {
    hljs.highlightElement(block);
  });

  // 체크박스 요소 변환
  processCheckboxes();

  // 콜아웃 요소 변환
  processCallouts();

  // 이미지 클릭 시 확대 기능
  document.querySelectorAll('#markdown-content img').forEach(img => {
    img.addEventListener('click', function() {
      this.classList.toggle('img-fullscreen');

      // 확대된 이미지 배경 만들기
      if (this.classList.contains('img-fullscreen')) {
        const overlay = document.createElement('div');
        overlay.className = 'img-overlay';
        overlay.style.position = 'fixed';
        overlay.style.top = '0';
        overlay.style.left = '0';
        overlay.style.width = '100%';
        overlay.style.height = '100%';
        overlay.style.backgroundColor = 'rgba(0,0,0,0.7)';
        overlay.style.zIndex = '9998';
        overlay.addEventListener('click', () => {
          this.classList.remove('img-fullscreen');
          overlay.remove();
        });
        document.body.appendChild(overlay);
      } else {
        const overlay = document.querySelector('.img-overlay');
        if (overlay) overlay.remove();
      }
    });
  });

  // 현재 페이지에 해당하는 카테고리 노드를 하이라이트
  highlightCurrentPage();
});

// 체크박스 처리 함수
function processCheckboxes() {
  // Markdown 리스트 항목 찾기
  document.querySelectorAll('#markdown-content li').forEach(item => {
    const textContent = item.textContent.trim();

    // 가능한 체크박스 패턴 검사
    let checkboxMatch = textContent.match(/^\[([ x><!\/?*nilfkISpbcq"0-9])]/);

    if (checkboxMatch) {
      // 체크박스 유형 결정
      const checkboxType = checkboxMatch[1];

      // 체크박스 클래스 추가
      item.classList.add('task-list-item');

      // 원래 텍스트에서 체크박스 표시 제거
      const contentWithoutCheckbox = item.innerHTML.replace(/\[([ x><!\/?*nilfkISpbcq"0-9])]/, '').trim();

      // 새 마커 요소 생성
      const markerSpan = document.createElement('span');
      markerSpan.className = getTaskMarkerClass(checkboxType);
      markerSpan.innerHTML = getTaskMarkerIcon(checkboxType);

      // 원래 내용 가져오기
      const taskContent = document.createElement('span');
      taskContent.className = 'task-content';
      taskContent.innerHTML = contentWithoutCheckbox;

      // 항목 내용 업데이트
      item.innerHTML = '';
      item.appendChild(markerSpan);
      item.appendChild(taskContent);
    }
  });
}

// 체크박스 마커 클래스 가져오기
function getTaskMarkerClass(type) {
  let baseClass = 'task-marker';

  switch (type) {
    case 'x': return `${baseClass} task-marker-checked`;
    case ' ': return `${baseClass} task-marker-unchecked`;
    case '>': return `${baseClass} task-marker-rescheduled`;
    case '<': return `${baseClass} task-marker-scheduled`;
    case '!': return `${baseClass} task-marker-important`;
    case '-': return `${baseClass} task-marker-cancelled`;
    case '/': return `${baseClass} task-marker-in-progress`;
    case '?': return `${baseClass} task-marker-question`;
    case '*': return `${baseClass} task-marker-star`;
    case 'n': return `${baseClass} task-marker-note`;
    case 'l': return `${baseClass} task-marker-location`;
    case 'f': return `${baseClass} task-marker-fire`;
    case 'k': return `${baseClass} task-marker-key`;
    case 'i': return `${baseClass} task-marker-information`;
    case 'I': return `${baseClass} task-marker-idea`;
    case 'S': return `${baseClass} task-marker-amount`;
    case 'p': return `${baseClass} task-marker-pro`;
    case 'c': return `${baseClass} task-marker-con`;
    case 'b': return `${baseClass} task-marker-bookmark`;
    case '"': return `${baseClass} task-marker-quote`;
    default:
      // 숫자인 경우 음성 버블로 처리
      if (!isNaN(parseInt(type))) {
        return `${baseClass} task-marker-speech`;
      }
      return `${baseClass} task-marker-unchecked`;
  }
}

// 체크박스 마커 아이콘 가져오기
function getTaskMarkerIcon(type) {
  switch (type) {
    case 'x': return '<i class="fas fa-check"></i>';
    case ' ': return '';
    case '>': return '<i class="fas fa-arrow-right"></i>';
    case '<': return '<i class="fas fa-arrow-left"></i>';
    case '!': return '<i class="fas fa-exclamation"></i>';
    case '-': return '<i class="fas fa-minus"></i>';
    case '/': return '<i class="fas fa-slash"></i>';
    case '?': return '<i class="fas fa-question"></i>';
    case '*': return '<i class="fas fa-star"></i>';
    case 'n': return '<i class="fas fa-sticky-note"></i>';
    case 'l': return '<i class="fas fa-map-marker-alt"></i>';
    case 'f': return '<i class="fa-solid fa-fire"></i>';
    case 'k': return '<i class="fa-solid fa-key"></i>';
    case 'i': return '<i class="fas fa-info"></i>';
    case 'I': return '<i class="fas fa-lightbulb"></i>';
    case 'S': return '<i class="fas fa-dollar-sign"></i>';
    case 'p': return '<i class="fas fa-thumbs-up"></i>';
    case 'c': return '<i class="fas fa-thumbs-down"></i>';
    case 'b': return '<i class="fas fa-bookmark"></i>';
    case '"': return '<i class="fas fa-quote-right"></i>';
    default:
      // 숫자인 경우 해당 숫자 표시
      if (!isNaN(parseInt(type))) {
        return '<i class="fas fa-comment"></i>';
      }
      return '';
  }
}

// 콜아웃 처리 함수
function processCallouts() {
  // 콜아웃으로 변환할 블록쿼트 찾기
  const blockquotes = document.querySelectorAll('#markdown-content blockquote');

  blockquotes.forEach(blockquote => {
    const firstParagraph = blockquote.querySelector('p:first-child');
    if (!firstParagraph) return;

    const text = firstParagraph.textContent.trim();

    // 콜아웃 형식 확인 (예: [!NOTE] 또는 [!WARNING])
    const calloutMatch = text.match(/^\[!(NOTE|INFO|TIP|TLDR|WARNING|DANGER|IMPORTANT|CAUTION|QUESTION|CITE|TODO|FAIL|SUCCESS)]\s*(.*)/i);

    if (calloutMatch) {
      // 콜아웃 타입과 제목
      const calloutType = calloutMatch[1].toLowerCase();
      const calloutTitle = calloutMatch[2];

      // 콜아웃 클래스 설정
      blockquote.classList.add('callout', `callout-${calloutType}`);

      // 아이콘과 타이틀 선택
      let iconClass;
      let titleText;

      switch (calloutType) {
        case 'note':
          iconClass = 'fas fa-note-sticky';
          titleText = '노트';
          break;
        case 'info':
        case 'tldr':
          iconClass = 'fas fa-circle-info';
          titleText = '정보';
          break;
        case 'tip':
          iconClass = 'fas fa-lightbulb';
          titleText = '팁';
          break;
        case 'warning':
          iconClass = 'fas fa-triangle-exclamation';
          titleText = '주의';
          break;
        case 'danger':
        case 'important':
        case 'caution':
          iconClass = 'fas fa-bolt';
          titleText = '위험';
          break;
        case 'todo':
        case 'success':
          iconClass = 'fas fa-square-check';
          titleText = '확인';
          break;
        case 'cite':
          iconClass = 'fas fa-quote-left';
          titleText = '인용'
          break;
        case 'question':
          iconClass = 'fas fa-circle-question';
          titleText = '궁금증'
          break;
        case 'fail':
          iconClass = 'fas fa-circle-xmark';
          titleText = '실패';
          break;
        default:
          iconClass = 'fas fa-info-circle';
          titleText = '참고';
      }

      // 콜아웃 제목 만들기
      const titleElement = document.createElement('div');
      titleElement.className = 'callout-title';
      titleElement.innerHTML = `<i class="${iconClass} callout-title-icon"></i>${calloutTitle || titleText}`;

      // 콜아웃 내용 컨테이너 만들기
      const contentElement = document.createElement('div');
      contentElement.className = 'callout-content';

      // 첫 번째 단락의 콜아웃 마커 제거
      firstParagraph.innerHTML = firstParagraph.innerHTML.replace(/^\[!(NOTE|INFO|TIP|TLDR|WARNING|DANGER|IMPORTANT|CAUTION|QUESTION|CITE|TODO|FAIL|SUCCESS)]\s*(.*)/i, '$2');

      // 내용이 비어있으면 제거하지 않음
      if (firstParagraph.textContent.trim() === '') {
        // 다음 단락이 있으면 그것을 첫 번째 단락으로 설정
        const nextParagraph = blockquote.querySelector('p:nth-child(2)');
        if (nextParagraph) {
          contentElement.appendChild(nextParagraph.cloneNode(true));
          nextParagraph.remove();
        }
        firstParagraph.remove();
      } else {
        contentElement.appendChild(firstParagraph.cloneNode(true));
        firstParagraph.remove();
      }

      // 나머지 내용도 콜아웃 내용으로 이동
      while (blockquote.firstChild) {
        contentElement.appendChild(blockquote.firstChild);
      }

      // 새 구조 추가
      blockquote.appendChild(titleElement);
      blockquote.appendChild(contentElement);
    }
  });
}

// 현재 페이지 하이라이트 함수
function highlightCurrentPage() {
  // 현재 페이지 경로
  const currentPath = window.location.pathname;

  // 카테고리 파일 항목
  const fileItems = document.querySelectorAll('.category-file');

  fileItems.forEach(item => {
    const href = item.getAttribute('href');
    if (href && currentPath.includes(encodeURIComponent(item.dataset.path))) {
      item.classList.add('active');

      // 부모 디렉토리 열기
      let parent = item.closest('.category-item');
      while (parent) {
        if (parent.classList.contains('collapsed')) {
          parent.classList.remove('collapsed');
        }
        parent = parent.parentElement.closest('.category-item');
      }
    }
  });
}